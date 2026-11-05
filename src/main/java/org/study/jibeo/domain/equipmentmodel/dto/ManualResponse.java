package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.equipmentmodel.model.Manual;

import java.util.List;

/**
 * content = 입력한 전문 (없으면 null), version = 저장할 때마다 1씩 증가 (없으면 0),
 * sections = 서버가 제목 기준으로 나눈 AI 검색 항목 (확인용)
 */
public record ManualResponse(String content, int version, List<Section> sections) {

    public record Section(String id, String title, Manual.SectionType type, int length) {
    }

    public static ManualResponse of(String content, Manual manual) {
        if (manual == null) {
            return new ManualResponse(content, 0, List.of());
        }
        return new ManualResponse(content, manual.version(), manual.sections().stream()
                .map(s -> new Section(s.id(), s.title(), s.type(), s.content().length()))
                .toList());
    }
}
