package com.jep.gateway.example.disruptor.basic;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.nio.ByteBuffer;

public class LongEventMain {
    /**
     * 主程序入口
     * 创建并启动Disruptor实例，用于处理长事件
     * 该程序演示了如何使用Disruptor库来发布和处理事件
     */
    public static void main(String[] args) throws Exception {
        // 定义环形缓冲区的大小
        int bufferSize = 2;
        // 初始化Disruptor实例，包含事件工厂、缓冲区大小、线程工厂、生产者类型和等待策略
        Disruptor<LongEvent> disruptor = new Disruptor<>(new LongEventFactory(), bufferSize,
                DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, new BlockingWaitStrategy());
        // 设置事件处理器
        disruptor.handleEventsWith(new LongEventHandler());
        // 启动Disruptor
        disruptor.start();

        // 获取环形缓冲区的引用
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
        // 创建一个字节缓冲区，用于存储长整型值
        ByteBuffer bb = ByteBuffer.allocate(8);
        // 无限循环，生成并发布事件
        for (long l = 0; true; l++) {
            // 将长整型值放入字节缓冲区
            bb.putLong(0, l);
            // 发布事件，使用字节缓冲区中的值填充事件
            ringBuffer.publishEvent((event, sequence, buffer) -> event.set(buffer.getLong(0)), bb);
            // 每秒发布一次事件
            Thread.sleep(1000);
        }
    }
}