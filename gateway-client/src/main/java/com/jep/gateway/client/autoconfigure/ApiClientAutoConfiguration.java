package com.jep.gateway.client.autoconfigure;

import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.client.support.SpringMVCClientRegisterManager;
import com.jep.gateway.client.support.dubbo.DubboClientRegisterManager;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.servlet.Servlet;

/**
 * spring.factories中使用EnableAutoConfiguration引入需要加载的类，Spring Boot应用在启动时，
 * 会自动扫描每个Jar的../META-INF/spring.factories文件，因此这种方式可以加载到类
 * EnableConfigurationProperties 将标注了@ConfigurationProperties注解的类注入到Spring容器中。
 * 该注解是用来开启对@ConfigurationProperties注解的支持。
 * ConditionalOnProperty(prefix = "api", name = {"registerAddress"})：
 * 这个注解表示当前自动配置类只有在配置文件中存在 api.registerAddress 属性时才会被激活。
 *
 * @author enping.jep
 * @date 2025/1/28 11:19
 **/
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@ConditionalOnProperty(prefix = "api", name = {"registerAddress"})
public class ApiClientAutoConfiguration {
    private final ApiProperties apiProperties;

    public ApiClientAutoConfiguration(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    /**
     * 只有在 ConditionalOnClass 存在且没有 ConditionalOnMissingBean  时才会注册 SpringMVCClientRegisterManager。
     *
     * @return
     */
    @Bean
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
    @ConditionalOnMissingBean(SpringMVCClientRegisterManager.class)
    public SpringMVCClientRegisterManager springMVCClientRegisterManager() {
        return new SpringMVCClientRegisterManager(apiProperties);
    }

    @Bean
    @ConditionalOnClass({ServiceBean.class})
    @ConditionalOnMissingBean(DubboClientRegisterManager.class)
    public DubboClientRegisterManager dubboClientRegisterManager() {
        return new DubboClientRegisterManager(apiProperties);
    }
}
