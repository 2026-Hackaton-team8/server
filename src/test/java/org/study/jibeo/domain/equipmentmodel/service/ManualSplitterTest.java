package org.study.jibeo.domain.equipmentmodel.service;

import org.junit.jupiter.api.Test;
import org.study.jibeo.domain.equipmentmodel.model.Manual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualSplitterTest {

    @Test
    void 제목마다_항목이_되고_본문_없는_제목은_건너뛴다() {
        Manual manual = ManualSplitter.split("""
                # 컴프레서 매뉴얼
                ## 1. 고장 대응
                ### 압력이 낮을 때
                필터를 확인합니다.
                ### 물이 샐 때 ###
                드레인 밸브를 잠급니다.
                ## 주의사항
                운전 중에는 커버를 열지 마세요.
                """, 3, "컴프레서 매뉴얼");

        assertThat(manual.version()).isEqualTo(3);
        assertThat(manual.sections()).extracting(Manual.Section::title)
                .containsExactly("압력이 낮을 때", "물이 샐 때", "주의사항");
        assertThat(manual.sections()).extracting(Manual.Section::id).containsExactly("s-001", "s-002", "s-003");
        assertThat(manual.sections()).extracting(Manual.Section::type).containsExactly(
                Manual.SectionType.TROUBLESHOOTING, Manual.SectionType.TROUBLESHOOTING, Manual.SectionType.SAFETY);
        assertThat(manual.sections().get(1).content()).isEqualTo("드레인 밸브를 잠급니다.");
    }

    @Test
    void 코드_블록_안의_샵은_제목이_아니다() {
        Manual manual = ManualSplitter.split("""
                ## 설정 파일
                ```
                # 주석입니다
                pressure=8
                ```
                """, 1, "x");

        assertThat(manual.sections()).singleElement()
                .satisfies(s -> assertThat(s.content()).contains("# 주석입니다", "pressure=8"));
    }

    @Test
    void 안전_밸브처럼_안전이_들어가도_수칙_경고가_아니면_검색_대상이다() {
        Manual manual = ManualSplitter.split("""
                ## 안전 밸브가 열릴 때
                압력 설정을 확인합니다.
                ## 안전 수칙
                전원을 차단합니다.
                ## 경고 표시
                경고등이 켜지면 멈춥니다.
                """, 1, "x");

        assertThat(manual.sections()).extracting(Manual.Section::type).containsExactly(
                Manual.SectionType.TROUBLESHOOTING, Manual.SectionType.SAFETY, Manual.SectionType.SAFETY);
    }

    @Test
    void 긴_본문은_문단_경계에서_나누고_AI_한도_12000자를_넘지_않는다() {
        String paragraph = "가".repeat(4000);
        String longLine = "나".repeat(15_000);
        Manual manual = ManualSplitter.split("## 긴 항목\n" + paragraph + "\n\n" + paragraph + "\n\n" + longLine,
                1, "x");

        assertThat(manual.sections()).allSatisfy(s -> assertThat(s.content().length()).isLessThanOrEqualTo(12_000));
        assertThat(manual.sections()).extracting(Manual.Section::title).first().isEqualTo("긴 항목 (1)");
        assertThat(String.join("", manual.sections().stream().map(Manual.Section::content).toList())
                .replace("\n", "")).isEqualTo(paragraph + paragraph + longLine);
    }

    @Test
    void 항목이_200개를_넘으면_거절한다() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            text.append("# ").append(i).append("\n내용\n");
        }

        assertThatThrownBy(() -> ManualSplitter.split(text.toString(), 1, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
