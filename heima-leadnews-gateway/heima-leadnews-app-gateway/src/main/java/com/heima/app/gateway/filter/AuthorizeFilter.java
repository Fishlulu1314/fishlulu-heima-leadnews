package com.heima.app.gateway.filter;

import com.heima.app.gateway.util.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizeFilter implements Ordered, GlobalFilter {

    /**
     * 过滤器逻辑
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        //判断是否是登录请求,如果是登录请求，直接放行f
        String path = exchange.getRequest().getURI().getPath();
        if (path.contains("/login")) {
            return chain.filter(exchange);
        }
        //判断是否携带了token
        String token = exchange.getRequest().getHeaders().getFirst("token");//前端约定的token名称
        //如果token为空，直接返回验证失败
            if(StringUtils.isBlank(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        try {
            //判断token是否有效
            int result = AppJwtUtil.verifyToken(AppJwtUtil.getClaimsBody(token));
            //如果过期
            if(result == 1 || result == 2){
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
        } catch (Exception e) {//可能会解析异常
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //放行
        return chain.filter(exchange);
    }

    /**
     * 过滤器执行顺序，返回值越小，执行优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
