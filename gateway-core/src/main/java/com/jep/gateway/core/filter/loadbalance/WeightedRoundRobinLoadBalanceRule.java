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
 * 加权轮询负载均衡规则实现类
 * 基于权重的轮询算法，权重越高的服务实例被选中的概率越大
 *
 * @author enping.jep
 * @date 2025/1/31 21:32
 **/
@Slf4j
public class WeightedRoundRobinLoadBalanceRule implements LoadBalanceRule {
    /**
     * 服务ID，标识该负载均衡规则对应的服务
     */
    private String serviceId;

    /**
     * 存储服务ID和对应的负载均衡规则实例的映射关系
     * 使用ConcurrentHashMap保证线程安全
     */
    private static ConcurrentHashMap<String, WeightedRoundRobinLoadBalanceRule> loadBalanceMap = new ConcurrentHashMap<>();

    /**
     * 原子计数器，用于记录当前轮询的位置
     * 通过原子操作保证多线程环境下的安全性
     */
    private AtomicInteger position = new AtomicInteger(0);

    /**
     * 构造函数，初始化服务ID
     * 
     * @param serviceId 服务唯一标识符
     */
    public WeightedRoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * 获取指定服务ID的负载均衡规则实例（单例模式）
     * 如果实例不存在则创建新实例并存入缓存
     * 
     * @param serviceId 服务唯一标识符
     * @return 对应服务的加权轮询负载均衡规则实例
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
     * 根据网关上下文选择合适的服务实例
     * 
     * @param ctx 网关上下文对象，包含请求相关信息
     * @return 选中的服务实例
     */
    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        Rule rule = ctx.getRule();
        return choose(rule.getServiceId(), ctx.isGray());
    }

    /**
     * 根据服务ID和灰度标识选择合适的服务实例
     * 实现加权轮询算法的核心逻辑
     * 
     * @param serviceId 服务唯一标识符
     * @param gray 是否为灰度流量
     * @return 选中的服务实例
     * @throws ResponseException 当找不到可用服务实例时抛出异常
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

        // 计算所有服务实例的权重总和
        int totalWeight = serviceLists.stream().mapToInt(ServiceInstance::getWeight).sum();
        int currentWeight = 0;

        // 计算当前位置（通过原子递增确保线程安全，并对总权重取模避免越界）
        int index = position.getAndIncrement() % totalWeight;

        // 遍历服务实例列表，根据权重和当前位置选择服务实例
        // 权重越大的实例被选中的概率越高
        for (ServiceInstance instance : serviceLists) {
            currentWeight += instance.getWeight();
            // 当累计权重超过当前位置索引时，选中当前实例
            if (currentWeight > index) {
                return instance;
            }
        }

        // 正常情况下不会执行到这里，作为兜底策略返回列表中的第一个实例
        return serviceLists.get(0);
    }
}