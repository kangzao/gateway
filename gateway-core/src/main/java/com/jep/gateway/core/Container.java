package com.jep.gateway.core;

import com.jep.gateway.core.config.Config;
import com.jep.gateway.core.netty.NettyHttpClient;
import com.jep.gateway.core.netty.NettyHttpServer;
import com.jep.gateway.core.netty.processor.DisruptorNettyCoreProcessor;
import com.jep.gateway.core.netty.processor.NettyCoreProcessor;
import com.jep.gateway.core.netty.processor.NettyProcessor;
import lombok.extern.slf4j.Slf4j;

import static com.jep.gateway.common.constant.GatewayConst.BUFFER_TYPE_PARALLEL;

/**
 * 网关启动核心容器
 * @author enping.jep
 * @date 2025/1/27 21:33
 **/
@Slf4j
public class Container implements LifeCycle {

    private final Config config;

    private NettyHttpServer nettyHttpServer;

    private NettyHttpClient nettyHttpClient;

    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }

    /**
     * 初始化Netty处理器及服务器和客户端组件
     * 此方法根据配置信息初始化Netty处理器如果配置中指定的缓冲类型为并行处理类型，
     * 则使用DisruptorNettyCoreProcessor，否则直接使用NettyCoreProcessor同时，初始化NettyHttpServer
     * 和NettyHttpClient，为它们提供统一的事件循环组
     */
    @Override
    public void init() {
        // 创建Netty核心处理器实例
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        // 根据配置中的缓冲类型决定使用哪种Netty处理器
        if (BUFFER_TYPE_PARALLEL.equals(config.getBufferType())) {
            // 如果是并行处理类型，则创建DisruptorNettyCoreProcessor实例
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        } else {
            // 否则直接使用NettyCoreProcessor实例
            this.nettyProcessor = nettyCoreProcessor;
        }
        // 创建NettyHttpServer实例，传入配置和Netty处理器
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
        //  nettyHttpServer、nettyHttpClient 共用相同的 work_threadGroup
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWoker());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway started!");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutDown();
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
