package com.jep.gateway.example.disruptor;

import com.lmax.disruptor.RingBuffer;

import java.nio.ByteBuffer;


/**
 * @author enping.jep
 * @date 2025/3/8 21:23
 * <p>
 * RingBuffer 内部维护了一个 Object 数组（也就是真正存储数据的容器），
 * 在 RingBuffer 初始化时该 Object 数组就已经使用 EventFactory 初始化了一些空 Event，后续就不需要在运行时来创建了，提高性能。
 * 因此这里通过 RingBuffer 获取指定序号得到的是一个空对象，需要对它进行赋值后，才能进行发布。
 * 这里通过 RingBuffer 的 next 方法获取可用序号，如果 RingBuffer 空间不足会阻塞。通过 next 方法获取序号后，需要确保接下来使用 publish 方法发布数据。
 **/
public class OrderEventProducer {
    private RingBuffer<OrderEvent> ringBuffer;

    public OrderEventProducer(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void sendData(ByteBuffer data) {
        // 1、在生产者发送消息的时候, 首先需要从我们的ringBuffer里面获取一个可用的序号
        long sequence = ringBuffer.next();
        try {
            //2、注意此时获取的OrderEvent对象是一个没有被赋值的空对象
            OrderEvent event = ringBuffer.get(sequence);
            //3、进行实际的赋值处理
            event.setValue(data.getLong(0));
        } finally {
            //4、 提交发布操作
            ringBuffer.publish(sequence);
        }
    }
}
