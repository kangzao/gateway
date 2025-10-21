package com.jep.gateway.core.filter.loadbalance;

import com.jep.gateway.common.config.DynamicConfigManager;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import com.jep.gateway.common.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.jep.gateway.common.enums.ResponseCode.SERVICE_INSTANCE_NOT_FOUND;

/**
 * @author enping.jep
 * @date 2025/1/31 21:24
 **/
@Slf4j
public class RandomLoadBalanceRule implements LoadBalanceRule {
    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    public RandomLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * 服务ID——随机负载均衡策略
     */
    private static ConcurrentHashMap<String, RandomLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    /**
     * 根据服务 ID 获取负载均衡策略
     */
    public static RandomLoadBalanceRule getInstance(String serviceId) {
        RandomLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RandomLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }


    /**
     * 负载均衡策略
     */
    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        String serviceId = ctx.getUniqueId();
        return choose(serviceId, ctx.isGray());
    }

    /**
     * 根据服务ID获取服务实例
     */
    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        List<ServiceInstance> serviceInstances = DynamicConfigManager.getInstance().getServiceInstanceByServiceId(serviceId, gray);
        if (serviceInstances.isEmpty()) {
            log.warn("No instance available for:{}", serviceId);
            throw new NotFoundException(SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstances);
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        ServiceInstance instance = (ServiceInstance) instances.get(index);
        return instance;
    }
}
