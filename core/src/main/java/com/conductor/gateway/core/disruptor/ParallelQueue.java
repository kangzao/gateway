package com.conductor.gateway.core.disruptor;

/**
 * @author enping.jep
 * @date 2023/08/22 11:25
 **/
public interface ParallelQueue<E> {

    /**
     * 添加元素
     *
     * @param event
     */
    void add(E event);

    void add(E... event);

    /**
     * 添加多个元素
     *
     * @param event
     * @return
     */
    boolean tryAdd(E event);

    boolean tryAdd(E... event);

    /**
     * 启动
     */
    void start();

    /**
     * 销毁
     */
    void shutDown();

    /**
     * 判断是否已经销毁
     */
    boolean isShutDown();

}