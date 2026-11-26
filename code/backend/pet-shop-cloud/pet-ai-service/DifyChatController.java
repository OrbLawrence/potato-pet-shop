package com.petshop.ai.controller;

import com.petshop.ai.service.DifyChatService;
import com.petshop.common.api.Result;
import com.petshop.common.security.BearerTokenResolver;
import com.petshop.common.security.TokenUser;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/ai")
public class DifyChatController {

    private static final Logger log = LoggerFactory.getLogger(DifyChatController.class);

    @Autowired
    private DifyChatService difyChatService;

    @Autowired
    private BearerTokenResolver bearerTokenResolver;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestHeader(value = "Authorization", required = false) String authHeader,
                           @RequestBody ChatRequest request) {
        TokenUser user = resolveUser(authHeader);
        validateQuery(request);
        return difyChatService.streamChat(request.getQuery().trim(), user.getUsername(), request.getConversationId());
    }

    @PostMapping("/chat/blocking")
    public Result<Map<String, Object>> chatBlocking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChatRequest request) {
        TokenUser user = resolveUser(authHeader);
        validateQuery(request);
        try {
            return Result.success(difyChatService.blockChat(
                    request.getQuery().trim(), user.getUsername(), request.getConversationId()));
        } catch (Exception e) {
            log.error("AI 问答失败", e);
            return Result.error(502, "AI 服务暂时不可用，请确认 Dify 已启动");
        }
    }

    private TokenUser resolveUser(String authHeader) {
        TokenUser user = bearerTokenResolver.resolve(authHeader);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (!"user".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI 宠物专家仅对用户开放");
        }
        return user;
    }

    private void validateQuery(ChatRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "问题不能为空");
        }
    }

    @Data
    public static class ChatRequest {
        private String query;
        private String conversationId;
    }
}
