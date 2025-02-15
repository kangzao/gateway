package com.jep.gateway.client.autoconfigure;

import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.client.support.SpringMVCClientRegisterManager;
import com.jep.gateway.client.support.dubbo.DubboClientRegisterManager;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.beans.factory.annotation.Autowired;
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
 * @author enping.jep
 * @date 2025/1/28 11:19
 *
 **/
@Configuration  //@Configuration会被 @SpringBootApplication扫描到，进而把它和它下面的 @Bean加入容器
//EnableConfigurationProperties 将标注了@ConfigurationProperties注解的类注入到Spring容器中。
//该注解是用来开启对@ConfigurationProperties注解的支持。
@EnableConfigurationProperties(ApiProperties.class)
//ConditionalOnProperty(prefix = "api", name = {"registerAddress"})：
// 这个注解表示当前自动配置类只有在配置文件中存在 api.registerAddress 属性时才会被激活。
@ConditionalOnProperty(prefix = "api", name = {"registerAddress"})
public class ApiClientAutoConfiguration {
    private final ApiProperties apiProperties;

    //如果只有一个有参构造函数，就调用这一个
    public ApiClientAutoConfiguration(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    /**
     * 只有在 ConditionalOnClass 存在且没有 ConditionalOnMissingBean  时才会注册 SpringMVCClientRegisterManager。
     */
    @Bean
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
    @ConditionalOnMissingBean(SpringMVCClientRegisterManager.class)
    public SpringMVCClientRegisterManager springMVCClientRegisterManager() {
        return new SpringMVCClientRegisterManager(apiProperties);
    }

    /**
     * 创建DubboClientRegisterManager的Bean定义
     * 该方法在满足特定条件时被调用，负责初始化和配置DubboClientRegisterManager实例
     *
     * @return DubboClientRegisterManager实例，用于管理Dubbo客户端的注册过程
     *
     * @ConditionalOnClass 注解指定该Bean仅在当前类路径中存在ServiceBean类时才创建
     * @ConditionalOnMissingBean 注解确保仅在容器中不存在DubboClientRegisterManager bean时才创建，
     * 这避免了重复创建该bean，确保了配置的一致性和唯一性
     */
    @Bean
    @ConditionalOnClass({ServiceBean.class})
    @ConditionalOnMissingBean(DubboClientRegisterManager.class)
    public DubboClientRegisterManager dubboClientRegisterManager() {
        return new DubboClientRegisterManager(apiProperties);
    }
}
