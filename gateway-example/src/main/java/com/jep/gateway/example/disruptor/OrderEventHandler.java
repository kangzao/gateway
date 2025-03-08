package com.jep.gateway.example.disruptor;

import com.lmax.disruptor.EventHandler;

/**
 * @author enping.jep
 * @date 2025/3/8 21:26
 **/
public class OrderEventHandler implements EventHandler<OrderEvent> {
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("消费者: " + event.getValue());
    }
}
