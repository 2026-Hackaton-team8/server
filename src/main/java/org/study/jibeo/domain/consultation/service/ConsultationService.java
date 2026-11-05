package org.study.jibeo.domain.consultation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.consultation.dto.AnswerRequest;
import org.study.jibeo.domain.consultation.dto.ConsultationDetailResponse;
import org.study.jibeo.domain.consultation.dto.ConsultationListResponse;
import org.study.jibeo.domain.consultation.dto.MessageResponse;
import org.study.jibeo.domain.consultation.dto.RequestDraftResponse;
import org.study.jibeo.domain.consultation.dto.StartConsultationRequest;
import org.study.jibeo.domain.consultation.model.Consultation;
import org.study.jibeo.domain.consultation.model.ConsultationMessage;
import org.study.jibeo.domain.consultation.model.ConsultationStatus;
import org.study.jibeo.domain.consultation.model.MessagePayload;
import org.study.jibeo.domain.consultation.model.MessageSender;
import org.study.jibeo.domain.consultation.model.MessageType;
import org.study.jibeo.domain.consultation.repository.ConsultationMessageRepository;
import org.study.jibeo.domain.consultation.repository.ConsultationRepository;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.repository.RequestRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.ai.AiClient;
import org.study.jibeo.global.ai.AiDtos;
import org.study.jibeo.global.exception.ErrorCode;
import team.themoment.sdk.exception.ExpectedException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 상담. 대화는 DB 에 저장하고, 차례마다 지금까지의 대화를 Python AI 에 보내 다음 질문/해결책/호출 권유를 받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private static final String UNKNOWN_CAUSE = "원인을 좁히지 못했어요";

    private final ConsultationRepository consultationRepository;
    private final ConsultationMessageRepository messageRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final AiClient aiClient;

    @Transactional
    public ConsultationDetailResponse start(Long userId, Long companyId, StartConsultationRequest body) {
        Equipment equipment = equipmentRepository.findWithModelById(body.equipmentId())
                .filter(e -> e.isOwnedBy(companyId))
                .orElseThrow(ErrorCode.EQUIPMENT_NOT_FOUND::toException);
        Consultation consultation = consultationRepository.save(Consultation.builder()
                .equipment(equipment)
                .company(equipment.getCompany())
                .user(userRepository.getReferenceById(userId))
                .symptom(body.symptom().trim())
                .build());
        save(consultation, MessageSender.USER, MessageType.SYMPTOM, consultation.getSymptom(), null);
        return askAi(consultation);
    }

    @Transactional
    public ConsultationDetailResponse answer(Long companyId, Long consultationId, AnswerRequest body) {
        Consultation consultation = findOwned(companyId, consultationId);
        consultation.requireInProgress();
        if (!isAsking(messages(consultation))) {
            throw ErrorCode.CONSULTATION_NOT_ASKING.toException();
        }
        save(consultation, MessageSender.USER, MessageType.ANSWER, body.content().trim(), null);
        return askAi(consultation);
    }

    public ConsultationListResponse getList(Long companyId, ConsultationStatus status, Long equipmentId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        for (ConsultationStatus s : ConsultationStatus.values()) {
            counts.put(s.name(), 0L);
        }
        for (ConsultationRepository.StatusCount c : consultationRepository.countByStatus(companyId, equipmentId)) {
            counts.put(c.getStatus().name(), c.getCount());
            counts.merge("ALL", c.getCount(), Long::sum);
        }
        List<ConsultationListResponse.Item> content = consultationRepository
                .findList(companyId, status, equipmentId).stream()
                .map(ConsultationListResponse.Item::from).toList();
        return new ConsultationListResponse(counts, content);
    }

    public ConsultationDetailResponse getDetail(Long companyId, Long consultationId) {
        return toDetail(findOwned(companyId, consultationId));
    }

    /** "해결됐어요" */
    @Transactional
    public ConsultationDetailResponse resolve(Long companyId, Long consultationId) {
        Consultation consultation = findOwned(companyId, consultationId);
        consultation.resolve();
        return toDetail(consultation);
    }

    /** 호출 신고서 자동 입력값: AI 요약 + 내 정보·회사 연락처 */
    public RequestDraftResponse getRequestDraft(Long userId, Long companyId, Long consultationId) {
        Consultation consultation = findOwned(companyId, consultationId);
        consultation.requireInProgress();
        User user = userRepository.findWithCompanyById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
        List<ConsultationMessage> messages = messages(consultation);

        AiSummary summary = consultation.getAiSummary() != null ? consultation.getAiSummary()
                : new AiSummary(consultation.getSymptom(), consultation.getAiCause(), triedActions(messages));
        Equipment e = consultation.getEquipment();
        EquipmentModel m = e.getEquipmentModel();
        return new RequestDraftResponse(consultation.getId(), e.getId(), e.getName(), m.getCode(), m.getName(),
                summary, callReasonOf(messages),
                consultation.getSuspectedPart() == null ? null : PartResponse.from(consultation.getSuspectedPart()),
                new RequestDraftResponse.Contact(user.getName(), user.getPhone(), user.getCompany().getPhone()));
    }

    /**
     * 호출 사유: 해결책을 받고도 호출 → SOLUTION_FAILED, AI 가 호출을 권했으면 그 사유, 그 외(질문 중 호출) → NEEDS_EXPERT
     */
    public CallReason callReasonOf(Consultation consultation) {
        return callReasonOf(messages(consultation));
    }

    public List<MessageResponse> getMessages(Long consultationId) {
        return messageRepository.findAllByConsultationIdOrderByIdAsc(consultationId).stream()
                .map(MessageResponse::from).toList();
    }

    // ---------- AI 차례 ----------

    private ConsultationDetailResponse askAi(Consultation consultation) {
        List<ConsultationMessage> history = messages(consultation);
        Equipment e = consultation.getEquipment();
        EquipmentModel m = e.getEquipmentModel();
        AiDtos.NextResponse res = aiClient.next(new AiDtos.NextRequest(
                consultation.getId(),
                new AiDtos.EquipmentInfo(e.getId(), e.getName(), e.getLocation()),
                new AiDtos.ModelInfo(m.getId(), m.getCode(), m.getName(), m.getCategory()),
                consultation.getSymptom(),
                consultation.getQuestionCount(),
                Consultation.MAX_QUESTIONS,
                history.stream().map(h -> new AiDtos.Message(h.getSender().name(), h.getType().name(),
                        h.getContent())).toList()));
        if (res == null || res.type() == null) {
            throw new ExpectedException("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
        }

        boolean outOfQuestions = consultation.getQuestionCount() >= Consultation.MAX_QUESTIONS;
        if (res.type() == AiDtos.TurnType.QUESTION && !outOfQuestions && notBlank(res.question())) {
            consultation.countQuestion();
            save(consultation, MessageSender.AI, MessageType.QUESTION, res.question(),
                    new MessagePayload(res.options(), res.candidates(), null, null, null, null));
        } else if (res.type() == AiDtos.TurnType.SOLUTION && res.steps() != null && !res.steps().isEmpty()) {
            String cause = notBlank(res.cause()) ? res.cause() : UNKNOWN_CAUSE;
            consultation.updateFinding(cause, part(m, res.suspectedPartCode()), res.summary());
            save(consultation, MessageSender.AI, MessageType.SOLUTION, cause,
                    new MessagePayload(null, null, res.steps(), res.cautions(), res.evidence(), null));
        } else {
            // 호출 권유, 또는 질문을 다 쓰고도 질문을 돌려준 경우 → 확신 부족으로 호출 권유
            String cause = notBlank(res.cause()) ? res.cause() : UNKNOWN_CAUSE;
            CallReason reason = res.type() == AiDtos.TurnType.ESCALATE && res.reason() != null
                    ? res.reason() : CallReason.LOW_CONFIDENCE;
            consultation.updateFinding(cause, part(m, res.suspectedPartCode()), res.summary());
            save(consultation, MessageSender.AI, MessageType.ESCALATE, cause,
                    new MessagePayload(null, null, null, null, null, reason));
        }
        return toDetail(consultation);
    }

    private Part part(EquipmentModel model, String code) {
        if (!notBlank(code)) {
            return null;
        }
        return partRepository.findByEquipmentModelIdAndCode(model.getId(), code).orElse(null);
    }

    // ---------- 공통 ----------

    private ConsultationDetailResponse toDetail(Consultation consultation) {
        List<ConsultationMessage> messages = messages(consultation);
        Long requestId = requestRepository.findFirstByConsultationId(consultation.getId())
                .map(Request::getId).orElse(null);
        return ConsultationDetailResponse.of(consultation, messages.stream().map(MessageResponse::from).toList(),
                isAsking(messages), requestId);
    }

    private static CallReason callReasonOf(List<ConsultationMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConsultationMessage m = messages.get(i);
            if (m.getType() == MessageType.ESCALATE) {
                return m.getPayload() != null && m.getPayload().reason() != null
                        ? m.getPayload().reason() : CallReason.LOW_CONFIDENCE;
            }
            if (m.getType() == MessageType.SOLUTION) {
                return CallReason.SOLUTION_FAILED;
            }
        }
        return CallReason.NEEDS_EXPERT;
    }

    private static String triedActions(List<ConsultationMessage> messages) {
        return messages.stream().filter(m -> m.getType() == MessageType.ANSWER)
                .map(ConsultationMessage::getContent)
                .reduce((a, b) -> a + ", " + b).orElse(null);
    }

    /** 마지막 메시지가 AI 질문이면 사용자가 답할 차례 */
    private static boolean isAsking(List<ConsultationMessage> messages) {
        return !messages.isEmpty() && messages.getLast().getType() == MessageType.QUESTION;
    }

    private List<ConsultationMessage> messages(Consultation consultation) {
        return messageRepository.findAllByConsultationIdOrderByIdAsc(consultation.getId());
    }

    private void save(Consultation consultation, MessageSender sender, MessageType type, String content,
                      MessagePayload payload) {
        messageRepository.save(ConsultationMessage.builder()
                .consultation(consultation).sender(sender).type(type).content(content).payload(payload)
                .build());
    }

    /** 다른 회사의 상담은 존재 여부도 드러내지 않도록 404 */
    private Consultation findOwned(Long companyId, Long consultationId) {
        return consultationRepository.findDetailById(consultationId)
                .filter(c -> c.isOwnedBy(companyId))
                .orElseThrow(ErrorCode.CONSULTATION_NOT_FOUND::toException);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
