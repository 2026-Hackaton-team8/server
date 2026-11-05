package org.study.jibeo.global.ai;

import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.CallReason;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Python AI 와 주고받는 형식 ("짚어 AI 연동 형식 초안" 문서와 같음)
 */
public final class AiDtos {

    private AiDtos() {
    }

    public record ModelInfo(Long id, String code, String name, String category) {
    }

    // ---------- POST /ai/consultations/next ----------

    public record NextRequest(Long consultationId, EquipmentInfo equipment, ModelInfo model, String symptom,
                              int questionCount, int maxQuestions, List<Message> messages) {
    }

    public record EquipmentInfo(Long id, String name, String location) {
    }

    public record Message(String sender, String type, String content) {
    }

    public enum TurnType {
        QUESTION,
        SOLUTION,
        ESCALATE
    }

    public record NextResponse(
            TurnType type,
            String question,
            List<String> options,
            List<Candidate> candidates,
            String cause,
            String suspectedPartCode,
            Double confidence,
            List<String> steps,
            List<String> cautions,
            Evidence evidence,
            CallReason reason,
            AiSummary summary,
            String aiStatus
    ) {
    }

    public record Candidate(String cause, String partCode, Double confidence) {
    }

    /** manualSources: 근거 매뉴얼 출처·원문 (sectionId, title, version, originalQuote 등). 형식은 AI 가 정하므로 그대로 저장·전달한다 */
    public record Evidence(List<String> manualSectionIds, List<Long> repairHistoryIds, Long solutionId,
                           List<Map<String, Object>> manualSources) {
    }

    // ---------- POST /ai/solutions/draft ----------

    public record DraftRequest(ModelInfo model, List<History> repairHistories) {
    }

    public record History(Long id, PartInfo part, String symptom, String cause, String solution, String difficulty,
                          LocalDate repairedAt) {
    }

    public record PartInfo(String code, String name) {
    }

    public record DraftResponse(String title, String category, List<String> symptoms, List<String> steps,
                                String fallback, List<Long> evidenceRepairHistoryIds) {
    }

    // ---------- POST /ai/index ----------

    /** AI 는 지금 매뉴얼 색인만 받는다 (수리 이력·솔루션은 422) */
    public enum IndexKind {
        MANUAL
    }

    public record IndexRequest(IndexKind kind, Long id) {
    }
}
