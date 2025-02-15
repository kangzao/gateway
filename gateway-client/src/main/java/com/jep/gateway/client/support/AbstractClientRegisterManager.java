package com.jep.gateway.client.support;

import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.jep.gateway.register.RegisterCenter;

import java.util.ServiceLoader;

/**
 * 抽象客户端注册管理器，支持多种协议，方便后续扩展
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

    /**
     * 注册服务定义和服务实例到注册中心
     *
     * @param serviceDefinition 服务定义，包含服务的相关信息和配置
     * @param serviceInstance 服务实例，表示具体的服务实例化对象
     * 此方法通过调用注册中心的register方法来完成服务的注册过程
     * 它将服务定义和服务实例作为参数传递给注册中心，以便在注册中心进行服务的记录和管理
     */
    protected void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        registerCenter.register(serviceDefinition, serviceInstance);
    }
}
