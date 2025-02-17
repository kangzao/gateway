package com.jep.gateway.example.spring.appcontext;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author enping.jep
 * @date 2025/2/17 21:21
 **/
@Service
public class MyService {
    public void doSomething() {
        // 通过 MyApplicationContextAware 获取 ApplicationContext
        ApplicationContext context = MyApplicationContextAware.getContext();

        // 从 ApplicationContext 中获取其他 Bean
        OtherBean otherBean = context.getBean(OtherBean.class);
        otherBean.doSomethingElse();
    }
}
