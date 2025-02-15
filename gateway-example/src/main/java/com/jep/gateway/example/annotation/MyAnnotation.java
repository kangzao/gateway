package com.jep.gateway.example.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author enping.jep
 * @date 2025/2/12 22:47
 **/
// 注解的作用目标：方法
@Target(ElementType.METHOD)
// 注解的生命周期：运行时保留
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAnnotation {
    // 定义一个属性 value，默认值为空字符串
    String value() default "";

    // 定义一个属性 enabled，默认值为 true
    boolean enabled() default true;
}
