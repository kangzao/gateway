package com.jep.gateway.core;

/**
 * 组件生命周期接口
 *
 * @author enping.jep
 * @date 2025/1/27 21:34
 **/
public interface LifeCycle {
    /**
     * 初始化
     */
    void init();

    /**
     * 启动
     */
    void start();

    /**
     * 关闭
     */
    void shutdown();
}
