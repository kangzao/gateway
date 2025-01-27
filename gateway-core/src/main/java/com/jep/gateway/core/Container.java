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

    @Override
    public void init() {
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        if (BUFFER_TYPE_PARALLEL.equals(config.getBufferType())) {
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        } else {
            this.nettyProcessor = nettyCoreProcessor;
        }
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
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
