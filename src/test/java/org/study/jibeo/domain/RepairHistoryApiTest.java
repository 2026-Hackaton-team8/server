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
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수리 이력: 배정된 호출에 기록 → 호출 COMPLETED → 모델·설비 수리 이력과 집계에 반영
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepairHistoryApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentModelRepository modelRepository;
    @Autowired
    private PartRepository partRepository;
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
    private Long modelId;
    private Long partId;
    private Long otherModelPartId;
    private Long equipmentId;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company otherMaker = company("다른 제작사", "100-00-00002", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        Company otherUserCompany = company("사용 업체 B", "200-00-00002", CompanyType.USER_COMPANY);

        User staff = user("staff1", UserRole.STAFF, maker, "김엔지");
        makerToken = token(staff);
        engineerId = staff.getId();
        otherMakerToken = token(user("staff2", UserRole.STAFF, otherMaker, "박엔지"));
        workerToken = token(user("worker1", UserRole.WORKER, userCompany, "홍길동"));
        otherWorkerToken = token(user("worker2", UserRole.WORKER, otherUserCompany, "이영희"));

        EquipmentModel model = model("WM-20", maker);
        modelId = model.getId();
        partId = part(model, "P-07", "배수 필터").getId();
        otherModelPartId = part(model("DR-50", maker), "P-01", "히터").getId();
        equipmentId = equipmentRepository.save(Equipment.builder()
                .company(userCompany).equipmentModel(model).name("1층 세탁실 1호기").location("본관 1층")
                .build()).getId();
    }

    @Test
    void 수리_이력을_기록하면_호출이_완료되고_이력과_집계에_반영된다() throws Exception {
        long requestId = assignedRequest();

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/repair-history"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody(partId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.part.code").value("P-07"))
                .andExpect(jsonPath("$.data.difficulty").value("SELF"))
                .andExpect(jsonPath("$.data.engineerName").value("김엔지"))
                .andExpect(jsonPath("$.data.repairedAt").value(LocalDate.now(ZoneId.of("Asia/Seoul")).toString()));

        // 호출은 수리 완료
        mockMvc.perform(auth(get("/api/requests/" + requestId), workerToken))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(auth(get("/api/maker/requests"), makerToken))
                .andExpect(jsonPath("$.data.counts.COMPLETED").value(1))
                .andExpect(jsonPath("$.data.counts.ASSIGNED").value(0));

        // 제작 업체: 모델 수리 이력 (이용 기업 이름 포함)
        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/repair-histories"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].equipment.companyName").value("사용 업체 A"))
                .andExpect(jsonPath("$.data[0].cause").value("배수 필터에 이물질이 막힘"));

        // 사용 업체: 설비 수리 이력
        mockMvc.perform(auth(get("/api/equipments/" + equipmentId + "/repair-histories"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].requestId").value(requestId));

        // 모델 목록·상세 집계
        mockMvc.perform(auth(get("/api/maker/models"), makerToken))
                .andExpect(jsonPath("$.data[?(@.code == 'WM-20')].repairHistoryCount").value(1));
        mockMvc.perform(auth(get("/api/models/" + modelId), workerToken))
                .andExpect(jsonPath("$.data.repairHistoryCount").value(1));
    }

    @Test
    void 배정_전이거나_이미_완료된_호출에는_기록할_수_없다() throws Exception {
        long received = createRequest();
        mockMvc.perform(auth(post("/api/maker/requests/" + received + "/repair-history"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody(partId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("엔지니어를 배정한 호출만 수리 이력을 기록할 수 있습니다."));

        long assigned = assignedRequest();
        mockMvc.perform(auth(post("/api/maker/requests/" + assigned + "/repair-history"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody(partId)))
                .andExpect(status().isOk());
        mockMvc.perform(auth(post("/api/maker/requests/" + assigned + "/repair-history"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody(partId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 종료된 호출입니다."));
    }

    @Test
    void 다른_모델의_부품이나_빈_값은_400() throws Exception {
        long requestId = assignedRequest();

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/repair-history"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody(otherModelPartId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이 설비 모델의 부품이 아닙니다."));

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/repair-history"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId":%d,"cause":"","difficulty":"SELF"}
                                """.formatted(partId)))
                .andExpect(status().isBadRequest());

        // 실패한 요청은 호출 상태를 바꾸지 않는다
        mockMvc.perform(auth(get("/api/maker/requests/" + requestId), makerToken))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test
    void 다른_회사의_호출_모델_설비_이력은_404() throws Exception {
        long requestId = assignedRequest();

        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/repair-history"), otherMakerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody(partId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/repair-histories"), otherMakerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/equipments/" + equipmentId + "/repair-histories"), otherWorkerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/repair-histories"), workerToken))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private long assignedRequest() throws Exception {
        long requestId = createRequest();
        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/assign"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"engineerId":%d,"visitAt":"2026-11-06T14:00:00"}
                                """.formatted(engineerId)))
                .andExpect(status().isOk());
        return requestId;
    }

    private long createRequest() throws Exception {
        String json = mockMvc.perform(auth(post("/api/requests"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentId":%d,"symptomDetail":"탈수 단계에서 멈춥니다.",
                                 "equipmentState":"STOPPED","urgency":"HIGH",
                                 "contact":{"reporterName":"홍길동","reporterPhone":"010-2222-3333","companyPhone":"062-111-2222"}}
                                """.formatted(equipmentId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.data.id")).longValue();
    }

    private static String historyBody(Long partId) {
        return """
                {"partId":%d,"cause":"배수 필터에 이물질이 막힘","solution":"배수 필터를 분리해 세척 후 재조립","difficulty":"SELF"}
                """.formatted(partId);
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

    private EquipmentModel model(String code, Company maker) {
        return modelRepository.save(EquipmentModel.builder()
                .code(code).name("모델 " + code).category("산업용 세탁기").makerCompany(maker).build());
    }

    private Part part(EquipmentModel model, String code, String name) {
        return partRepository.save(Part.builder().equipmentModel(model).code(code).name(name).build());
    }

    private String token(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompany().getId());
    }
}
