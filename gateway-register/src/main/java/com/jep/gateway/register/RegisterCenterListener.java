package com.jep.gateway.register;

import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;

import java.util.List;
import java.util.Set;

/**
 * @author enping.jep
 * @date 2025/1/28 17:18
 **/
public interface RegisterCenterListener {
    void onChange(ServiceDefinition serviceDefinition, List<ServiceInstance> serviceInstances);
}
