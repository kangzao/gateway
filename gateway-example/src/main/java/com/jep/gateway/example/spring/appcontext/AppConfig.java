package com.jep.gateway.example.spring.appcontext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author enping.jep
 * @date 2025/2/17 21:23
 **/
@Configuration
public class AppConfig {
    @Bean
    public MyApplicationContextAware myApplicationContextAware() {
        return new MyApplicationContextAware();
    }

    @Bean
    public MyService myService() {
        return new MyService();
    }

    @Bean
    public OtherBean otherBean() {
        return new OtherBean();
    }
}
