package com.jep.gateway.client;

import java.lang.annotation.*;

/**
 * 服务定义注解
 * @author enping.jep
 * @date 2025/1/28 11:29
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiService {
    String serviceId();

    String version() default "1.0.0";

    ApiProtocol protocol();

    String patternPath();
}
