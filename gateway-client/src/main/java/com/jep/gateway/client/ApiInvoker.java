package com.jep.gateway.client;

import java.lang.annotation.*;

/**
 * @author enping.jep
 * @date 2025/1/28 11:35
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiInvoker {
    String path();
}
