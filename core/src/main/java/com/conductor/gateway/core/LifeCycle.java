package com.conductor.gateway.core;

/**
 * 组件声明周期
 *
 * @author enping.jep
 * @date 2023/08/15 16:41
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
