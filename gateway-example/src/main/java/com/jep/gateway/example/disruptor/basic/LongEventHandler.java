package com.jep.gateway.example.disruptor.basic;

import com.lmax.disruptor.EventHandler;

public class LongEventHandler implements EventHandler<LongEvent> {
    /**
     * 当有LongEvent事件发生时，该方法会被调用
     *
     * @param longEvent 触发的长事件对象，包含事件的具体信息
     * @param sequence 事件的序列号，用于标识事件的顺序
     * @param endOfBatch 标志是否为一批次事件处理的结束标志
     * @throws Exception 如果事件处理过程中发生异常，则抛出此异常
     */
    @Override
    public void onEvent(LongEvent longEvent, long sequence, boolean endOfBatch) throws Exception {
        // 打印当前线程名称和事件信息，用于调试和日志记录
        System.out.println("currentThread:" + Thread.currentThread().getName() + " Event: " + longEvent);
    }
}