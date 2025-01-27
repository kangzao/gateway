package com.jep.gateway.core.disruptor;

/**
 * 事件监听处理器
 * @author enping.jep
 * @date 2025/1/27 22:26
 **/
public interface EventListener<E> {

    /**
     * 事件处理方法
     */
    void onEvent(E event);

    /**
     * 异常处理方法
     */
    void onException(Throwable ex, long sequence, E event);
}
