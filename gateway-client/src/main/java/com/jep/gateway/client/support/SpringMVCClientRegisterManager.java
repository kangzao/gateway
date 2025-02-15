package com.jep.gateway.client.support;

import com.jep.gateway.client.ApiAnnotationScanner;
import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.util.NetUtil;
import com.jep.gateway.common.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jep.gateway.common.constant.BasicConst.COLON_SEPARATOR;
import static com.jep.gateway.common.constant.GatewayConst.DEFAULT_WEIGHT;

/**
 * SpringMVC 客户端注册管理器
 *
 * @author enping.jep
 * @date 2025/1/28 17:23
 **/
@Slf4j
public class SpringMVCClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    private ApplicationContext applicationContext;

    /**
     * 在Spring Boot中，ServerProperties 是一个配置类，它封装了与内嵌的Web服务器（如Tomcat、Jetty或Undertow）相关的配置属性。
     * 通过这个类，你可以访问和修改Web服务器的配置，例如端口号、上下文路径、SSL配置等。
     */
    @Autowired
    private ServerProperties serverProperties;

    private Set<Object> set = new HashSet<>();

    public SpringMVCClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 处理应用事件，用于在应用启动后注册Spring MVC相关配置
     *
     * @param applicationEvent 应用事件对象，用于标识应用的生命周期事件
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // 检查事件类型是否为应用启动事件  ApplicationStartedEvent 是 Spring 框架中的一个事件，
        // 它在 Spring 应用上下文（ApplicationContext）完全初始化并且已经启动之后发布
        if (applicationEvent instanceof ApplicationStartedEvent) {
            try {
                // 尝试执行Spring MVC的注册逻辑
                doRegisterSpringMvc();
            } catch (Exception e) {
                // 如果注册过程中发生异常，记录错误日志并抛出运行时异常
                log.error("doRegisterSpringMvc error", e);
                throw new RuntimeException(e);
            }

            // 注册完成后，记录Spring MVC API启动的信息日志
            log.info("springmvc api started");
        }
    }


    /**
     * 获取所有请求映射处理器。
     * 遍历每个处理器中的方法映射。
     * 获取方法所属的Bean实例。
     * 扫描带有@Api注解的Bean，生成服务定义。
     * 创建并配置服务实例。
     * 将服务实例注册到服务注册中心。
     */
    private void doRegisterSpringMvc() {
        //通过BeanFactoryUtils.beansOfTypeIncludingAncestors方法，从Spring应用上下文（applicationContext）中获取所有类型为RequestMappingHandlerMapping的Bean。
        //RequestMappingHandlerMapping是Spring MVC中用于处理请求映射的类，它负责将HTTP请求映射到具体的处理方法（即Controller中的方法）。
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RequestMappingHandlerMapping.class, true, false);

        // 遍历每个RequestMappingHandlerMapping，并通过getHandlerMethods()方法获取所有请求映射信息（RequestMappingInfo）和对应的处理方法（HandlerMethod）。
        for (RequestMappingHandlerMapping handlerMapping : allRequestMappings.values()) {
            //获取每个映射处理器中的方法映射
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

            //遍历方法映射
            for (Map.Entry<RequestMappingInfo, HandlerMethod> me : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = me.getValue();
                //获取方法所属的Bean类型
                Class<?> clazz = handlerMethod.getBeanType();

                //获取方法所属的Bean实例
                Object bean = applicationContext.getBean(clazz);

                //如果Bean实例已经在集合中，则跳过
                if (set.contains(bean)) {
                    continue;
                }

                //扫描带有@Api注解的Bean，获取服务定义
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scan(bean);

                //如果服务定义为空，则跳过
                if (serviceDefinition == null) {
                    continue;
                }

                //设置环境类型
                serviceDefinition.setEnvType(getApiProperties().getEnv());

                //创建服务实例
                ServiceInstance serviceInstance = new ServiceInstance();
                //获取本地IP地址
                String localIp = NetUtil.getLocalIp();
                //获取服务器端口
                int port = serverProperties.getPort();
                //生成服务实例ID
                String serviceInstanceId = localIp + COLON_SEPARATOR + port;
                //获取唯一标识符
                String uniqueId = serviceDefinition.getUniqueId();
                //获取版本号
                String version = serviceDefinition.getVersion();

                //设置服务实例属性
                serviceInstance.setServiceInstanceId(serviceInstanceId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setVersion(version);
                serviceInstance.setWeight(DEFAULT_WEIGHT);

                //如果配置为灰度发布，则设置灰度标志
                if (getApiProperties().isGray()) {
                    serviceInstance.setGray(true);
                }

                //注册服务
                register(serviceDefinition, serviceInstance);
            }
        }
    }
}
