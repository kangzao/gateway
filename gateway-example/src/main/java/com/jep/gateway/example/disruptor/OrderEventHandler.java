package com.jep.gateway.example.disruptor;

import com.lmax.disruptor.EventHandler;

/**
 *
 * 建立监听事件类用于处理数据
 * 监听事件类就是Event处理器，处理的数据就是Event类实例对象。
 *
 * @author enping.jep
 * @date 2025/3/8 21:26
 **/
public class OrderEventHandler implements EventHandler<OrderEvent> {
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("消费者: " + event.getValue());
    }
}
