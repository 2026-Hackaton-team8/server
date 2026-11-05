package org.study.jibeo.domain;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import org.study.jibeo.global.ai.AiClient;
import org.study.jibeo.global.ai.AiDtos;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;
import team.themoment.sdk.exception.ExpectedException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AI 가 규칙을 어기거나 응답하지 못할 때 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsultationAiFailureTest {

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
    @MockitoBean
    private AiClient aiClient;

    private String workerToken;
    private Long equipmentId;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        User worker = userRepository.save(User.builder().loginId("worker1").pw("{noop}x").role(UserRole.WORKER)
                .company(userCompany).name("홍길동").phone("010-1234-5678").build());
        workerToken = tokenProvider.createAccessToken(worker.getId(), worker.getRole(), worker.getCompanyId());
        EquipmentModel model = modelRepository.save(EquipmentModel.builder()
                .code("WM-20").name("세탁기").category("세탁기").makerCompany(maker).build());
        equipmentId = equipmentRepository.save(Equipment.builder()
                .company(userCompany).equipmentModel(model).name("1호기").location("1층").build()).getId();
    }

    @Test
    void 질문_5번을_넘기면_AI가_또_물어도_확신_부족으로_호출을_권한다() throws Exception {
        given(aiClient.next(any())).willReturn(new AiDtos.NextResponse(AiDtos.TurnType.QUESTION, "또 질문",
                List.of(), List.of(), null, null, null, null, null, null, null, null));

        String json = mockMvc.perform(auth(post("/api/consultations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":%d,\"symptom\":\"소음\"}".formatted(equipmentId)))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.data.id")).longValue();
        for (int i = 0; i < 4; i++) {
            answer(id).andExpect(jsonPath("$.data.questionCount").value(i + 2));
        }
        answer(id)
                .andExpect(jsonPath("$.data.questionCount").value(5))
                .andExpect(jsonPath("$.data.messages[-1].type").value("ESCALATE"))
                .andExpect(jsonPath("$.data.messages[-1].payload.reason").value("LOW_CONFIDENCE"))
                .andExpect(jsonPath("$.data.canAnswer").value(false));
    }

    @Test
    void AI가_응답하지_못하면_502() throws Exception {
        given(aiClient.next(any())).willThrow(
                new ExpectedException("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY));

        mockMvc.perform(auth(post("/api/consultations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":%d,\"symptom\":\"소음\"}".formatted(equipmentId)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    private org.springframework.test.web.servlet.ResultActions answer(long id) throws Exception {
        return mockMvc.perform(auth(post("/api/consultations/" + id + "/messages"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"네\"}"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + workerToken);
    }

    private Company company(String name, String businessNumber, CompanyType type) {
        return companyRepository.save(Company.builder()
                .name(name).located("광주").businessNumber(businessNumber).type(type).phone("062-123-4567")
                .build());
    }
}
