package org.study.jibeo.domain.equipmentmodel.service;

import org.junit.jupiter.api.Test;
import org.study.jibeo.domain.equipmentmodel.model.Manual;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualParserTest {

    @Test
    void 줄_머리표를_칸으로_읽는다() {
        List<ManualParser.Section> sections = ManualParser.parse("""
                # 컴프레서 사용 설명서
                ## 고장: 압력이 올라가지 않아요
                유형: 고장
                오류 코드: C-04, C-05
                증상: 압력계 바늘이 0.8 MPa까지 오르지 않습니다.
                가능한 원인:
                - 흡입 필터 막힘
                - 배관 이음부 누설
                조치:
                1. 드레인 밸브가 잠겨 있는지 확인합니다.
                2. 흡입 필터 표면의 먼지를 털어냅니다.
                담당자 호출: 필터가 깨끗한데도 압력이 오르지 않으면
                호출합니다.

                주의: 운전 중에는 필터를 만지지 마세요.
                근거: Ingersoll Rand 2475 설명서 고장 대응 표

                필터는 매주 청소합니다.
                """, "x");

        assertThat(sections).singleElement().satisfies(s -> {
            assertThat(s.key()).isEqualTo("s-001");
            assertThat(s.title()).isEqualTo("고장: 압력이 올라가지 않아요");
            assertThat(s.type()).isEqualTo(Manual.SectionType.TROUBLESHOOTING);
            assertThat(s.errorCodes()).containsExactly("C-04", "C-05");
            assertThat(s.symptoms()).containsExactly("압력계 바늘이 0.8 MPa까지 오르지 않습니다.");
            assertThat(s.causes()).containsExactly("흡입 필터 막힘", "배관 이음부 누설");
            assertThat(s.steps()).containsExactly("드레인 밸브가 잠겨 있는지 확인합니다.", "흡입 필터 표면의 먼지를 털어냅니다.");
            assertThat(s.escalation()).isEqualTo("필터가 깨끗한데도 압력이 오르지 않으면 호출합니다.");
            assertThat(s.cautions()).containsExactly("운전 중에는 필터를 만지지 마세요.");
            assertThat(s.source()).isEqualTo("Ingersoll Rand 2475 설명서 고장 대응 표");
            assertThat(s.description()).isEqualTo("필터는 매주 청소합니다.");
        });
    }

    @Test
    void AI용_항목에는_조치와_오류_코드가_들어가고_호출_기준은_주의사항_끝에_붙는다() {
        Manual.Section ai = ManualParser.parse("""
                ## 고장: 압력이 낮아요
                오류 코드: C-04
                증상: 압력이 낮음
                조치:
                1. 필터를 확인합니다.
                주의: 뜨거우니 조심하세요.
                담당자 호출: 계속되면 호출합니다.
                """, "x").getFirst().toAi();

        assertThat(ai.id()).isEqualTo("s-001");
        assertThat(ai.symptoms()).containsExactly("C-04", "압력이 낮음");
        assertThat(ai.steps()).containsExactly("필터를 확인합니다.");
        assertThat(ai.cautions()).containsExactly("뜨거우니 조심하세요.", "담당자 호출: 계속되면 호출합니다.");
        assertThat(ai.content()).startsWith("오류 코드: C-04");
    }

    @Test
    void 유형은_줄_머리표가_우선이고_없으면_제목으로_정한다() {
        List<ManualParser.Section> sections = ManualParser.parse("""
                ## 제품 개요
                유형: 개요
                6축 로봇입니다.
                ## 일상 점검
                유형: 점검
                1. 주변을 확인합니다.
                ## 정기 정비
                유형: 정비
                - 6개월마다 그리스 점검
                ## 고장 진단 시 안전 유의사항
                감전 위험이 있습니다.
                ## 로봇 고장 상담 시 안전 안내
                재가동은 담당자가 판단합니다.
                ## 안전 밸브가 열릴 때
                증상: 공기가 터져 나옴
                """, "x");

        assertThat(sections).extracting(ManualParser.Section::type).containsExactly(
                Manual.SectionType.OVERVIEW, Manual.SectionType.OPERATION, Manual.SectionType.MAINTENANCE,
                Manual.SectionType.SAFETY, Manual.SectionType.SAFETY, Manual.SectionType.TROUBLESHOOTING);
    }

    @Test
    void 알_수_없는_유형이나_너무_긴_목록은_거절한다() {
        assertThatThrownBy(() -> ManualParser.parse("## a\n유형: 기타\n내용", "x"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("기타");

        StringBuilder steps = new StringBuilder("## a\n조치:\n");
        for (int i = 1; i <= 31; i++) {
            steps.append(i).append(". 단계\n");
        }
        assertThatThrownBy(() -> ManualParser.parse(steps.toString(), "x"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("31개");
    }

    @Test
    void 코드_블록_안의_샵은_제목이_아니다() {
        List<ManualParser.Section> sections = ManualParser.parse("""
                ## 설정 파일
                ```
                # 주석입니다
                pressure=8
                ```
                """, "x");

        assertThat(sections).singleElement()
                .satisfies(s -> assertThat(s.content()).contains("# 주석입니다", "pressure=8"));
    }

    @Test
    void 줄_머리표가_없는_긴_본문은_문단_경계에서_나눈다() {
        String paragraph = "가".repeat(4000);
        String longLine = "나".repeat(15_000);
        List<ManualParser.Section> sections = ManualParser.parse(
                "## 긴 항목\n" + paragraph + "\n\n" + paragraph + "\n\n" + longLine, "x");

        assertThat(sections).allSatisfy(s -> assertThat(s.content().length()).isLessThanOrEqualTo(12_000));
        assertThat(sections).extracting(ManualParser.Section::title).first().isEqualTo("긴 항목 (1)");
    }

    @Test
    void 제목이_없으면_전체가_항목_하나이고_200개를_넘으면_거절한다() {
        assertThat(ManualParser.parse("제목 없는 매뉴얼", "ROBOT 매뉴얼"))
                .singleElement().extracting(ManualParser.Section::title).isEqualTo("ROBOT 매뉴얼");

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            text.append("# ").append(i).append("\n내용\n");
        }
        assertThatThrownBy(() -> ManualParser.parse(text.toString(), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
