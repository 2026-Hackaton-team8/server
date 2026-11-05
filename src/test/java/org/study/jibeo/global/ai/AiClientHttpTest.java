package org.study.jibeo.global.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.study.jibeo.domain.auth.RefreshTokenStore;
import org.study.jibeo.domain.request.model.CallReason;
import team.themoment.sdk.exception.ExpectedException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ai.base-url 이 있으면 실제 HTTP 로 Python 을 부른다. 가짜 서버로 요청 형식·응답 해석·오류 처리를 확인한다.
 */
@SpringBootTest
class AiClientHttpTest {

    private static final HttpServer SERVER = start();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();
    private static final AtomicReference<String> reply = new AtomicReference<>();
    private static final AtomicReference<Integer> replyStatus = new AtomicReference<>(200);

    @Autowired
    private AiClient aiClient;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @DynamicPropertySource
    static void aiUrl(DynamicPropertyRegistry registry) {
        registry.add("ai.base-url", () -> "http://localhost:" + SERVER.getAddress().getPort());
    }

    @BeforeEach
    void reset() {
        replyStatus.set(200);
    }

    @AfterAll
    static void stop() {
        SERVER.stop(0);
    }

    @Test
    void 상담_요청을_문서_형식으로_보내고_응답을_읽는다() {
        reply.set("""
                {"type":"ESCALATE","reason":"LOW_CONFIDENCE","cause":"원인 미확인","suspectedPartCode":null,
                 "summary":{"symptom":"소음","cause":"미확인","triedAction":"필터 확인"},"extraField":"무시"}
                """);

        AiDtos.NextResponse res = aiClient.next(new AiDtos.NextRequest(7L,
                new AiDtos.EquipmentInfo(1L, "1호기", "1층"), new AiDtos.ModelInfo(3L, "WM-20", "세탁기", "세탁기"),
                "소음", 0, 5, List.of(new AiDtos.Message("USER", "SYMPTOM", "소음"))));

        assertThat(lastPath.get()).isEqualTo("/ai/consultations/next");
        assertThat(lastBody.get()).contains("\"consultationId\":7", "\"maxQuestions\":5", "\"sender\":\"USER\"",
                "\"code\":\"WM-20\"");
        assertThat(res.type()).isEqualTo(AiDtos.TurnType.ESCALATE);
        assertThat(res.reason()).isEqualTo(CallReason.LOW_CONFIDENCE);
        assertThat(res.summary().triedAction()).isEqualTo("필터 확인");
    }

    @Test
    void 초안_요청의_수리일은_날짜_문자열로_보낸다() {
        reply.set("""
                {"title":"t","category":"c","symptoms":["s"],"steps":["1"],"fallback":"f","evidenceRepairHistoryIds":[41]}
                """);

        AiDtos.DraftResponse res = aiClient.draftSolution(new AiDtos.DraftRequest(
                new AiDtos.ModelInfo(3L, "WM-20", "세탁기", "세탁기"),
                List.of(new AiDtos.History(41L, new AiDtos.PartInfo("P-07", "배수 필터"), "증상", "원인", "해결",
                        "SELF", LocalDate.of(2026, 10, 28)))));

        assertThat(lastPath.get()).isEqualTo("/ai/solutions/draft");
        assertThat(lastBody.get()).contains("\"repairedAt\":\"2026-10-28\"", "\"difficulty\":\"SELF\"");
        assertThat(res.evidenceRepairHistoryIds()).containsExactly(41L);
    }

    @Test
    void Python이_오류를_주면_502() {
        replyStatus.set(500);
        reply.set("{\"error\":{\"code\":\"MODEL_TIMEOUT\",\"message\":\"timeout\"}}");

        assertThatThrownBy(() -> aiClient.draftSolution(new AiDtos.DraftRequest(
                new AiDtos.ModelInfo(3L, "WM-20", "세탁기", "세탁기"), List.of())))
                .isInstanceOf(ExpectedException.class)
                .hasMessage("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/ai/", exchange -> {
                lastPath.set(exchange.getRequestURI().getPath());
                lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] out = reply.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(replyStatus.get(), out.length);
                exchange.getResponseBody().write(out);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
