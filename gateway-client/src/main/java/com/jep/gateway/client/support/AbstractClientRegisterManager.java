package com.jep.gateway.client.support;

import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.jep.gateway.register.RegisterCenter;

import java.util.ServiceLoader;

/**
 * @author enping.jep
 * @date 2025/1/28 11:40
 **/
@Slf4j
public abstract class AbstractClientRegisterManager {
    @Getter
    private ApiProperties apiProperties;

    private RegisterCenter registerCenter;

    protected AbstractClientRegisterManager(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;

        //利用SPI机制   初始化注册中心对象  这里可以加载多个实现类 返回一个Iterator
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("not found RegisterCenter impl");
            return new RuntimeException("not found RegisterCenter impl");
        });
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());
    }

    protected void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        registerCenter.register(serviceDefinition, serviceInstance);
    }
}
