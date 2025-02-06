package com.jep.gateway.core.filter.chain;

import com.jep.gateway.core.context.ContextStatus;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.Filter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤器链 用于存储实现的过滤器的信息 并且按照顺序进行执行
 * @author enping.jep
 * @date 2025/1/27 21:51
 **/
@Slf4j
public class FilterChain {

    private List<Filter> filters = new ArrayList<>();


    public FilterChain addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }


    public void addFilterList(List<Filter> filter) {
        filters.addAll(filter);
    }


    /**
     * 执行过滤器链
     */
    public GatewayContext doFilter(GatewayContext ctx) throws Exception {
        log.info("执行过滤器链,请求id: {}", ctx.getRequest().getId());
        if (filters.isEmpty()) {
            return ctx;
        }
        try {
            for (Filter fl : filters) {
                fl.doFilter(ctx);
                if (ctx.getContextStatus() == ContextStatus.Terminated) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("执行过滤器发生异常,异常信息：{}", e.getMessage());
            throw e;
        }
        return ctx;
    }
}
