package com.jep.gateway.client.support.dubbo;

import com.jep.gateway.client.ApiAnnotationScanner;
import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.client.support.AbstractClientRegisterManager;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.util.NetUtil;
import com.jep.gateway.common.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.HashSet;
import java.util.Set;

import static com.jep.gateway.common.constant.BasicConst.COLON_SEPARATOR;
import static com.jep.gateway.common.constant.GatewayConst.DEFAULT_WEIGHT;

/**
 * @author enping.jep
 * @date 2025/1/28 11:40
 **/
@Slf4j
public class DubboClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent> {

    private Set<Object> set = new HashSet<>();

    public DubboClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    /**
     * 处理应用事件，主要用于Dubbo服务注册和应用启动事件处理
     *
     * @param applicationEvent 应用事件，可以是服务导出事件或应用启动事件
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // 具体来说，当一个服务提供者（Provider）通过 Dubbo 框架将服务发布到注册中心，并且服务可以被消费者（Consumer）调用时，就会触发这个事件。
        if (applicationEvent instanceof ServiceBeanExportedEvent) {
            try {
                // 获取服务 bean 对象，用于后续的服务注册操作 ServiceBean 是服务提供者的核心组件，它负责将服务接口的实现类封装为可以被 Dubbo 框架管理的提供者端点（endpoint）。
                // ServiceBean 通常与服务提供者一起被注册到 Dubbo 的注册中心，以便服务消费者可以发现并调用这些服务。
                ServiceBean serviceBean = ((ServiceBeanExportedEvent) applicationEvent).getServiceBean();
                // 执行Dubbo服务注册操作
                doRegisterDubbo(serviceBean);
            } catch (Exception e) {
                // 记录服务注册异常信息
                log.error("doRegisterDubbo error", e);
                // 将捕获的异常重新抛出，保证异常能够被外部捕获并处理
                throw new RuntimeException(e);
            }
        } else if (applicationEvent instanceof ApplicationStartedEvent) {
            // 记录应用启动成功信息
            log.info("dubbo api started");
        }
    }


    /**
     * 注册Dubbo服务
     * <p>
     * 本方法负责将服务实例注册到Dubbo服务框架中，包括检查服务是否已注册、扫描服务定义、
     * 创建服务实例并最终注册服务
     *
     * @param serviceBean 服务实例的配置信息，包含服务引用、协议端口等
     */
    private void doRegisterDubbo(ServiceBean serviceBean) {
        // 获取服务引用
        Object bean = serviceBean.getRef();

        // 如果服务已经注册，则直接返回
        if (set.contains(bean)) {
            return;
        }

        // 扫描服务注解，获取服务定义
        ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scan(bean, serviceBean);

        // 如果服务定义为空，则直接返回
        if (serviceDefinition == null) {
            return;
        }

        // 设置服务定义的环境类型
        serviceDefinition.setEnvType(getApiProperties().getEnv());

        // 创建服务实例
        ServiceInstance serviceInstance = new ServiceInstance();
        // 获取本地IP地址
        String localIp = NetUtil.getLocalIp();
        // 获取服务端口
        int port = serviceBean.getProtocol().getPort();
        // 构造服务实例ID，格式为：IP:端口
        String serviceInstanceId = localIp + COLON_SEPARATOR + port;
        // 获取服务唯一ID
        String uniqueId = serviceDefinition.getUniqueId();
        // 获取服务版本
        String version = serviceDefinition.getVersion();

        // 设置服务实例属性
        serviceInstance.setServiceInstanceId(serviceInstanceId);
        serviceInstance.setUniqueId(uniqueId);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        // 设置注册时间
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        serviceInstance.setVersion(version);
        // 设置默认权重
        serviceInstance.setWeight(DEFAULT_WEIGHT);

        // 注册服务定义和服务实例
        register(serviceDefinition, serviceInstance);
    }
}
