package com.jep.gateway.core.filter.loadbalance;

import com.jep.gateway.common.config.DynamicConfigManager;
import com.jep.gateway.common.config.Rule;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.ResponseException;
import com.jep.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询的负载均衡过滤器
 *
 * @author enping.jep
 * @date 2025/1/31 21:32
 **/
@Slf4j
public class WeightedRoundRobinLoadBalanceRule implements LoadBalanceRule {
    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 存储服务ID和对应的负载均衡规则
     */
    private static ConcurrentHashMap<String, WeightedRoundRobinLoadBalanceRule> loadBalanceMap = new ConcurrentHashMap<>();

    /**
     * 用于记录当前轮询的位置
     */
    private AtomicInteger position = new AtomicInteger(0);

    /**
     * 构造函数，初始化服务ID
     */
    public WeightedRoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * 根据服务ID获取对应的负载均衡规则，如果不存在则创建一个新的
     */
    public static WeightedRoundRobinLoadBalanceRule getInstance(String serviceId) {
        WeightedRoundRobinLoadBalanceRule rule = loadBalanceMap.get(serviceId);
        if (rule == null) {
            rule = new WeightedRoundRobinLoadBalanceRule(serviceId);
            loadBalanceMap.put(serviceId, rule);
        }
        return rule;
    }

    /**
     * 根据上下文和是否灰度发布选择服务实例
     */
    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        Rule rule = ctx.getRule();
        return choose(rule.getServiceId(), ctx.isGray());
    }

    /**
     * 根据服务ID和是否灰度发布选择服务实例
     */
    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        // 获取服务实例集合
        List<ServiceInstance> serviceSets = DynamicConfigManager.getInstance().getServiceInstanceByServiceId(serviceId, gray);
        // 如果服务实例集合为空，则抛出异常
        if (CollectionUtils.isEmpty(serviceSets)) {
            log.warn("serviceId {} don't match any serviceInstance", serviceId);
            throw new ResponseException(ResponseCode.SERVICE_INVOKER_NOT_FOUND);
        }
        // 将服务实例集合转换为列表
        List<ServiceInstance> serviceLists = new ArrayList<>(serviceSets);

        // 计算总权重
        int totalWeight = serviceLists.stream().mapToInt(ServiceInstance::getWeight).sum();
        int currentWeight = 0;

        // 计算当前位置
        int index = position.getAndIncrement() % totalWeight;

        // 遍历服务实例列表，根据权重和当前位置选择服务实例
        for (ServiceInstance instance : serviceLists) {
            currentWeight += instance.getWeight();
            if (currentWeight > index) {
                return instance;
            }
        }

        // 如果没有找到合适的服务实例，则返回列表中的第一个
        return serviceLists.get(0);
    }
}
