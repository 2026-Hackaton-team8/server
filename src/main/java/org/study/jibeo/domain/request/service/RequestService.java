package org.study.jibeo.domain.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.consultation.dto.MessageResponse;
import org.study.jibeo.domain.consultation.model.Consultation;
import org.study.jibeo.domain.consultation.repository.ConsultationRepository;
import org.study.jibeo.domain.consultation.service.ConsultationService;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.file.service.FileService;
import org.study.jibeo.domain.request.dto.AssignRequest;
import org.study.jibeo.domain.request.dto.CreateRequestRequest;
import org.study.jibeo.domain.request.dto.EngineerResponse;
import org.study.jibeo.domain.request.dto.InboxResponse;
import org.study.jibeo.domain.request.dto.RequestDetailResponse;
import org.study.jibeo.domain.request.dto.RequestSummaryResponse;
import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.model.RequestAttachment;
import org.study.jibeo.domain.request.model.RequestStatus;
import org.study.jibeo.domain.request.repository.RequestAttachmentRepository;
import org.study.jibeo.domain.request.repository.RequestRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.exception.ErrorCode;
import org.study.jibeo.global.storage.FileStorage;
import org.study.jibeo.global.storage.FileViewUrls;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {

    private final RequestRepository requestRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RequestAttachmentRepository attachmentRepository;
    private final FileStorage fileStorage;
    private final FileViewUrls fileViewUrls;
    private final ConsultationRepository consultationRepository;
    private final ConsultationService consultationService;

    // ---------- 사용 업체 ----------

    @Transactional
    public RequestDetailResponse create(Long userId, Long companyId, CreateRequestRequest body) {
        Equipment equipment = equipmentRepository.findWithModelById(body.equipmentId())
                .filter(e -> e.isOwnedBy(companyId))
                .orElseThrow(ErrorCode.EQUIPMENT_NOT_FOUND::toException);
        CreateRequestRequest.Contact contact = body.contact();
        Map<String, FileStorage.FileMeta> attachments = checkAttachments(companyId, body.attachmentKeys());

        Consultation consultation = null;
        CallReason callReason = CallReason.NEEDS_EXPERT;
        if (body.consultationId() != null) {
            consultation = consultationRepository.findDetailById(body.consultationId())
                    .filter(c -> c.isOwnedBy(companyId))
                    .orElseThrow(ErrorCode.CONSULTATION_NOT_FOUND::toException);
            if (!consultation.getEquipment().getId().equals(equipment.getId())) {
                throw ErrorCode.CONSULTATION_EQUIPMENT_MISMATCH.toException();
            }
            callReason = consultationService.callReasonOf(consultation);
            consultation.escalate();
        }

        Request request = requestRepository.save(Request.builder()
                .consultation(consultation)
                .equipment(equipment)
                .company(equipment.getCompany())
                .user(userRepository.getReferenceById(userId))
                .makerCompany(equipment.getEquipmentModel().getMakerCompany())
                .symptomDetail(body.symptomDetail())
                .equipmentState(body.equipmentState())
                .urgency(body.urgency())
                .callReason(callReason)
                .reporterName(contact.reporterName())
                .reporterPhone(contact.reporterPhone())
                .companyPhone(contact.companyPhone())
                .aiSummary(body.aiSummary())
                .build());
        request.assignRequestNo();
        attachments.forEach((key, meta) -> attachmentRepository.save(RequestAttachment.builder()
                .request(request).fileKey(key).contentType(meta.contentType()).size(meta.size())
                .build()));
        return toDetail(request);
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
        return toDetail(request);
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
        return toDetail(findReceived(makerCompanyId, requestId));
    }

    @Transactional
    public RequestDetailResponse assign(Long makerCompanyId, Long requestId, AssignRequest body) {
        Request request = findReceived(makerCompanyId, requestId);
        User engineer = userRepository.findById(body.engineerId())
                .filter(u -> u.getRole() == UserRole.STAFF && u.getCompany().getId().equals(makerCompanyId))
                .orElseThrow(ErrorCode.ENGINEER_NOT_FOUND::toException);
        request.assign(engineer, body.visitAt());
        return toDetail(request);
    }

    @Transactional
    public RequestDetailResponse resolveBySelf(Long makerCompanyId, Long requestId) {
        Request request = findReceived(makerCompanyId, requestId);
        request.resolveBySelf();
        return toDetail(request);
    }

    /** 호출 상세의 "대화 전체 보기". 상담 없이 바로 호출했으면 빈 목록 */
    public List<MessageResponse> getConversation(Long makerCompanyId, Long requestId) {
        Request request = findReceived(makerCompanyId, requestId);
        return request.getConsultation() == null ? List.of()
                : consultationService.getMessages(request.getConsultation().getId());
    }

        public List<EngineerResponse> getEngineers(Long makerCompanyId) {
        return userRepository.findAllByCompanyAndRole(makerCompanyId, UserRole.STAFF).stream()
                .map(EngineerResponse::from).toList();
    }

    // ---------- 공통 ----------

    /** 첨부 키는 우리 회사 경로에 실제로 올라간 파일만 허용한다 (같은 키는 한 번만). 호출을 저장하기 전에 확인 */
    private Map<String, FileStorage.FileMeta> checkAttachments(Long companyId, List<String> keys) {
        Map<String, FileStorage.FileMeta> result = new LinkedHashMap<>();
        if (keys == null) {
            return result;
        }
        String prefix = FileService.requestAttachmentPrefix(companyId);
        for (String key : new LinkedHashSet<>(keys)) {
            if (!key.startsWith(prefix) || key.contains("..")) {
                throw ErrorCode.ATTACHMENT_NOT_FOUND.toException();
            }
            result.put(key, fileStorage.find(key).orElseThrow(ErrorCode.ATTACHMENT_NOT_FOUND::toException));
        }
        return result;
    }

    private RequestDetailResponse toDetail(Request request) {
        List<RequestDetailResponse.Attachment> attachments = attachmentRepository
                .findAllByRequestIdOrderByIdAsc(request.getId()).stream()
                .map(a -> new RequestDetailResponse.Attachment(a.getFileKey(), a.getContentType(), a.getSize(),
                        fileViewUrls.urlOf(a.getFileKey())))
                .toList();
        return RequestDetailResponse.of(request, attachments);
    }

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
