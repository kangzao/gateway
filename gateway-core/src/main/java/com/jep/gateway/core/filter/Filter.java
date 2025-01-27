package com.jep.gateway.core.filter;

import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.annotation.FilterAspect;

/**
 * 过滤器顶层接口，具体子类实现过滤器功能
 * @author enping.jep
 * @date 2025/1/27 21:52
 **/
public interface Filter {

    /**
     * 执行过滤器
     */
    void doFilter(GatewayContext ctx) throws Exception;

    /**
     * 获取过滤器执行顺序
     * default 关键字用于为接口中的抽象方法提供一个默认的实现 jdk8引入
     * 这意味着实现该接口的类可以选择不提供自己的实现，而是直接使用接口中定义的默认实现。
     */
    default int getOrder() {
        FilterAspect annotation = this.getClass().getAnnotation(FilterAspect.class);
        if (annotation != null) {
            return annotation.order();
        }
        return Integer.MAX_VALUE;
    }
}
