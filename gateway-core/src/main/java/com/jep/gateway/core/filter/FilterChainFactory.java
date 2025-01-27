package com.jep.gateway.core.filter;

import com.jep.gateway.core.context.GatewayContext;

/**
 * 过滤器链工厂 用于生成过滤器链
 *
 * @author enping.jep
 * @date 2025/1/27 21:51
 **/
public interface FilterChainFactory {

    /**
     * 构建过滤器链条
     */
    FilterChain buildFilterChain(GatewayContext ctx) throws Exception;

    /**
     * 通过过滤器ID获取过滤器
     */
    <T> T getFilterInfo(String filterId) throws Exception;
}
