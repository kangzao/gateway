package com.jep.gateway.example.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author enping.jep
 * @date 2025/3/8 21:28
 **/
public class App {
    public static void main(String[] args) {
        // 参数准备
        OrderEventFactory orderEventFactory = new OrderEventFactory();
        int ringBufferSize = 4;
        ExecutorService executor = Executors.newFixedThreadPool(1);
        /*
         *
         * 首先初始化一个 Disruptor 对象，Disruptor 有多个重载的构造函数。
         * 支持传入 EventFactory 、ringBufferSize （需要是2的幂次方）、
         * executor（用于执行EventHandler 的事件处理逻辑，一个 EventHandler 对应一个线程，一个线程只服务于一个 EventHandler ）、
         * 生产者模式（支持单生产者、多生产者）、阻塞等待策略。在创建 Disruptor 对象时，内部会创建好指定 size 的 RingBuffer 对象。
         *
         *1. 实例化disruptor对象
         *  1) eventFactory: 消息(event)工厂对象
         *  2) ringBufferSize: 容器的长度
         *  3) executor: 线程池(建议使用自定义线程池)，RejectedExecutionHandler
         *  4) ProducerType: 单生产者还是多生产者
         *  5) waitStrategy: 等待策略
         */
        Disruptor<OrderEvent> disruptor = new Disruptor<OrderEvent>(
                orderEventFactory,
                ringBufferSize,
                executor,
                ProducerType.SINGLE,//单生产者
                new BlockingWaitStrategy());
        // 2. 添加消费者的监听 可以通过该对象添加消费者 EventHandler

        /*
         * 将 EventHandler 消费者封装成 EventProcessor（实现了 Runnable 接口），
         * 提交到构建 Disruptor 时指定的 executor 对象中。
         * 由于 EventProcessor 的 run 方法是一个 while 循环，
         * 不断尝试从RingBuffer 中获取数据。因此可以说一个 EventHandler 对应一个线程，一个线程只服务于一个EventHandler。
         **/
        disruptor.handleEventsWith(new OrderEventHandler());
        // 3. 启动disruptor
        disruptor.start();

        // 4. 获取实际存储数据的容器: RingBuffer
        RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();
        // 创建生产者对象，用于向RingBuffer中发布事件
        OrderEventProducer producer = new OrderEventProducer(ringBuffer);
        // 分配一个容量为8字节的ByteBuffer，用于存储要发送的数据
        ByteBuffer bb = ByteBuffer.allocate(8);
        // 循环5次，向Disruptor中发送5个事件
        for (long i = 0; i < 5; i++) {
            // 将当前循环变量i放到ByteBuffer的起始位置(索引0处)
            bb.putLong(0, i);
            // 通过生产者将数据发布到RingBuffer中
            producer.sendData(bb);
        }
        // 关闭disruptor，释放资源
        disruptor.shutdown();
        // 关闭线程池，释放资源
        executor.shutdown();

    }

}
