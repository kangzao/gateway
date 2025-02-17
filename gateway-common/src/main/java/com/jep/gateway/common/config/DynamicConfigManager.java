package com.jep.gateway.common.config;

import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态服务缓存配置管理类
 *
 * @author enping.jep
 * @date 2025/1/27 21:19
 **/
public class DynamicConfigManager {

    /**
     * 服务定义信息集合    serviceId —> ServiceDefinition
     */
    @Getter
    private ConcurrentHashMap<String, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 服务实例集合       serviceId —> List<ServiceInstance>
     */
    private ConcurrentHashMap<String, List<ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

    /**
     * 规则集合         ruleId —> Rule
     */
    @Getter
    private ConcurrentHashMap<String, Rule> ruleMap = new ConcurrentHashMap<>();

    /**
     * 路径以及规则集合  serviceId.requestPath——>Rule
     */
    private ConcurrentHashMap<String, Rule> pathRuleMap = new ConcurrentHashMap<>();

    /**
     * 路径集合        service——>List<Rule>
     */
    private ConcurrentHashMap<String, List<Rule>> serviceRuleMap = new ConcurrentHashMap<>();

    public DynamicConfigManager() {
    }

    private static class SingletonHolder {
        private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }

    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /******* 对服务定义缓存的相关方法 ********/
    public void putServiceDefinition(String uniqueId, ServiceDefinition definition) {
        serviceDefinitionMap.put(uniqueId, definition);
    }

    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }

    public ServiceDefinition getServiceDefinition(String uniqueId) {
        return serviceDefinitionMap.get(uniqueId);
    }

    /******* 对服务实例缓存的相关方法 ********/
    public void putServiceInstance(String uniqueId, ServiceDefinition serviceDefinition) {
        serviceDefinitionMap.put(uniqueId, serviceDefinition);
        ;
    }

    /**
     * 将服务实例集合与唯一标识符关联并存储
     * <p>
     * 此方法用于将一个包含多个服务实例的集合与一个唯一的标识符关联起来
     * 它通过将这些信息存入serviceInstanceMap来实现，使得后续可以根据唯一标识符检索服务实例集合
     *
     * @param uniqueId         唯一标识符，用于标识服务实例集合
     * @param serviceInstances 服务实例集合，包含一组相关的服务实例
     */
    public void addServiceInstance(String uniqueId, List<ServiceInstance> serviceInstances) {
        serviceInstanceMap.put(uniqueId, serviceInstances);
    }

    /**
     * 根据服务ID删除服务实例
     */
    public void removeServiceInstanceByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }

    /******* 缓存规则相关操作方法 ********/
    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }

    public void putAllRule(List<Rule> ruleList) {
        ConcurrentHashMap<String, Rule> newRuleMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Rule> newPathMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<Rule>> newServiceMap = new ConcurrentHashMap<>();
        for (Rule rule : ruleList) {
            newRuleMap.put(rule.getId(), rule);
            List<Rule> rules = newServiceMap.get(rule.getServiceId());
            if (rules == null) {
                rules = new ArrayList<>();
            }
            rules.add(rule);
            newServiceMap.put(rule.getServiceId(), rules);

            List<String> paths = rule.getPaths();
            for (String path : paths) {
                String key = rule.getServiceId() + "." + path;
                newPathMap.put(key, rule);
            }
        }
        ruleMap = newRuleMap;
        pathRuleMap = newPathMap;
        serviceRuleMap = newServiceMap;
    }

    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
    }

    public Rule getRulePath(String path) {
        return pathRuleMap.get(path);
    }

    public List<Rule> getRuleByServiceId(String serviceId) {
        return serviceRuleMap.get(serviceId);
    }


    /***************** 	对服务实例缓存进行操作的系列方法 	***************/

    public List<ServiceInstance> getServiceInstanceByServiceId(String serviceId, boolean gray) {
        List<ServiceInstance> serviceInstances = serviceInstanceMap.get(serviceId);
        if (CollectionUtils.isEmpty(serviceInstances)) {
            return serviceInstances;
        }

        if (gray) {
            return serviceInstances.stream()
                    .filter(ServiceInstance::isGray)
                    .collect(Collectors.toList());
        }

        return serviceInstances;
    }
}
