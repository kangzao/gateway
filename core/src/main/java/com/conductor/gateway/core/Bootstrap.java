package com.conductor.gateway.core;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author jep
 * @version 1.0
 * @description 启动类
 * @date 2023/6/28 15:35:05
 */
@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        //加载网关核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        System.out.println(config.getPort());


    }
}
