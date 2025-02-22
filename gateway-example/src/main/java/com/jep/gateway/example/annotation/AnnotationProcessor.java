package com.jep.gateway.example.annotation;

import java.lang.reflect.Method;

/**
 * 通过反射读取注解信息，并根据注解属性执行逻辑。
 *
 * @author enping.jep
 * @date 2025/2/12 22:48
 **/
public class AnnotationProcessor {

    public static void main(String[] args) {
        // 获取 MyClass 的所有方法
        Method[] methods = MyClass.class.getDeclaredMethods();

        // 遍历方法
        for (Method method : methods) {
            // 检查方法是否标记了 @MyAnnotation
            if (method.isAnnotationPresent(MyAnnotation.class)) {
                // 获取注解实例
                MyAnnotation annotation = method.getAnnotation(MyAnnotation.class);

                // 输出注解信息
                System.out.println("Method: " + method.getName());
                System.out.println("Value: " + annotation.value());
                System.out.println("Enabled: " + annotation.enabled());

                // 根据 enabled 属性决定是否执行方法
                if (annotation.enabled()) {
                    try {
                        // 调用方法
                        method.invoke(new MyClass());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("-------------------");
            }
        }
    }
}
