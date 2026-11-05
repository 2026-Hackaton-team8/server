package org.study.jibeo.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import team.themoment.sdk.exception.ExpectedException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Python AI 서비스 호출. ai.base-url 이 비어 있으면 StubAi 의 임시 응답을 쓴다.
 */
@Slf4j
@Component
public class AiClient {

    private final AiProperties properties;
    private final StubAi stub;
    private final JsonMapper jsonMapper;
    private final RestClient consultationClient;
    private final RestClient draftClient;
    private final RestClient indexClient;

    public AiClient(AiProperties properties, StubAi stub, JsonMapper jsonMapper) {
        this.properties = properties;
        this.stub = stub;
        this.jsonMapper = jsonMapper;
        this.consultationClient = client(properties.consultationTimeout());
        this.draftClient = client(properties.draftTimeout());
        this.indexClient = client(Duration.ofSeconds(3));
        log.info("AI mode: {}", properties.enabled() ? "remote " + properties.baseUrl() : "stub");
    }

    public AiDtos.NextResponse next(AiDtos.NextRequest request) {
        if (!properties.enabled()) {
            return stub.next(request);
        }
        return post(consultationClient, "/ai/consultations/next", request, AiDtos.NextResponse.class);
    }

    public AiDtos.DraftResponse draftSolution(AiDtos.DraftRequest request) {
        if (!properties.enabled()) {
            return stub.draft(request);
        }
        return post(draftClient, "/ai/solutions/draft", request, AiDtos.DraftResponse.class);
    }

    /**
     * 인덱싱 알림. 커밋된 뒤에(Python 이 DB 에서 바로 읽을 수 있게) 별도 스레드로 보내고, 실패해도 무시한다.
     */
    public void notifyIndex(AiDtos.IndexKind kind, Long id) {
        if (!properties.enabled()) {
            return;
        }
        Runnable send = () -> Thread.ofVirtual().start(() -> {
            try {
                indexClient.post().uri("/ai/index").contentType(MediaType.APPLICATION_JSON)
                        .body(jsonMapper.writeValueAsString(new AiDtos.IndexRequest(kind, id)))
                        .retrieve().toBodilessEntity();
            } catch (RuntimeException e) {
                log.warn("AI index notify failed: kind={}, id={}, {}", kind, id, e.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private <T> T post(RestClient client, String path, Object body, Class<T> type) {
        try {
            String json = client.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(jsonMapper.writeValueAsString(body))
                    .retrieve().body(String.class);
            T result = json == null ? null : jsonMapper.readValue(json, type);
            if (result == null) {
                throw new IllegalStateException("empty body");
            }
            return result;
        } catch (RestClientException | JacksonException | IllegalStateException e) {
            log.error("AI call failed: {} {}", path, e.getMessage());
            throw new ExpectedException("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
        }
    }

    private RestClient client(Duration timeout) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(properties.enabled() ? properties.baseUrl() : "http://localhost")
                .requestFactory(factory).build();
    }
}
