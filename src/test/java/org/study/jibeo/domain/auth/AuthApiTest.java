package org.study.jibeo.domain.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private JwtTokenProvider tokenProvider;

    // Redis 없이 테스트하기 위해 리프레시 토큰 저장소는 mock 처리
    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    void 토큰_없이_보호된_API를_호출하면_SDK_형식의_401() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 잘못된_토큰이면_401() throws Exception {
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 유효한_토큰이면_인증을_통과한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, UserRole.WORKER, 1L);

        // 인증은 통과하고, 없는 경로라 404
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void 회원가입_후_로그인하면_토큰을_받는다() throws Exception {
        Long companyId = companyRepository.save(Company.builder()
                .name("테스트 회사")
                .located("광주")
                .businessNumber("123-45-67890")
                .type(CompanyType.USER_COMPANY)
                .build()).getId();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"worker1","pw":"password123","role":"WORKER","companyId":%d}
                                """.formatted(companyId)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.loginId").value("worker1"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"worker1","pw":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void 유효성_검사에_실패하면_필드별_오류를_준다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값이 유효하지 않습니다."))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void Swagger_문서에_JWT_인증_스키마가_있다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }
}