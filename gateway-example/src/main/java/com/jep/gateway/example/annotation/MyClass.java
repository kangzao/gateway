package com.jep.gateway.example.annotation;

/**
 * @author enping.jep
 * @date 2025/2/12 22:48
 **/
public class MyClass {
    // 使用注解，并设置 value 和 enabled 属性
    @MyAnnotation(value = "Hello, Annotation!", enabled = false)
    public void myMethod() {
        System.out.println("This is myMethod.");
    }

    // 使用注解，只设置 value 属性
    @MyAnnotation("Another method")
    public void anotherMethod() {
        System.out.println("This is anotherMethod.");
    }

    // 未使用注解的方法
    public void normalMethod() {
        System.out.println("This is a normal method.");
    }
}

