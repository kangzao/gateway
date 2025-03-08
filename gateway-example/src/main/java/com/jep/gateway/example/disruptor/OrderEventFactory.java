package com.jep.gateway.example.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * @author enping.jep
 * @date 2025/3/8 21:22
 **/
public class OrderEventFactory implements EventFactory<OrderEvent> {
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
}
