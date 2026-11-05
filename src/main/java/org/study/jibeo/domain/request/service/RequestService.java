package org.study.jibeo.domain.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.request.dto.AssignRequest;
import org.study.jibeo.domain.request.dto.CreateRequestRequest;
import org.study.jibeo.domain.request.dto.EngineerResponse;
import org.study.jibeo.domain.request.dto.InboxResponse;
import org.study.jibeo.domain.request.dto.RequestDetailResponse;
import org.study.jibeo.domain.request.dto.RequestSummaryResponse;
import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.model.RequestStatus;
import org.study.jibeo.domain.request.repository.RequestRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.exception.ErrorCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {

    private final RequestRepository requestRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    // ---------- 사용 업체 ----------

    @Transactional
    public RequestDetailResponse create(Long userId, Long companyId, CreateRequestRequest body) {
        Equipment equipment = equipmentRepository.findWithModelById(body.equipmentId())
                .filter(e -> e.isOwnedBy(companyId))
                .orElseThrow(ErrorCode.EQUIPMENT_NOT_FOUND::toException);
        CreateRequestRequest.Contact contact = body.contact();

        Request request = requestRepository.save(Request.builder()
                .equipment(equipment)
                .company(equipment.getCompany())
                .user(userRepository.getReferenceById(userId))
                .makerCompany(equipment.getEquipmentModel().getMakerCompany())
                .symptomDetail(body.symptomDetail())
                .equipmentState(body.equipmentState())
                .urgency(body.urgency())
                // TODO AI 상담 연동 후 상담 기록으로 판단 (SOLUTION_FAILED, LOW_CONFIDENCE 등)
                .callReason(CallReason.NEEDS_EXPERT)
                .reporterName(contact.reporterName())
                .reporterPhone(contact.reporterPhone())
                .companyPhone(contact.companyPhone())
                .aiSummary(body.aiSummary())
                .build());
        request.assignRequestNo();
        return RequestDetailResponse.from(request);
    }

    public List<RequestSummaryResponse> getMyRequests(Long companyId) {
        return requestRepository.findAllByCompanyIdOrderByIdDesc(companyId).stream()
                .map(RequestSummaryResponse::from).toList();
    }

    public RequestDetailResponse getMyRequest(Long companyId, Long requestId) {
        Request request = findDetail(requestId);
        if (!request.isRequestedBy(companyId)) {
            throw ErrorCode.REQUEST_NOT_FOUND.toException();
        }
        return RequestDetailResponse.from(request);
    }

    // ---------- 제작 업체 ----------

    public InboxResponse getInbox(Long makerCompanyId, RequestStatus status, int page, int size) {
        Page<Request> result = requestRepository.findInbox(makerCompanyId, status, PageRequest.of(page, size));

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        for (RequestStatus s : RequestStatus.values()) {
            counts.put(s.name(), 0L);
        }
        for (RequestRepository.StatusCount c : requestRepository.countByStatusForMaker(makerCompanyId)) {
            counts.put(c.getStatus().name(), c.getCount());
            counts.merge("ALL", c.getCount(), Long::sum);
        }

        return new InboxResponse(counts, result.map(RequestSummaryResponse::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public RequestDetailResponse getReceived(Long makerCompanyId, Long requestId) {
        return RequestDetailResponse.from(findReceived(makerCompanyId, requestId));
    }

    @Transactional
    public RequestDetailResponse assign(Long makerCompanyId, Long requestId, AssignRequest body) {
        Request request = findReceived(makerCompanyId, requestId);
        User engineer = userRepository.findById(body.engineerId())
                .filter(u -> u.getRole() == UserRole.STAFF && u.getCompany().getId().equals(makerCompanyId))
                .orElseThrow(ErrorCode.ENGINEER_NOT_FOUND::toException);
        request.assign(engineer, body.visitAt());
        return RequestDetailResponse.from(request);
    }

    @Transactional
    public RequestDetailResponse resolveBySelf(Long makerCompanyId, Long requestId) {
        Request request = findReceived(makerCompanyId, requestId);
        request.resolveBySelf();
        return RequestDetailResponse.from(request);
    }

    public List<EngineerResponse> getEngineers(Long makerCompanyId) {
        return userRepository.findAllByCompanyIdAndRoleOrderByNameAsc(makerCompanyId, UserRole.STAFF).stream()
                .map(EngineerResponse::from).toList();
    }

    // ---------- 공통 ----------

    /** 다른 제작 업체로 간 호출은 존재 여부도 드러내지 않도록 404 */
    private Request findReceived(Long makerCompanyId, Long requestId) {
        Request request = findDetail(requestId);
        if (!request.isReceivedBy(makerCompanyId)) {
            throw ErrorCode.REQUEST_NOT_FOUND.toException();
        }
        return request;
    }

    private Request findDetail(Long requestId) {
        return requestRepository.findDetailById(requestId)
                .orElseThrow(ErrorCode.REQUEST_NOT_FOUND::toException);
    }
}
