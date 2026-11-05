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
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

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
                        .content(manual("drain-not-working", "P-07")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections[0].id").value("drain-not-working"));

        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/manual"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections[0].partCodes[0]").value("P-07"));

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
    void 매뉴얼은_등록된_부품_코드와_중복없는_섹션_id만_허용한다() throws Exception {
        long modelId = createModel("WM-35");

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual("drain-not-working", "P-99")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("P-99")));

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":1,"sections":[
                                  {"id":"a","title":"A","type":"OVERVIEW","content":"x"},
                                  {"id":"a","title":"B","type":"OVERVIEW","content":"y"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("중복")));

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":1,"sections":[{"id":"Bad Id","title":"","type":"OVERVIEW","content":"x"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값이 유효하지 않습니다."));
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
    void 모델_코드가_붙은_긴_부품_코드도_등록하고_매뉴얼에서_쓸_수_있다() throws Exception {
        long modelId = createModel("COMPRESSOR-001");

        mockMvc.perform(auth(post("/api/maker/models/" + modelId + "/parts"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"COMPRESSOR-001:PRESSURE_GAUGE","name":"압력계"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("COMPRESSOR-001:PRESSURE_GAUGE"));

        mockMvc.perform(auth(put("/api/maker/models/" + modelId + "/manual"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manual("pressure-low", "COMPRESSOR-001:PRESSURE_GAUGE")))
                .andExpect(status().isOk());

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

    private static String manual(String sectionId, String partCode) {
        return """
                {"version":1,"sections":[{
                  "id":"%s","title":"탈수가 안 되고 물이 남을 때","type":"TROUBLESHOOTING",
                  "symptoms":["탈수 안 됨"],"partCodes":["%s"],
                  "content":"배수 필터가 막히면 물이 빠지지 않습니다.",
                  "steps":["전원을 끕니다.","배수 필터를 청소합니다."],"cautions":["전원을 차단하세요."]}]}
                """.formatted(sectionId, partCode);
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
