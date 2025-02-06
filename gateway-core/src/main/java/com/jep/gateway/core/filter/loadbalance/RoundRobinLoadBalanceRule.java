package com.jep.gateway.core.filter.loadbalance;

import com.jep.gateway.common.config.DynamicConfigManager;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.exception.NotFoundException;
import com.jep.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jep.gateway.common.enums.ResponseCode.SERVICE_INSTANCE_NOT_FOUND;

/**
 * @author enping.jep
 * @date 2025/1/31 21:30
 **/
@Slf4j
public class RoundRobinLoadBalanceRule implements LoadBalanceRule {
    private AtomicInteger position = new AtomicInteger(1);

    private final String serviceId;


    public RoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String, RoundRobinLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RoundRobinLoadBalanceRule getInstance(String serviceId) {
        RoundRobinLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RoundRobinLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        return choose(ctx.getUniqueId(), ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        Set<ServiceInstance> serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
        if (serviceInstanceSet.isEmpty()) {
            log.warn("No instance available for:{}", serviceId);
            throw new NotFoundException(SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstanceSet);
        if (instances.isEmpty()) {
            log.warn("No instance available for service:{}", serviceId);
            return null;
        } else {
            int pos = Math.abs(this.position.incrementAndGet());
            return instances.get(pos % instances.size());
        }
    }
}
