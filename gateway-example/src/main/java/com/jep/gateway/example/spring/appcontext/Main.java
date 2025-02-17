package com.jep.gateway.example.spring.appcontext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author enping.jep
 * @date 2025/2/17 21:24
 **/
public class Main {
    /**
     * 程序的主入口点
     * 负责初始化Spring容器，并调用服务方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // AnnotationConfigApplicationContext 支持自动扫描带有 @Component、@Service、@Repository、@Controller 等注解的类，并将它们注册为 Spring Bean
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 获取 MyService 并调用方法
        MyService myService = context.getBean(MyService.class);
        myService.doSomething();
    }
}
