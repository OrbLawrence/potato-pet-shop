package com.petshop.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DifyChatService {

    private static final Logger log = LoggerFactory.getLogger(DifyChatService.class);

    @Value("${dify.chatflow.base-url:http://localhost/v1}")
    private String baseUrl;

    @Value("${dify.chatflow.api-key:}")
    private String apiKey;

    @Value("${dify.chatflow.timeout-seconds:120}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter streamChat(String query, String user, String conversationId) {
        log.info("[AI] 收到流式对话请求: user={}, conversationId={}, query={}", user, conversationId, query);
        SseEmitter emitter = new SseEmitter((long) timeoutSeconds * 1000L);
        executor.execute(() -> doStream(emitter, query, user, conversationId));
        return emitter;
    }

    private void doStream(SseEmitter emitter, String query, String user, String conversationId) {
        try {
            RestTemplate restTemplate = buildRestTemplate();
            String url = baseUrl.replaceAll("/+$", "") + "/chat-messages";
            log.info("[AI] 调用 Dify 流式接口: url={}, user={}", url, user);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("inputs", Collections.emptyMap());
            payload.put("query", query);
            payload.put("response_mode", "streaming");
            payload.put("conversation_id", conversationId == null ? "" : conversationId);
            payload.put("user", user);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

            byte[] body = objectMapper.writeValueAsBytes(payload);

            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                        request.getBody().write(body);
                    },
                    response -> {
                        StringBuilder pendingAnswer = new StringBuilder();
                        boolean[] inThinking = {false};
                        String currentConversationId = conversationId == null ? "" : conversationId;
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring(5).trim();
                                if (data.isEmpty() || "[DONE]".equals(data)) {
                                    continue;
                                }
                                Map<String, Object> event = objectMapper.readValue(
                                        data,
                                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                        });
                                Object eventConversationId = event.get("conversation_id");
                                if (eventConversationId != null && !String.valueOf(eventConversationId).isEmpty()) {
                                    currentConversationId = String.valueOf(eventConversationId);
                                }
                                Object answer = event.get("answer");
                                if (answer != null) {
                                    String cleanAnswer = filterThinking(
                                            pendingAnswer,
                                            inThinking,
                                            String.valueOf(answer));
                                    if (cleanAnswer != null && !cleanAnswer.isEmpty()) {
                                        Map<String, Object> messageEvent = new LinkedHashMap<>();
                                        messageEvent.put("type", "message");
                                        messageEvent.put("content", cleanAnswer);
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data(objectMapper.writeValueAsString(messageEvent),
                                                        MediaType.APPLICATION_JSON));
                                    }
                                }
                                if ("message_end".equals(event.get("event"))) {
                                    if (currentConversationId != null && !currentConversationId.isEmpty()) {
                                        Map<String, Object> conversationEvent = new LinkedHashMap<>();
                                        conversationEvent.put("type", "conversation");
                                        conversationEvent.put("conversationId", currentConversationId);
                                        emitter.send(SseEmitter.event()
                                                .name("conversation")
                                                .data(objectMapper.writeValueAsString(conversationEvent),
                                                        MediaType.APPLICATION_JSON));
                                    }
                                    Map<String, Object> doneEvent = new LinkedHashMap<>();
                                    doneEvent.put("type", "done");
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data(objectMapper.writeValueAsString(doneEvent),
                                                    MediaType.APPLICATION_JSON));
                                }
                            }
                        }
                        emitter.complete();
                        log.info("[AI] Dify 流式对话完成: user={}, conversationId={}", user, currentConversationId);
                        return null;
                    });
        } catch (Exception e) {
            log.error("[AI] Dify 流式转发失败: user={}, error={}", user, e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    public Map<String, Object> blockChat(String query, String user, String conversationId) {
        long start = System.currentTimeMillis();
        log.info("[AI] 收到阻塞对话请求: user={}, conversationId={}, query={}", user, conversationId, query);
        String url = baseUrl.replaceAll("/+$", "") + "/chat-messages";
        log.info("[AI] 调用 Dify 阻塞接口: url={}, user={}", url, user);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("inputs", Collections.emptyMap());
        payload.put("query", query);
        payload.put("response_mode", "blocking");
        payload.put("conversation_id", conversationId == null ? "" : conversationId);
        payload.put("user", user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        RestTemplate restTemplate = buildRestTemplate();
        Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(payload, headers), Map.class);

        String answer = "";
        String cid = conversationId == null ? "" : conversationId;
        if (response != null) {
            Object answerObj = response.get("answer");
            if (answerObj != null) {
                answer = String.valueOf(answerObj);
            }
            Object cidObj = response.get("conversation_id");
            if (cidObj != null && !String.valueOf(cidObj).isEmpty()) {
                cid = String.valueOf(cidObj);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", filterThinking(new StringBuilder(), new boolean[]{false}, answer));
        result.put("conversationId", cid);
        log.info("[AI] Dify 阻塞对话完成: user={}, elapsed={}ms, answerLength={}, conversationId={}",
                user, System.currentTimeMillis() - start, answer.length(), cid);
        return result;
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }

    private String filterThinking(StringBuilder pending, boolean[] inThinking, String chunk) {
        pending.append(chunk);
        StringBuilder output = new StringBuilder();

        while (pending.length() > 0) {
            if (!inThinking[0]) {
                int start = pending.indexOf("<think");
                if (start < 0) {
                    output.append(pending);
                    pending.setLength(0);
                    break;
                }
                if (start > 0) {
                    output.append(pending.substring(0, start));
                    pending.delete(0, start);
                }
                int tagEnd = pending.indexOf(">");
                if (tagEnd < 0) {
                    break;
                }
                pending.delete(0, tagEnd + 1);
                inThinking[0] = true;
            } else {
                int end = pending.indexOf("</think>");
                if (end < 0) {
                    break;
                }
                pending.delete(0, end + 8);
                inThinking[0] = false;
            }
        }

        return output.toString();
    }
}
