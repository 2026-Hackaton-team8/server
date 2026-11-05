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
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 2단계 담당자 호출: 사용 업체 호출 제출 → 제작 업체 수신함 → 엔지니어 배정 / 자가 해결
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RequestApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentModelRepository modelRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    private String makerToken;
    private String otherMakerToken;
    private String workerToken;
    private String otherWorkerToken;
    private Long engineerId;
    private Long otherMakerEngineerId;
    private Long workerId;
    private Long equipmentId;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company otherMaker = company("다른 제작사", "100-00-00002", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        Company otherUserCompany = company("사용 업체 B", "200-00-00002", CompanyType.USER_COMPANY);

        User staff = user("staff1", UserRole.STAFF, maker, "김엔지");
        User otherStaff = user("staff2", UserRole.STAFF, otherMaker, "박엔지");
        User worker = user("worker1", UserRole.WORKER, userCompany, "홍길동");
        makerToken = token(staff);
        otherMakerToken = token(otherStaff);
        workerToken = token(worker);
        otherWorkerToken = token(user("worker2", UserRole.WORKER, otherUserCompany, "이영희"));
        engineerId = user("staff3", UserRole.STAFF, maker, "가엔지").getId();
        otherMakerEngineerId = otherStaff.getId();
        workerId = worker.getId();

        EquipmentModel model = modelRepository.save(EquipmentModel.builder()
                .code("WM-20").name("산업용 세탁기 20kg").category("산업용 세탁기").makerCompany(maker).build());
        equipmentId = equipmentRepository.save(Equipment.builder()
                .company(userCompany).equipmentModel(model).name("1층 세탁실 1호기").location("본관 1층")
                .build()).getId();
    }

    @Test
    void 호출을_제출하면_요청번호가_발급되고_제작_업체_수신함에_들어간다() throws Exception {
        long requestId = createRequest(workerToken, equipmentId);

        mockMvc.perform(auth(get("/api/requests/" + requestId), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestNo").value(matchesPattern("AS-\\d{4,}")))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.callReason").value("NEEDS_EXPERT"))
                .andExpect(jsonPath("$.data.contact.reporterPhone").value("010-2222-3333"))
                .andExpect(jsonPath("$.data.aiSummary.cause").value("배수 필터 막힘"))
                .andExpect(jsonPath("$.data.equipment.modelCode").value("WM-20"))
                .andExpect(jsonPath("$.data.engineer").doesNotExist());

        mockMvc.perform(auth(get("/api/requests"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].symptomSummary").value("탈수가 안 됨"));

        mockMvc.perform(auth(get("/api/maker/requests"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.ALL").value(1))
                .andExpect(jsonPath("$.data.counts.RECEIVED").value(1))
                .andExpect(jsonPath("$.data.counts.ASSIGNED").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].companyName").value("사용 업체 A"))
                .andExpect(jsonPath("$.data.content[0].urgency").value("HIGH"));

        mockMvc.perform(auth(get("/api/maker/requests/" + requestId), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contact.companyPhone").value("062-111-2222"))
                .andExpect(jsonPath("$.data.symptomDetail").value("아침 첫 세탁에서 탈수 단계에 멈췄습니다."));
    }

    @Test
    void 제출_시점_연락처를_저장해서_프로필을_바꿔도_호출은_그대로다() throws Exception {
        long requestId = createRequest(workerToken, equipmentId);

        User worker = userRepository.findById(workerId).orElseThrow();
        worker.updateContact("바뀐이름", "010-0000-0000", null);
        userRepository.flush();

        mockMvc.perform(auth(get("/api/maker/requests/" + requestId), makerToken))
                .andExpect(jsonPath("$.data.contact.reporterName").value("홍길동"))
                .andExpect(jsonPath("$.data.contact.reporterPhone").value("010-2222-3333"));
    }

    @Test
    void 엔지니어를_배정하면_ASSIGNED_이고_탭별_개수가_바뀐다() throws Exception {
        long requestId = createRequest(workerToken, equipmentId);

        mockMvc.perform(auth(get("/api/maker/engineers"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("가엔지"));

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/assign"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"engineerId":%d,"visitAt":"2026-11-06T14:00:00"}
                                """.formatted(engineerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.engineer.name").value("가엔지"))
                .andExpect(jsonPath("$.data.visitAt").value("2026-11-06T14:00:00"));

        // 사용 업체도 배정 결과를 본다
        mockMvc.perform(auth(get("/api/requests/" + requestId), workerToken))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.engineer.phone").value("010-1234-5678"));

        mockMvc.perform(auth(get("/api/maker/requests").param("status", "ASSIGNED"), makerToken))
                .andExpect(jsonPath("$.data.counts.RECEIVED").value(0))
                .andExpect(jsonPath("$.data.counts.ASSIGNED").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(auth(get("/api/maker/requests").param("status", "RECEIVED"), makerToken))
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.counts.ALL").value(1));
    }

    @Test
    void 자가_해결로_종료하면_더_이상_배정할_수_없다() throws Exception {
        long requestId = createRequest(workerToken, equipmentId);

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/self-resolved"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SELF_RESOLVED"));

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/assign"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"engineerId":%d,"visitAt":"2026-11-06T14:00:00"}
                                """.formatted(engineerId)))
                .andExpect(status().isConflict());
    }

    @Test
    void 다른_회사의_엔지니어는_배정할_수_없다() throws Exception {
        long requestId = createRequest(workerToken, equipmentId);

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/assign"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"engineerId":%d,"visitAt":"2026-11-06T14:00:00"}
                                """.formatted(otherMakerEngineerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("배정할 수 없는 엔지니어입니다."));

        // 사용 업체 직원(WORKER)도 엔지니어가 될 수 없다
        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/assign"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"engineerId":%d,"visitAt":"2026-11-06T14:00:00"}
                                """.formatted(workerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 다른_회사의_호출과_설비는_404() throws Exception {
        long requestId = createRequest(workerToken, equipmentId);

        // 다른 사용 업체: 남의 호출 조회, 남의 설비로 호출
        mockMvc.perform(auth(get("/api/requests/" + requestId), otherWorkerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/requests"), otherWorkerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(equipmentId)))
                .andExpect(status().isNotFound());

        // 다른 제작 업체: 수신함에 없고 상세·배정도 404
        mockMvc.perform(auth(get("/api/maker/requests"), otherMakerToken))
                .andExpect(jsonPath("$.data.counts.ALL").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(auth(get("/api/maker/requests/" + requestId), otherMakerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/self-resolved"), otherMakerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 필수값과_연락처_형식을_검사하고_역할이_다르면_403() throws Exception {
        mockMvc.perform(auth(post("/api/requests"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentId":%d,"symptomDetail":"멈춤","equipmentState":"STOPPED","urgency":"HIGH",
                                 "contact":{"reporterName":"홍길동","reporterPhone":"not-a-phone","companyPhone":"062-111-2222"}}
                                """.formatted(equipmentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("contact.reporterPhone"));

        mockMvc.perform(auth(post("/api/requests"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentId":%d,"equipmentState":"STOPPED","urgency":"HIGH"}
                                """.formatted(equipmentId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(auth(get("/api/maker/requests").param("size", "1000"), makerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(auth(get("/api/requests"), makerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/maker/requests"), workerToken))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private long createRequest(String token, Long equipmentId) throws Exception {
        String json = mockMvc.perform(auth(post("/api/requests"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(equipmentId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.data.id")).longValue();
    }

    private static String requestBody(Long equipmentId) {
        return """
                {"equipmentId":%d,
                 "aiSummary":{"symptom":"탈수가 안 됨","cause":"배수 필터 막힘","triedAction":"배수 필터 청소 후 해결 안 됨"},
                 "symptomDetail":"아침 첫 세탁에서 탈수 단계에 멈췄습니다.",
                 "equipmentState":"STOPPED","urgency":"HIGH",
                 "contact":{"reporterName":"홍길동","reporterPhone":"010-2222-3333","companyPhone":"062-111-2222"}}
                """.formatted(equipmentId);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private Company company(String name, String businessNumber, CompanyType type) {
        return companyRepository.save(Company.builder()
                .name(name).located("광주").businessNumber(businessNumber).type(type).phone("062-123-4567")
                .build());
    }

    private User user(String loginId, UserRole role, Company company, String name) {
        return userRepository.save(User.builder()
                .loginId(loginId).pw("{noop}x").role(role).company(company).name(name).phone("010-1234-5678")
                .build());
    }

    private String token(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompany().getId());
    }
}
