package com.jep.gateway.core.netty.processor;

import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.core.config.Config;
import com.jep.gateway.core.context.HttpRequestWrapper;
import com.jep.gateway.core.disruptor.EventListener;
import com.jep.gateway.core.disruptor.ParallelQueueHandler;
import com.jep.gateway.core.helper.ResponseHelper;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Disruptor流程处理类
 * 负责将接收到的HTTP请求封装成事件，并通过Disruptor框架进行异步处理。
 * 通过Disruptor的事件监听机制，可以实现高并发下的请求处理。
 * 使用Disruptor框架来异步处理HTTP请求。代码中包含了构造函数、请求处理方法、事件监听处理器以及启动和关闭Disruptor队列处理器的方法。
 * 同时，还包含了异常处理逻辑，用于在事件处理过程中出现异常时记录日志并发送错误响应。
 *
 * 原始的 NettyCoreProcessor 直接处理每个 HTTP 请求，
 * 而 DisruptorNettyCoreProcessor 使用了 Disruptor 框架，
 * 将 HTTP 请求异步地添加到一个处理队列中，然后由 BatchEventListenerProcessor 来处理这个队列中的事件。
 * Disruptor 是一个高性能的异步事件处理框架，它采用了无锁的设计，通过利用 RingBuffer 的结构，实现了高效的事件发布和消费。
 * 在这里，使用 Disruptor 的好处是可以提高并发处理能力，减轻了 Netty 核心处理器的负担。
 * 因为网络请求通常是 I/O 密集型的操作，通过异步处理可以提高系统的吞吐量。
 *
 *
 * @author enping.jep
 * @date 2025/1/27 22:20
 **/
@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor {

    private static final String THREAD_NAME_PREFIX = "gateway-queue-";

    private Config config;

    // 网关核心处理器，用于实际处理请求
    private NettyCoreProcessor nettyCoreProcessor;

    // 并行队列处理器，用于异步处理请求
    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;

    // 构造函数，初始化Disruptor相关组件
    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;
        // 构建并行队列处理器
        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())//缓冲区大小
                .setThreads(config.getProcessThread())//设置处理线程数量
                .setProducerType(ProducerType.MULTI)//设置生产者类型为多生产者
                .setNamePrefix(THREAD_NAME_PREFIX)//设置线程名前缀
                .setWaitStrategy(config.getWaitStrategy());//设置等待策略

        // 创建批量事件监听处理器
        BatchEventListenerProcessor batchEventListenerProcessor = new BatchEventListenerProcessor();
        builder.setListener(batchEventListenerProcessor);
        this.parallelQueueHandler = builder.build();

    }

    // 处理请求的方法，将请求添加到Disruptor队列中
    @Override
    public void process(HttpRequestWrapper wrapper) {
        this.parallelQueueHandler.add(wrapper);
    }


    public class BatchEventListenerProcessor implements EventListener<HttpRequestWrapper> {
        // 处理事件的方法，将事件委托给网关核心处理器处理
        @Override
        public void onEvent(HttpRequestWrapper event) {
            nettyCoreProcessor.process(event);

        }

        @Override
        // 处理异常的方法，当事件处理过程中出现异常时调用
        public void onException(Throwable ex, long sequence, HttpRequestWrapper event) {
            HttpRequest request = event.getRequest();
            ChannelHandlerContext ctx = event.getCtx();
            try {
                log.error("BatchEventListenerProcessor onException请求写回失败，request:{},errMsg:{} ", request, ex.getMessage(), ex);

                //构建响应对象
                FullHttpResponse fullHttpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                if (!HttpUtil.isKeepAlive(request)) {
                    // 如果请求不是长连接，则在发送响应后关闭连接
                    ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    // 如果请求是长连接，则保持连接
                    fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.writeAndFlush(fullHttpResponse);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor onException请求写回失败，request:{},errMsg:{} ", request, e.getMessage(), e);
            }
        }
    }

    // 启动Disruptor队列处理器
    @Override
    public void start() {
        parallelQueueHandler.start();
    }

    // 关闭Disruptor队列处理器
    @Override
    public void shutDown() {
        parallelQueueHandler.shutDown();
    }
}
