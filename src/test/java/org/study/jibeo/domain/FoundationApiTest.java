package org.study.jibeo.domain;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.auth.RefreshTokenStore;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.model.ManualSection;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.equipmentmodel.repository.ManualSectionRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 1단계 기반 API: 회사 · 내 정보 · 모델/부품/매뉴얼 · 설치 설비
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FoundationApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private EquipmentModelRepository modelRepository;
    @Autowired
    private ManualSectionRepository sectionRepository;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    private String makerToken;
    private String otherMakerToken;
    private String workerToken;
    private String otherWorkerToken;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company otherMaker = company("다른 제작사", "100-00-00002", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        Company otherUserCompany = company("사용 업체 B", "200-00-00002", CompanyType.USER_COMPANY);

        makerToken = token(user("staff1", UserRole.STAFF, maker));
        otherMakerToken = token(user("staff2", UserRole.STAFF, otherMaker));
        workerToken = token(user("worker1", UserRole.WORKER, userCompany));
        otherWorkerToken = token(user("worker2", UserRole.WORKER, otherUserCompany));
    }

    @Test
    void 회사_목록은_로그인_없이_타입별로_조회된다() throws Exception {
        mockMvc.perform(get("/api/companies").param("type", "MAKER_COMPANY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].type").value(hasItem("MAKER_COMPANY")));
    }

    @Test
    void 회사를_등록하고_가입한_뒤_그_회사로_역할을_고른다() throws Exception {
        long companyId = idOf(mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"새 사용 업체","located":"광주","businessNumber":"300-00-00001",
                                 "type":"USER_COMPANY","phone":"062-555-6666"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새 사용 업체"))
                .andReturn().getResponse().getContentAsString());

        // 가입(역할 미정) → 로그인 → 등록한 회사로 역할 선택
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"newworker","pw":"password123","name":"새담당","phone":"010-5555-6666"}
                                """))
                .andExpect(status().isOk());
        String pendingToken = JsonPath.read(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"newworker","pw":"password123"}
                                """))
                .andReturn().getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(auth(post("/api/me/role"), pendingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"WORKER","companyId":%d}
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyId").value(companyId));

        // 같은 사업자등록번호는 다시 등록할 수 없다
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"중복","located":"광주","businessNumber":"300-00-00001",
                                 "type":"USER_COMPANY","phone":"062-555-6666"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void 내_정보를_조회하고_연락처를_수정한다() throws Exception {
        mockMvc.perform(auth(get("/api/me"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("worker1"))
                .andExpect(jsonPath("$.data.company.name").value("사용 업체 A"));

        mockMvc.perform(auth(patch("/api/me"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"김담당","phone":"010-9999-8888","email":"kim@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김담당"))
                .andExpect(jsonPath("$.data.phone").value("010-9999-8888"));

        mockMvc.perform(auth(patch("/api/me"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"김담당","phone":"not-a-phone"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("phone"));
    }

    @Test
    void 제작_업체가_모델_부품_매뉴얼을_등록하고_사용_업체가_설비로_등록한다() throws Exception {
        // 제작 업체: 모델 → 부품 → 매뉴얼
        long modelId = idOf(mockMvc.perform(auth(post("/api/maker/models"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WM-20","name":"산업용 세탁기 20kg","category":"산업용 세탁기"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("WM-20"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(auth(post("/api/maker/models/" + modelId + "/parts"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"P-07","name":"배수 필터","locationDesc":"전면 하단 왼쪽 커버 안쪽","meshName":"drain_filter"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("P-07"));

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual(MANUAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.sections.length()").value(3));

        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/manual"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value(MANUAL))
                .andExpect(jsonPath("$.data.sections[1].title").value("탈수가 안 되고 물이 남을 때"));

        // 사용 업체: 모델 검색 → 상세 → 설비 등록 → 목록
        mockMvc.perform(auth(get("/api/models").param("q", "wm-2"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("WM-20"))
                .andExpect(jsonPath("$.data[0].makerCompanyName").value("제작사"));

        mockMvc.perform(auth(get("/api/models/" + modelId), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partCount").value(1))
                .andExpect(jsonPath("$.data.repairHistoryCount").value(0));

        long equipmentId = idOf(mockMvc.perform(auth(post("/api/equipments"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"modelId":%d,"name":"1층 세탁실 1호기","location":"본관 1층","serialNumber":"SN-31877-C"}
                                """.formatted(modelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model.code").value("WM-20"))
                .andExpect(jsonPath("$.data.parts[0].meshName").value("drain_filter"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(auth(get("/api/equipments"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("1층 세탁실 1호기"));

        // 다른 사용 업체는 이 설비를 볼 수 없다
        mockMvc.perform(auth(get("/api/equipments/" + equipmentId), otherWorkerToken))
                .andExpect(status().isNotFound());

        // 제작 업체 모델 목록에 설치 대수가 집계된다
        mockMvc.perform(auth(get("/api/maker/models"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].installedCount").value(1))
                .andExpect(jsonPath("$.data[0].repairHistoryCount").value(0));
    }

    @Test
    void 매뉴얼_전문을_제목_기준으로_나눠_AI_검색_항목으로_저장한다() throws Exception {
        long modelId = createModel("WM-35");

        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/manual"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").doesNotExist())
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.sections.length()").value(0));

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual(MANUAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections[0].id").value("s-001"))
                .andExpect(jsonPath("$.data.sections[0].title").value("개요"))
                .andExpect(jsonPath("$.data.sections[0].type").value("TROUBLESHOOTING"))
                .andExpect(jsonPath("$.data.sections[1].title").value("탈수가 안 되고 물이 남을 때"))
                .andExpect(jsonPath("$.data.sections[1].errorCodes[0]").value("E-07"))
                .andExpect(jsonPath("$.data.sections[1].causes[0]").value("배수 필터 막힘"))
                .andExpect(jsonPath("$.data.sections[1].steps.length()").value(2))
                .andExpect(jsonPath("$.data.sections[1].escalation").value("청소 후에도 물이 남으면 호출합니다."))
                .andExpect(jsonPath("$.data.sections[1].source").value("세탁기 설명서 고장 대응 표"))
                .andExpect(jsonPath("$.data.sections[2].title").value("안전 수칙"))
                .andExpect(jsonPath("$.data.sections[2].type").value("SAFETY"));

        // 항목 테이블에도 같은 칸으로 저장된다
        assertThat(sectionRepository.findAllByEquipmentModelIdOrderBySortOrderAsc(modelId))
                .extracting(ManualSection::getSectionKey).containsExactly("s-001", "s-002", "s-003");

        // AI 가 읽는 jsonb 는 기존 항목 형식 그대로이고, 조치가 steps 로 들어간다
        Manual saved = modelRepository.findById(modelId).orElseThrow().getManual();
        assertThat(saved.sections().get(1).steps()).containsExactly("전원을 끕니다.", "배수 필터를 청소합니다.");
        assertThat(saved.sections().get(1).symptoms()).containsExactly("E-07", "탈수 단계에서 멈추고 물이 남습니다.");
        assertThat(saved.sections().get(1).cautions()).containsExactly("담당자 호출: 청소 후에도 물이 남으면 호출합니다.");

        // 다시 저장하면 버전이 오른다
        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual("제목 없이 쓴 매뉴얼입니다.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.sections[0].title").value("모델 매뉴얼"));

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual("  ")))
                .andExpect(status().isBadRequest());

        String tooMany = java.util.stream.IntStream.rangeClosed(1, 201)
                .mapToObj(i -> "# 항목 " + i + "\n내용").collect(java.util.stream.Collectors.joining("\n"));
        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual(tooMany)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("201개")));
    }

    @Test
    void 다른_제작_업체의_모델은_수정할_수_없고_중복_코드는_409() throws Exception {
        long modelId = createModel("DR-50");

        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/manual"), otherMakerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(auth(post("/api/maker/models"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DR-50","name":"대형 건조기","category":"건조기"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void 모델_코드가_붙은_긴_부품_코드도_등록할_수_있다() throws Exception {
        long modelId = createModel("COMPRESSOR-001");

        mockMvc.perform(auth(post("/api/maker/models/" + modelId + "/parts"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"COMPRESSOR-001:PRESSURE_GAUGE","name":"압력계"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("COMPRESSOR-001:PRESSURE_GAUGE"));

        mockMvc.perform(auth(post("/api/maker/models/" + modelId + "/parts"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"x\"}".formatted("P".repeat(65))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 역할에_맞지_않는_API는_403() throws Exception {
        mockMvc.perform(auth(get("/api/maker/models"), workerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(auth(get("/api/equipments"), makerToken))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private long createModel(String code) throws Exception {
        return idOf(mockMvc.perform(auth(post("/api/maker/models"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"모델","category":"산업용 세탁기"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private static final String MANUAL = """
            산업용 세탁기 매뉴얼입니다.

            ## 탈수가 안 되고 물이 남을 때
            유형: 고장
            오류 코드: E-07
            증상: 탈수 단계에서 멈추고 물이 남습니다.
            가능한 원인:
            - 배수 필터 막힘
            조치:
            1. 전원을 끕니다.
            2. 배수 필터를 청소합니다.
            담당자 호출: 청소 후에도 물이 남으면 호출합니다.
            근거: 세탁기 설명서 고장 대응 표

            ## 안전 수칙
            점검 전 전원을 차단하세요.
            """;

    private static String manual(String content) {
        return JsonMapper.builder().build().writeValueAsString(java.util.Map.of("content", content));
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.data.id")).longValue();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private Company company(String name, String businessNumber, CompanyType type) {
        return companyRepository.save(Company.builder()
                .name(name).located("광주").businessNumber(businessNumber).type(type).phone("062-123-4567")
                .build());
    }

    private User user(String loginId, UserRole role, Company company) {
        return userRepository.save(User.builder()
                .loginId(loginId).pw("{noop}x").role(role).company(company).name("홍길동").phone("010-1234-5678")
                .build());
    }

    private String token(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompany().getId());
    }
}
