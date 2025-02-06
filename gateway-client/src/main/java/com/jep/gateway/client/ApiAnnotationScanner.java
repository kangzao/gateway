package com.jep.gateway.client;

import com.jep.gateway.client.support.dubbo.DubboConstants;
import com.jep.gateway.common.config.DubboServiceInvoker;
import com.jep.gateway.common.config.HttpServiceInvoker;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInvoker;
import com.jep.gateway.common.constant.BasicConst;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.spring.ServiceBean;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 扫描注解
 *
 * @author enping.jep
 * @date 2025/1/28 11:28
 **/
public class ApiAnnotationScanner {
    private ApiAnnotationScanner() {
    }

    private static class SingletonHolder {
        static final ApiAnnotationScanner INSTANCE = new ApiAnnotationScanner();
    }

    public static ApiAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 扫描传入的bean对象，最终返回一个服务定义
     * 该方法主要用于解析带有@ApiService注解的类和带有@ApiInvoker注解的方法，生成服务定义对象
     *
     * @param bean 待扫描的bean对象
     * @param args 可变参数，用于传递特定协议（如Dubbo）的服务实例化对象
     * @return 返回生成的服务定义对象，如果入参不满足条件，则返回null
     */
    public ServiceDefinition scanner(Object bean, Object... args) {
        // 获取bean的类信息
        Class<?> aClass = bean.getClass();
        // 检查类上是否标注了@ApiService注解
        if (!aClass.isAnnotationPresent(ApiService.class)) {
            // 如果没有标注@ApiService注解，直接返回null
            return null;
        }

        // 获取@ApiService注解的信息
        ApiService apiService = aClass.getAnnotation(ApiService.class);
        // 服务ID
        String serviceId = apiService.serviceId();
        // 服务协议
        ApiProtocol protocol = apiService.protocol();
        // 路径模式
        String patternPath = apiService.patternPath();
        // 版本信息
        String version = apiService.version();


        // 创建服务调用器的映射表
        Map<String, ServiceInvoker> invokerMap = new HashMap<>();

        // 获取类中的所有方法
        Method[] methods = aClass.getMethods();
        // 如果方法数组不为空且长度大于0，则遍历方法
        if (methods.length > 0) {
            for (Method method : methods) {
                // 检查方法上是否标注了@ApiInvoker注解
                ApiInvoker apiInvoker = method.getAnnotation(ApiInvoker.class);
                if (apiInvoker == null) {
                    // 如果没有标注@ApiInvoker注解，跳过该方法
                    continue;
                }

                // 获取方法路径
                String path = apiInvoker.path();

                // 根据服务协议创建相应的服务调用器
                switch (protocol) {
                    case HTTP:
                        // 创建HTTP服务调用器
                        HttpServiceInvoker httpServiceInvoker = createHttpServiceInvoker(path);
                        // 将HTTP服务调用器放入映射表
                        invokerMap.put(path, httpServiceInvoker);
                        break;
                    case DUBBO:
                        // 获取Dubbo服务Bean
                        ServiceBean<?> serviceBean = (ServiceBean<?>) args[0];
                        // 创建Dubbo服务调用器
                        DubboServiceInvoker dubboServiceInvoker = createDubboServiceInvoker(path, serviceBean, method);

                        // 获取Dubbo服务调用器的版本信息
                        String dubboVersion = dubboServiceInvoker.getVersion();
                        // 如果Dubbo服务调用器的版本信息不为空且不等于blank字符串，则更新版本信息
                        if (!StringUtils.isBlank(dubboVersion)) {
                            version = dubboVersion;
                        }
                        // 将Dubbo服务调用器放入映射表
                        invokerMap.put(path, dubboServiceInvoker);
                        break;
                    default:
                        // 默认情况，不做任何操作
                        break;
                }
            }
            // 创建服务定义对象
            ServiceDefinition serviceDefinition = new ServiceDefinition();
            // 设置服务定义对象的属性
            serviceDefinition.setUniqueId(serviceId + BasicConst.COLON_SEPARATOR + version);
            serviceDefinition.setServiceId(serviceId);
            serviceDefinition.setVersion(version);
            serviceDefinition.setProtocol(protocol.getCode());
            serviceDefinition.setPatternPath(patternPath);
            serviceDefinition.setEnable(true);
            serviceDefinition.setInvokerMap(invokerMap);

            // 返回服务定义对象
            return serviceDefinition;
        }

        // 如果方法数组为空或长度为0，返回null
        return null;
    }


    /**
     * 构建HttpServiceInvoker对象
     */
    private HttpServiceInvoker createHttpServiceInvoker(String path) {
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(path);
        return httpServiceInvoker;
    }

    /**
     * 构建DubboServiceInvoker对象
     *
     * @param path        路径
     * @param serviceBean 服务bean
     * @param method      方法
     * @return DubboServiceInvoker实例
     */
    private DubboServiceInvoker createDubboServiceInvoker(String path, ServiceBean<?> serviceBean, Method method) {
        DubboServiceInvoker dubboServiceInvoker = new DubboServiceInvoker();
        dubboServiceInvoker.setInvokerPath(path);

        String methodName = method.getName();
        String registerAddress = serviceBean.getRegistry().getAddress();
        String interfaceClass = serviceBean.getInterface();

        dubboServiceInvoker.setRegisterAddress(registerAddress);
        dubboServiceInvoker.setMethodName(methodName);
        dubboServiceInvoker.setInterfaceClass(interfaceClass);

        String[] parameterTypes = new String[method.getParameterCount()];
        Class<?>[] classes = method.getParameterTypes();
        for (int i = 0; i < classes.length; i++) {
            parameterTypes[i] = classes[i].getName();
        }
        dubboServiceInvoker.setParameterTypes(parameterTypes);

        Integer serviceTimeout = serviceBean.getTimeout();
        if (serviceTimeout == null || serviceTimeout == 0) {
            // ProviderConfig 是一个配置类，用于定义服务提供者的相关配置
            ProviderConfig providerConfig = serviceBean.getProvider();
            if (providerConfig != null) {
                Integer providerTimeout = providerConfig.getTimeout();
                if (providerTimeout == null || providerTimeout == 0) {
                    //如果没有超时时间，就设置默认超时时间
                    serviceTimeout = DubboConstants.DUBBO_TIMEOUT;
                } else {
                    serviceTimeout = providerTimeout;
                }
            }
        }
        dubboServiceInvoker.setTimeout(serviceTimeout);
        String dubboVersion = serviceBean.getVersion();
        dubboServiceInvoker.setVersion(dubboVersion);
        return dubboServiceInvoker;
    }
}
