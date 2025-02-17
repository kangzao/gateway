package com.jep.gateway.example.spring.appcontext;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * ApplicationContextAware的使用示例
 * 创建一个类并实现 ApplicationContextAware 接口。通过实现 setApplicationContext 方法，将 ApplicationContext 对象注入到当前类中。
 * @author enping.jep
 * @date 2025/2/17 21:19
 **/
@Component
public class MyApplicationContextAware implements ApplicationContextAware {
    // 提供一个静态方法，方便其他类获取 ApplicationContext 对象
    // 定义一个私有变量来存储 ApplicationContext 对象
    @Getter
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 在 setApplicationContext 方法中，将 ApplicationContext 对象赋值给静态变量
        context = applicationContext;
    }

}
