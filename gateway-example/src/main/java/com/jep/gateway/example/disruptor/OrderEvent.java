package com.jep.gateway.example.disruptor;

import lombok.Getter;
import lombok.Setter;

/**
 * Event 是具体的数据实体，生产者生产 Event ，存入 RingBuffer，
 * 消费者从 RingBuffer 中消费它进行逻辑处理。Event 就是一个普通的 Java 对象，无需实现 Disruptor 内定义的接口。
 * @author enping.jep
 * @date 2025/3/8 21:21
 **/
@Getter
@Setter
public class OrderEvent {

    private long value;
}
