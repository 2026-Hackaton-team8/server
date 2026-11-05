package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.model.ManualSection;

import java.util.List;

/**
 * content = 입력한 전문 (없으면 null), version = 저장할 때마다 1씩 증가 (없으면 0),
 * sections = 서버가 제목·줄 머리표로 나눈 항목
 */
public record ManualResponse(String content, int version, List<Section> sections) {

    /** id = 항목 id (s-001 …), steps = 작업자 조치, escalation = 담당자 호출 기준, source = 근거 문서 */
    public record Section(String id, String title, Manual.SectionType type, List<String> errorCodes,
                          List<String> partCodes, List<String> symptoms, List<String> causes, List<String> steps, String escalation,
                          List<String> cautions, String source, String description) {

        public static Section from(ManualSection s) {
            return new Section(s.getSectionKey(), s.getTitle(), s.getType(), orEmpty(s.getErrorCodes()), orEmpty(s.getPartCodes()),
                    orEmpty(s.getSymptoms()), orEmpty(s.getCauses()), orEmpty(s.getSteps()), s.getEscalation(),
                    orEmpty(s.getCautions()), s.getSource(), s.getDescription());
        }

        private static List<String> orEmpty(List<String> list) {
            return list == null ? List.of() : list;
        }
    }

    public static ManualResponse of(String content, Manual manual, List<ManualSection> sections) {
        return new ManualResponse(content, manual == null ? 0 : manual.version(),
                sections.stream().map(Section::from).toList());
    }
}
