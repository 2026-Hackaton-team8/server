package org.study.jibeo.global.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.CallReason;

import java.util.List;
import java.util.Optional;

/**
 * Python AI 연동 전 임시 응답. 질문 2번 뒤 매뉴얼의 문제 해결 섹션으로 해결책을 내고, 섹션이 없으면 호출을 권한다.
 * 흐름(질문, 해결책, 호출 권유)을 프론트와 맞춰 보기 위한 것으로 증상 내용은 이해하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class StubAi {

    private static final List<String> QUESTIONS = List.of(
            "증상이 언제부터 있었나요?",
            "최근에 청소하거나 부품을 바꾼 적이 있나요?");

    private final EquipmentModelRepository modelRepository;

    public AiDtos.NextResponse next(AiDtos.NextRequest request) {
        int asked = request.questionCount();
        if (asked < QUESTIONS.size() && asked < request.maxQuestions()) {
            return new AiDtos.NextResponse(AiDtos.TurnType.QUESTION, QUESTIONS.get(asked),
                    List.of("오늘", "며칠 전", "모르겠어요"), List.of(), null, null, null, null, null, null, null, null);
        }

        String tried = request.messages().stream()
                .filter(m -> "ANSWER".equals(m.type()))
                .map(AiDtos.Message::content)
                .reduce((a, b) -> a + ", " + b).orElse(null);
        Optional<Manual.Section> section = troubleshooting(request.model().id());
        if (section.isEmpty()) {
            return new AiDtos.NextResponse(AiDtos.TurnType.ESCALATE, null, null, null,
                    "참고할 매뉴얼이 없어 원인을 좁히지 못했어요", null, null, null, null, null, CallReason.NO_HISTORY,
                    new AiSummary(request.symptom(), "미확인", tried));
        }
        Manual.Section s = section.get();
        String partCode = s.partCodes() == null || s.partCodes().isEmpty() ? null : s.partCodes().getFirst();
        List<String> steps = s.steps() == null || s.steps().isEmpty() ? List.of(s.content()) : s.steps();
        return new AiDtos.NextResponse(AiDtos.TurnType.SOLUTION, null, null, null, s.title(), partCode, 0.5,
                steps, s.cautions() == null ? List.of() : s.cautions(),
                new AiDtos.Evidence(List.of(s.id()), List.of(), null), null,
                new AiSummary(request.symptom(), s.title(), tried));
    }

    public AiDtos.DraftResponse draft(AiDtos.DraftRequest request) {
        List<AiDtos.History> self = request.repairHistories().stream()
                .filter(h -> "SELF".equals(h.difficulty())).toList();
        List<AiDtos.History> base = self.isEmpty() ? request.repairHistories() : self;
        AiDtos.History first = base.getFirst();
        return new AiDtos.DraftResponse(
                first.symptom(),
                first.part().name() + " 관련",
                List.of(first.symptom()),
                base.stream().map(AiDtos.History::solution).distinct().toList(),
                "그래도 해결되지 않으면 담당자를 호출하세요.",
                base.stream().map(AiDtos.History::id).toList());
    }

    private Optional<Manual.Section> troubleshooting(Long modelId) {
        return modelRepository.findById(modelId)
                .map(EquipmentModel::getManual)
                .flatMap(manual -> manual.sections().stream()
                        .filter(s -> s.type() == Manual.SectionType.TROUBLESHOOTING)
                        .findFirst());
    }
}
