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
import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 상담: 임시 AI(StubAi)는 질문 2번 뒤 매뉴얼의 문제 해결 섹션으로 해결책을 내고, 매뉴얼이 없으면 호출을 권한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsultationApiTest {

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

    private String workerToken;
    private String otherWorkerToken;
    private String makerToken;
    private Long equipmentId;
    private Long noManualEquipmentId;
    private Long otherEquipmentId;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        Company otherCompany = company("사용 업체 B", "200-00-00002", CompanyType.USER_COMPANY);
        workerToken = token(user("worker1", UserRole.WORKER, userCompany, "홍길동"));
        otherWorkerToken = token(user("worker2", UserRole.WORKER, otherCompany, "이영희"));
        makerToken = token(user("staff1", UserRole.STAFF, maker, "김엔지"));

        EquipmentModel model = modelRepository.save(EquipmentModel.builder()
                .code("WM-20").name("산업용 세탁기").category("세탁기").makerCompany(maker).build());
        partRepository.save(Part.builder().equipmentModel(model).code("P-07").name("배수 필터")
                .meshName("drain_filter").build());
        model.updateManual(new Manual(1, List.of(new Manual.Section("drain-not-working", "배수 필터 막힘",
                Manual.SectionType.TROUBLESHOOTING, List.of("탈수 안 됨"), List.of("P-07"),
                "배수 필터가 막히면 물이 빠지지 않습니다.", List.of("전원을 끕니다.", "배수 필터를 청소합니다."),
                List.of("물이 쏟아질 수 있어요.")))));
        EquipmentModel noManual = modelRepository.save(EquipmentModel.builder()
                .code("DR-50").name("건조기").category("건조기").makerCompany(maker).build());

        equipmentId = equipment(userCompany, model, "1호기");
        noManualEquipmentId = equipment(userCompany, noManual, "건조기 1호기");
        otherEquipmentId = equipment(userCompany, model, "2호기");
    }

    @Test
    void 질문에_답하면_해결책이_나오고_해결이_안되면_SOLUTION_FAILED_로_호출된다() throws Exception {
        long id = idOf(start(equipmentId, "탈수가 안 되고 물이 남아요")
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.questionCount").value(1))
                .andExpect(jsonPath("$.data.canAnswer").value(true))
                .andExpect(jsonPath("$.data.messages[0].type").value("SYMPTOM"))
                .andExpect(jsonPath("$.data.messages[1].type").value("QUESTION"))
                .andExpect(jsonPath("$.data.messages[1].payload.options[0]").value("오늘")));

        answer(id, "오늘 아침부터요").andExpect(jsonPath("$.data.questionCount").value(2));
        answer(id, "아니요")
                .andExpect(jsonPath("$.data.messages[5].type").value("SOLUTION"))
                .andExpect(jsonPath("$.data.messages[5].payload.steps[1]").value("배수 필터를 청소합니다."))
                .andExpect(jsonPath("$.data.messages[5].payload.evidence.manualSectionIds[0]").value("drain-not-working"))
                .andExpect(jsonPath("$.data.messages[5].payload.evidence.manualSources[0].sectionId").value("drain-not-working"))
                .andExpect(jsonPath("$.data.aiCause").value("배수 필터 막힘"))
                .andExpect(jsonPath("$.data.suspectedPart.meshName").value("drain_filter"))
                .andExpect(jsonPath("$.data.canAnswer").value(false))
                .andExpect(jsonPath("$.data.canResolve").value(true));

        // 해결책이 나온 뒤에는 답할 수 없다
        answer(id, "더 물어봐 주세요").andExpect(status().isConflict());

        mockMvc.perform(auth(get("/api/consultations/" + id + "/request-draft"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.callReason").value("SOLUTION_FAILED"))
                .andExpect(jsonPath("$.data.aiSummary.cause").value("배수 필터 막힘"))
                .andExpect(jsonPath("$.data.contact.reporterName").value("홍길동"))
                .andExpect(jsonPath("$.data.contact.companyPhone").value("062-123-4567"))
                .andExpect(jsonPath("$.data.suspectedPart.code").value("P-07"));

        long requestId = callFrom(id, equipmentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.callReason").value("SOLUTION_FAILED"))
                .andExpect(jsonPath("$.data.consultationId").value(id))
                .andReturn().getResponse().getContentAsString().transform(ConsultationApiTest::idOf);

        mockMvc.perform(auth(get("/api/consultations/" + id), workerToken))
                .andExpect(jsonPath("$.data.status").value("ESCALATED"))
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.canResolve").value(false));

        // 제작 업체는 호출에서 대화 전체를 본다
        mockMvc.perform(auth(get("/api/maker/requests/" + requestId + "/conversation"), makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[2].content").value("오늘 아침부터요"));

        // 끝난 상담은 다시 호출하거나 해결할 수 없다
        callFrom(id, equipmentId).andExpect(status().isConflict());
        mockMvc.perform(auth(post("/api/consultations/" + id + "/resolve"), workerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void 매뉴얼이_없으면_AI가_호출을_권하고_그_사유로_호출된다() throws Exception {
        long id = idOf(start(noManualEquipmentId, "건조가 안 돼요"));
        answer(id, "어제부터요");
        answer(id, "아니요")
                .andExpect(jsonPath("$.data.messages[5].type").value("ESCALATE"))
                .andExpect(jsonPath("$.data.messages[5].payload.reason").value("NO_HISTORY"))
                .andExpect(jsonPath("$.data.suspectedPart").doesNotExist());

        callFrom(id, noManualEquipmentId)
                .andExpect(jsonPath("$.data.callReason").value("NO_HISTORY"));
    }

    @Test
    void 질문_도중에_호출하면_NEEDS_EXPERT_이고_다른_설비로는_호출할_수_없다() throws Exception {
        long id = idOf(start(equipmentId, "소음이 커요"));

        callFrom(id, otherEquipmentId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("상담한 설비와 호출할 설비가 다릅니다."));
        callFrom(id, equipmentId)
                .andExpect(jsonPath("$.data.callReason").value("NEEDS_EXPERT"));
    }

    @Test
    void 해결됐어요_후_상담_내역의_탭별_개수와_필터() throws Exception {
        long resolved = idOf(start(equipmentId, "문이 안 열려요"));
        idOf(start(equipmentId, "소음이 커요"));
        idOf(start(noManualEquipmentId, "건조가 안 돼요"));

        mockMvc.perform(auth(post("/api/consultations/" + resolved + "/resolve"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        mockMvc.perform(auth(get("/api/consultations"), workerToken))
                .andExpect(jsonPath("$.data.counts.ALL").value(3))
                .andExpect(jsonPath("$.data.counts.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.data.counts.RESOLVED").value(1))
                .andExpect(jsonPath("$.data.content[0].symptom").value("건조가 안 돼요"));
        mockMvc.perform(auth(get("/api/consultations").param("status", "RESOLVED"), workerToken))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(resolved));
        mockMvc.perform(auth(get("/api/consultations").param("equipmentId", noManualEquipmentId.toString()), workerToken))
                .andExpect(jsonPath("$.data.counts.ALL").value(1))
                .andExpect(jsonPath("$.data.content[0].modelCode").value("DR-50"));
    }

    @Test
    void 다른_회사의_상담과_설비는_404_제작_업체는_403() throws Exception {
        long id = idOf(start(equipmentId, "문이 안 열려요"));

        mockMvc.perform(auth(get("/api/consultations/" + id), otherWorkerToken)).andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/consultations/" + id + "/messages"), otherWorkerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"네\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/consultations"), otherWorkerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":%d,\"symptom\":\"x\"}".formatted(equipmentId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/consultations"), makerToken)).andExpect(status().isForbidden());
        mockMvc.perform(auth(post("/api/consultations"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"equipmentId\":%d}".formatted(equipmentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("symptom"));
    }

    // ---------- helpers ----------

    private ResultActions start(Long equipmentId, String symptom) throws Exception {
        return mockMvc.perform(auth(post("/api/consultations"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":%d,\"symptom\":\"%s\"}".formatted(equipmentId, symptom)))
                .andExpect(status().isOk());
    }

    private ResultActions answer(long consultationId, String content) throws Exception {
        return mockMvc.perform(auth(post("/api/consultations/" + consultationId + "/messages"), workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"%s\"}".formatted(content)));
    }

    private ResultActions callFrom(long consultationId, Long equipmentId) throws Exception {
        return mockMvc.perform(auth(post("/api/requests"), workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"consultationId":%d,"equipmentId":%d,"symptomDetail":"상담에서 해결되지 않았습니다.",
                         "equipmentState":"STOPPED","urgency":"HIGH",
                         "contact":{"reporterName":"홍길동","reporterPhone":"010-2222-3333","companyPhone":"062-111-2222"}}
                        """.formatted(consultationId, equipmentId)));
    }

    private static long idOf(ResultActions result) throws Exception {
        return idOf(result.andReturn().getResponse().getContentAsString());
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.data.id")).longValue();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private Long equipment(Company company, EquipmentModel model, String name) {
        return equipmentRepository.save(Equipment.builder()
                .company(company).equipmentModel(model).name(name).location("1층").build()).getId();
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
