package com.jep.gateway.core.filter.annotation;

import java.lang.annotation.*;


/**
 * @author enping.jep
 * @date 2025/1/27 22:01
 **/

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface FilterAspect {

    /**
     * 过滤器ID
     */
    String id();

    /**
     * 过滤器名称
     */
    String name() default "";

    /**
     * 排序
     */
    int order() default 0;
}
