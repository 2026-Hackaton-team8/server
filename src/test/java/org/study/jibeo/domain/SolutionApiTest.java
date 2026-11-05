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
import org.springframework.test.web.servlet.ResultActions;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 솔루션: 수리 이력 → AI 초안(임시 AI 는 SELF 이력의 해결 방법을 단계로 씀) → 수정 → 승인 → 내리기
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolutionApiTest {

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
    private Long engineerId;
    private Long modelId;
    private Long equipmentId;
    private Long otherModelEquipmentId;
    private Long partId;
    private Long otherPartId;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company otherMaker = company("다른 제작사", "100-00-00002", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        User staff = user("staff1", UserRole.STAFF, maker, "김엔지");
        makerToken = token(staff);
        engineerId = staff.getId();
        otherMakerToken = token(user("staff2", UserRole.STAFF, otherMaker, "박엔지"));
        workerToken = token(user("worker1", UserRole.WORKER, userCompany, "홍길동"));

        EquipmentModel model = model("WM-20", maker);
        EquipmentModel otherModel = model("DR-50", maker);
        modelId = model.getId();
        partId = partRepository.save(Part.builder().equipmentModel(model).code("P-07").name("배수 필터").build()).getId();
        otherPartId = partRepository.save(Part.builder().equipmentModel(otherModel).code("P-01").name("히터").build()).getId();
        equipmentId = equipment(userCompany, model);
        otherModelEquipmentId = equipment(userCompany, otherModel);
    }

    @Test
    void 수리_이력으로_초안을_만들고_수정_승인_내리기() throws Exception {
        long self = repairHistory(equipmentId, partId, "배수 필터를 분리해 세척", "SELF");
        long engineer = repairHistory(equipmentId, partId, "배수 펌프 교체", "ENGINEER");

        long id = idOf(draft(self, engineer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.steps.length()").value(1))
                .andExpect(jsonPath("$.data.steps[0]").value("배수 필터를 분리해 세척"))
                .andExpect(jsonPath("$.data.evidence.length()").value(1))
                .andExpect(jsonPath("$.data.evidence[0].id").value(self))
                .andExpect(jsonPath("$.data.category").value("배수 필터 관련")));

        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/solutions"), makerToken))
                .andExpect(jsonPath("$.data.counts.DRAFT").value(1))
                .andExpect(jsonPath("$.data.content[0].stepCount").value(1));
        mockMvc.perform(auth(get("/api/maker/models"), makerToken))
                .andExpect(jsonPath("$.data[?(@.code == 'WM-20')].draftSolutionCount").value(1));

        mockMvc.perform(auth(put("/api/maker/solutions/" + id), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"탈수가 안 되고 통에 물이 남을 때","category":"배수 계통","symptoms":["탈수 안 됨"],
                                 "steps":["전원을 끕니다.","배수 필터를 분리해 세척합니다."],"fallback":"담당자를 호출하세요."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps.length()").value(2))
                .andExpect(jsonPath("$.data.steps[1]").value("배수 필터를 분리해 세척합니다."))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(auth(post("/api/maker/solutions/" + id + "/approve"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedByName").value("김엔지"));
        mockMvc.perform(auth(post("/api/maker/solutions/" + id + "/approve"), makerToken))
                .andExpect(status().isConflict());
        mockMvc.perform(auth(get("/api/maker/models"), makerToken))
                .andExpect(jsonPath("$.data[?(@.code == 'WM-20')].approvedSolutionCount").value(1));

        mockMvc.perform(auth(post("/api/maker/solutions/" + id + "/withdraw"), makerToken))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
        mockMvc.perform(auth(put("/api/maker/solutions/" + id), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"category\":\"c\",\"steps\":[\"s\"]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(auth(post("/api/maker/solutions/" + id + "/approve"), makerToken))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/solutions").param("status", "DRAFT"), makerToken))
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.counts.APPROVED").value(1));
    }

    @Test
    void 다른_모델이나_없는_수리_이력으로는_초안을_만들_수_없다() throws Exception {
        long mine = repairHistory(equipmentId, partId, "필터 세척", "SELF");
        long otherModel = repairHistory(otherModelEquipmentId, otherPartId, "히터 교체", "ENGINEER");

        draft(mine, otherModel)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이 모델의 수리 이력이 아닙니다."));
        draft(mine, 999_999L).andExpect(status().isBadRequest());
        mockMvc.perform(auth(post("/api/maker/models/" + modelId + "/solutions/draft"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"repairHistoryIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("repairHistoryIds"));
    }

    @Test
    void 다른_제작_업체는_404_사용_업체는_403() throws Exception {
        long id = idOf(draft(repairHistory(equipmentId, partId, "필터 세척", "SELF")));

        mockMvc.perform(auth(get("/api/maker/solutions/" + id), otherMakerToken)).andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/maker/solutions/" + id + "/approve"), otherMakerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/maker/models/" + modelId + "/solutions"), otherMakerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/maker/solutions/" + id), workerToken)).andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private ResultActions draft(Long... repairHistoryIds) throws Exception {
        String ids = String.join(",", java.util.Arrays.stream(repairHistoryIds).map(String::valueOf).toList());
        return mockMvc.perform(auth(post("/api/maker/models/" + modelId + "/solutions/draft"), makerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repairHistoryIds\":[" + ids + "]}"));
    }

    /** 호출 → 배정 → 수리 이력 기록 */
    private long repairHistory(Long equipmentId, Long partId, String solution, String difficulty) throws Exception {
        long requestId = idOf(mockMvc.perform(auth(post("/api/requests"), workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"equipmentId":%d,"symptomDetail":"탈수 단계에서 멈춥니다.","equipmentState":"STOPPED","urgency":"HIGH",
                         "contact":{"reporterName":"홍길동","reporterPhone":"010-2222-3333","companyPhone":"062-111-2222"}}
                        """.formatted(equipmentId))));
        mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/assign"), makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"engineerId\":%d,\"visitAt\":\"2026-11-06T14:00:00\"}".formatted(engineerId)))
                .andExpect(status().isOk());
        return idOf(mockMvc.perform(auth(post("/api/maker/requests/" + requestId + "/repair-history"), makerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partId\":%d,\"cause\":\"막힘\",\"solution\":\"%s\",\"difficulty\":\"%s\"}"
                        .formatted(partId, solution, difficulty))));
    }

    private static long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private EquipmentModel model(String code, Company maker) {
        return modelRepository.save(EquipmentModel.builder()
                .code(code).name("모델 " + code).category("세탁기").makerCompany(maker).build());
    }

    private Long equipment(Company company, EquipmentModel model) {
        return equipmentRepository.save(Equipment.builder()
                .company(company).equipmentModel(model).name("설비 " + model.getCode()).location("1층").build()).getId();
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
        return tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompanyId());
    }
}
