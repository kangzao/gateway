package com.jep.gateway.core.filter.auth;

import com.jep.gateway.common.constant.FilterConst;
import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.ResponseException;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.Filter;
import com.jep.gateway.core.filter.annotation.FilterAspect;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author enping.jep
 * @date 2025/1/31 13:09
 **/
@Slf4j
@FilterAspect(id = FilterConst.AUTH_FILTER_ID, name = FilterConst.AUTH_FILTER_NAME, order = FilterConst.AUTH_FILTER_ORDER)
public class AuthFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 从请求中获取token，如果token不存在，那么就抛出一个未授权的异常
        Cookie cookie = ctx.getRequest().getCookie(FilterConst.COOKIE_KEY);
        String token = cookie.value();
        if (StringUtils.isBlank(token)) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }
        //解析用户id
        try {
            long userId = parseUserIdFromToken(token);
            ctx.getRequest().setUserId(userId);
        } catch (Exception e) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }

    }

    /**
     * 解析token中的payload —— 用户ID
     */
    private long parseUserIdFromToken(String token) {
        Jwt jwt = Jwts.parser().setSigningKey(FilterConst.TOKEN_SECRET).parse(token);
        return Long.parseLong(((DefaultClaims) jwt.getBody()).getSubject());
    }
}
