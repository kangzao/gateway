package com.jep.gateway.example.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author enping.jep
 * @date 2025/2/13 21:45
 **/
@Component
public class AppRunner implements CommandLineRunner {

    private final AppService appService;

    @Autowired
    public AppRunner(AppService appService) {
        this.appService = appService;
    }

    /**
     * 以可变参数方式接收输入参数并运行应用
     * 本方法主要用于执行应用的一些初始化或运行时信息的打印
     *
     * @param args 可变参数字符串数组，用于接收从命令行传入的参数
     * @throws Exception 如果应用的运行过程中抛出了异常，则向调用者抛出异常
     */
    @Override
    public void run(String... args) throws Exception {
        // 打印应用的相关信息
        appService.printAppInfo();
    }
}
