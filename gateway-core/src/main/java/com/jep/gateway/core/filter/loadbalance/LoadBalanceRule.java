package com.jep.gateway.core.filter.loadbalance;

import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.core.context.GatewayContext;

/**
 * @author enping.jep
 * @date 2025/1/31 13:14
 **/
public interface LoadBalanceRule {

    /**
     * 通过上下文参数获取服务实例
     *
     * @param ctx
     * @return
     */
    ServiceInstance choose(GatewayContext ctx);

    /**
     * 通过服务ID拿到对应的服务实例
     *
     * @param serviceId
     * @param gray
     * @return
     */
    ServiceInstance choose(String serviceId, boolean gray);
}
