package com.jep.gateway.core.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import com.jep.gateway.common.config.Rule;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.constant.FilterConst;
import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.NotFoundException;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.Filter;
import com.jep.gateway.core.filter.annotation.FilterAspect;
import com.jep.gateway.core.request.GatewayRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;


import static com.jep.gateway.common.constant.FilterConst.*;

/**
 * 负载均衡
 * @author enping.jep
 * @date 2025/1/31 13:11
 **/
@Slf4j
@FilterAspect(id = LOAD_BALANCE_FILTER_ID,
        name = LOAD_BALANCE_FILTER_NAME,
        order = LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 验证输入
        if (ctx == null || ctx.getUniqueId() == null || ctx.getRequest() == null) {
            log.error("Context or context's unique ID or request is null");
            throw new IllegalArgumentException("Context and its unique ID, request must not be null");
        }

        // 获取上下文服务ID
        String serviceId = ctx.getUniqueId();

        // 加载负载均衡策略，增加null检查
        LoadBalanceRule gatewayRule = getLoadBalanceRule(ctx);
        if (gatewayRule == null) {
            log.error("Load balance rule is null for service ID: {}", serviceId);
            throw new IllegalStateException("Load balance rule must not be null");
        }

        // 选取服务实例，重新构造 Request 请求头
        ServiceInstance instance = gatewayRule.choose(serviceId, ctx.isGray());

        // 日志记录优化
        if (instance != null ) {
            log.info("ServiceInstance ip:{}, port:{}", instance.getIp(), instance.getPort());
        } else {
            log.error("No instance available for service ID: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }

        GatewayRequest gatewayRequest = ctx.getRequest();
        if (gatewayRequest != null) {
            String modifyHost = instance.getIp() + ":" + instance.getPort();
            gatewayRequest.setModifyHost(modifyHost);
        }
    }

    /**
     * 获取负载均衡策略
     */
    public LoadBalanceRule getLoadBalanceRule(GatewayContext context) {
        LoadBalanceRule balanceRule = null;
        Rule rule = context.getRule();

        if (rule != null) {
            Set<Rule.FilterConfig> configFilters = rule.getFilterConfigs();
            for (Rule.FilterConfig filterConfig : configFilters) {
                if (filterConfig == null) {
                    continue;
                }
                String filterId = filterConfig.getId();

                // 解析Rule配置的过滤器属性，获取过滤器类型描述
                if (filterId.equals(FilterConst.LOAD_BALANCE_FILTER_ID)) {
                    balanceRule = parseLoadBalanceConfig(filterConfig.getConfig(), rule.getServiceId());
                    // 找到负载均衡配置后即退出循环
                    break;
                }
            }
        }
        return balanceRule;
    }

    /**
     * 解析负载均衡配置
     */
    private LoadBalanceRule parseLoadBalanceConfig(String config, String serviceId) {
        String strategy = FilterConst.LOAD_BALANCE_STRATEGY_RANDOM;
        if (StringUtils.isNotEmpty(config)) {
            Map<String, String> map = JSON.parseObject(config, Map.class);
            strategy = map.getOrDefault(FilterConst.LOAD_BALANCE_KEY, strategy);
        }
        return getLoadBalanceRuleByStrategy(strategy, serviceId);
    }

    /**
     * 根据策略获取负载均衡规则
     */
    private LoadBalanceRule getLoadBalanceRuleByStrategy(String strategy, String serviceId) {
        return switch (strategy) {
            case FilterConst.LOAD_BALANCE_STRATEGY_RANDOM -> RandomLoadBalanceRule.getInstance(serviceId);
            case FilterConst.LOAD_BALANCE_STRATEGY_ROUND_ROBIN -> RoundRobinLoadBalanceRule.getInstance(serviceId);
            case FilterConst.LOAD_BALANCE_STRATEGY_WEIGHT_RANDOM ->
                    WeightedRoundRobinLoadBalanceRule.getInstance(serviceId);
            default -> {
                log.warn("No load balance rule can be loaded for service={}, using default strategy: {}", serviceId, strategy);
                yield RandomLoadBalanceRule.getInstance(serviceId);
            }
        };
    }
}
