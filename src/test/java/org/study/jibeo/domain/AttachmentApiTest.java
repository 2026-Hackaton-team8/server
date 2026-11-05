package org.study.jibeo.domain;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
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
import org.study.jibeo.global.storage.FileStorage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 첨부 파일: POST /api/files 로 올리고, 호출 제출 때 attachmentKeys 로 붙인다. S3 는 mock
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttachmentApiTest {

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
    private FileStorage fileStorage;

    private String workerToken;
    private String makerToken;
    private Long companyId;
    private Long otherCompanyId;
    private Long equipmentId;

    @BeforeEach
    void setUp() {
        Company maker = company("제작사", "100-00-00001", CompanyType.MAKER_COMPANY);
        Company userCompany = company("사용 업체 A", "200-00-00001", CompanyType.USER_COMPANY);
        companyId = userCompany.getId();
        otherCompanyId = company("사용 업체 B", "200-00-00002", CompanyType.USER_COMPANY).getId();
        workerToken = token(user("worker1", UserRole.WORKER, userCompany));
        makerToken = token(user("staff1", UserRole.STAFF, maker));

        EquipmentModel model = modelRepository.save(EquipmentModel.builder()
                .code("WM-20").name("산업용 세탁기").category("세탁기").makerCompany(maker).build());
        equipmentId = equipmentRepository.save(Equipment.builder()
                .company(userCompany).equipmentModel(model).name("1호기").location("1층").build()).getId();

    }

    @Test
    void 이미지를_올리면_회사별_경로의_키와_미리보기_URL을_준다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.JPG", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(auth(multipart("/api/files").file(file), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileKey").value(
                        matchesPattern("uploads/requests/" + companyId + "/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.jpg")))
                .andExpect(jsonPath("$.data.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.url").value(matchesPattern("http://localhost/api/files/view\\?key=uploads/requests/"
                        + companyId + "/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.jpg&exp=\\d+&sig=[A-Za-z0-9_-]{43}")));

        verify(fileStorage).upload(startsWith("uploads/requests/" + companyId + "/"), any(InputStream.class),
                eq(3L), eq("image/jpeg"));
    }

    @Test
    void 이미지_영상이_아니거나_빈_파일이면_400이고_저장하지_않는다() throws Exception {
        mockMvc.perform(auth(multipart("/api/files")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1})), workerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지(jpeg, png, webp, heic)나 영상(mp4, mov, webm)만 올릴 수 있습니다."));

        mockMvc.perform(auth(multipart("/api/files")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0])), workerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("빈 파일은 올릴 수 없습니다."));

        mockMvc.perform(auth(multipart("/api/files")
                        .file(new MockMultipartFile("other", "a.jpg", "image/jpeg", new byte[]{1})), workerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'file' 파일이 필요합니다."));

        verify(fileStorage, never()).upload(anyString(), any(), anyLong(), anyString());

        // 제작 업체는 아직 업로드 대상이 아니다
        mockMvc.perform(auth(multipart("/api/files")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1})), makerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 올린_파일을_호출에_붙이면_사용_업체와_제작_업체_상세에_보기_URL과_함께_나온다() throws Exception {
        String photo = "uploads/requests/" + companyId + "/2026/11/a.jpg";
        String video = "uploads/requests/" + companyId + "/2026/11/b.mp4";
        given(fileStorage.find(photo)).willReturn(Optional.of(new FileStorage.FileMeta("image/jpeg", 100)));
        given(fileStorage.find(video)).willReturn(Optional.of(new FileStorage.FileMeta("video/mp4", 2000)));

        String json = mockMvc.perform(auth(post("/api/requests"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("\"%s\",\"%s\",\"%s\"".formatted(photo, video, photo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long requestId = ((Number) JsonPath.read(json, "$.data.id")).longValue();

        mockMvc.perform(auth(get("/api/requests/" + requestId), workerToken))
                .andExpect(jsonPath("$.data.attachments[0].fileKey").value(photo))
                .andExpect(jsonPath("$.data.attachments[1].contentType").value("video/mp4"))
                .andExpect(jsonPath("$.data.attachments[1].size").value(2000));

        mockMvc.perform(auth(get("/api/maker/requests/" + requestId), makerToken))
                .andExpect(jsonPath("$.data.attachments[0].url").value(
                        matchesPattern("http://localhost/api/files/view\\?key=" + photo + "&exp=\\d+&sig=.+")));
    }

    @Test
    void 보기_주소는_로그인_없이_파일을_내려주고_캐시_시간을_붙인다() throws Exception {
        String photo = "uploads/requests/" + companyId + "/2026/11/a.jpg";
        given(fileStorage.open(photo, null)).willReturn(Optional.of(new FileStorage.FileStream(
                new ByteArrayInputStream(new byte[]{9, 8, 7}), "image/jpeg", 3L, null, "\"etag-1\"")));

        mockMvc.perform(get(viewUrl(photo)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{9, 8, 7}))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Length", "3"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("ETag", "\"etag-1\""))
                .andExpect(header().string("Cache-Control", matchesPattern("private, max-age=\\d+")));
    }

    @Test
    void 영상_구간_요청은_S3로_넘기고_206() throws Exception {
        String video = "uploads/requests/" + companyId + "/2026/11/b.mp4";
        given(fileStorage.open(video, "bytes=0-1")).willReturn(Optional.of(new FileStorage.FileStream(
                new ByteArrayInputStream(new byte[]{1, 2}), "video/mp4", 2L, "bytes 0-1/2000", null)));

        mockMvc.perform(get(viewUrl(video)).header("Range", "bytes=0-1"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-1/2000"))
                .andExpect(content().bytes(new byte[]{1, 2}));
    }

    @Test
    void 서명이_틀리거나_다른_파일이면_403_없는_파일이면_404() throws Exception {
        String photo = "uploads/requests/" + companyId + "/2026/11/a.jpg";
        String url = viewUrl(photo);
        String sig = url.substring(url.indexOf("&sig=") + 5);
        String exp = url.replaceAll(".*&exp=(\\d+)&.*", "$1");

        mockMvc.perform(get(url.replace("&sig=" + sig, "&sig=x" + sig.substring(1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("파일 주소가 만료되었거나 올바르지 않습니다. 화면을 새로 고쳐 주세요."));
        // 같은 서명으로 다른 회사 파일이나 더 늦은 만료 시각은 열 수 없다
        String others = "uploads/requests/" + otherCompanyId + "/2026/11/a.jpg";
        mockMvc.perform(get(url.replace(photo, others))).andExpect(status().isForbidden());
        mockMvc.perform(get(url.replace("&exp=" + exp, "&exp=" + (Long.parseLong(exp) + 3600))))
                .andExpect(status().isForbidden());
        verify(fileStorage, never()).open(anyString(), any());

        given(fileStorage.open(photo, null)).willReturn(Optional.empty());
        mockMvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void 다른_회사_경로나_없는_파일은_붙일_수_없고_호출도_만들어지지_않는다() throws Exception {
        String others = "uploads/requests/" + otherCompanyId + "/2026/11/a.jpg";
        String missing = "uploads/requests/" + companyId + "/2026/11/missing.jpg";
        given(fileStorage.find(missing)).willReturn(Optional.empty());

        for (String key : new String[]{others, missing, "uploads/requests/" + companyId + "/../" + otherCompanyId + "/a.jpg"}) {
            mockMvc.perform(auth(post("/api/requests"), workerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody("\"" + key + "\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("첨부 파일을 찾을 수 없습니다. 파일을 다시 올려 주세요."));
        }
        verify(fileStorage, never()).find(others);

        mockMvc.perform(auth(get("/api/requests"), workerToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 첨부는_5개까지() throws Exception {
        String keys = String.join(",", java.util.Collections.nCopies(6, "\"uploads/requests/" + companyId + "/x.jpg\""));

        mockMvc.perform(auth(post("/api/requests"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(keys)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("attachmentKeys"));
    }

    // ---------- helpers ----------

    /** 업로드 응답으로 받은 보기 주소 */
    private String viewUrl(String key) throws Exception {
        given(fileStorage.find(key)).willReturn(Optional.of(new FileStorage.FileMeta("image/jpeg", 3)));
        String json = mockMvc.perform(auth(post("/api/requests"), workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("\"" + key + "\"")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.attachments[0].url");
    }

    private String requestBody(String attachmentKeysJson) {
        return """
                {"equipmentId":%d,"symptomDetail":"탈수 단계에서 멈춥니다.","equipmentState":"STOPPED","urgency":"HIGH",
                 "contact":{"reporterName":"홍길동","reporterPhone":"010-2222-3333","companyPhone":"062-111-2222"},
                 "attachmentKeys":[%s]}
                """.formatted(equipmentId, attachmentKeysJson);
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B auth(B builder, String token) {
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
        return tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompanyId());
    }
}