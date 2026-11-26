package com.petshop.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLogGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLogGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String uri = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        String target = query == null ? uri : uri + "?" + query;
        long start = System.currentTimeMillis();

        log.info("[GATEWAY] 收到请求: {} {}", request.getMethodValue(), target);
        return chain.filter(exchange).doFinally(signal -> {
            HttpStatus status = exchange.getResponse().getStatusCode();
            log.info("[GATEWAY] 请求完成: {} {} -> status={} cost={}ms",
                    request.getMethodValue(), target, status == null ? 0 : status.value(),
                    System.currentTimeMillis() - start);
        });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
