package com.jep.gateway.core.netty;

import com.jep.gateway.core.LifeCycle;
import com.jep.gateway.core.config.Config;
import com.jep.gateway.core.helper.AsyncHttpHelper;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;

/**
 * 负责创建和管理基于Netty的异步HTTP客户端
 * NettyHttpClient 类负责创建和管理基于Netty的异步HTTP客户端。
 * 它实现了LifeCycle接口，以提供初始化、启动和关闭客户端的方法。
 * @author enping.jep
 * @date 2025/1/27 21:45
 **/
@Slf4j
public class NettyHttpClient implements LifeCycle {
    private final Config config;

    private final EventLoopGroup eventLoopGroupWoker;

    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWoker) {
        this.config = config;
        this.eventLoopGroupWoker = eventLoopGroupWoker;
        init();
    }

    @Override
    public void init() {
        // 使用自定义配置构建异步HTTP客户端
        // AsyncHttpClient 是一个建立在 Netty 之上的异步 HTTP 客户端库，它提供了简单的 API 来执行 HTTP 请求并处理响应。
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                // 设置EventLoopGroup，用于处理I/O操作
                .setEventLoopGroup(eventLoopGroupWoker)
                // 设置连接超时时间
                .setConnectTimeout(config.getHttpConnectTimeout())
                // 设置请求超时时间
                .setRequestTimeout(config.getHttpRequestTimeout())
                // 设置最大重定向次数
                .setMaxRedirects(config.getHttpMaxRequestRetry())
                // 使用池化的ByteBuf分配器，以提高性能
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                // 启用压缩功能
                .setCompressionEnforced(true)
                // 设置最大连接数
                .setMaxConnections(config.getHttpMaxConnections())
                // 设置每个主机的最大连接数
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
                // 设置连接空闲超时时间
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());
        // 创建并初始化异步HTTP客户端
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
    }

    @Override
    public void start() {
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        if (asyncHttpClient != null) {
            try {
                this.asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttpClient shutdown error", e);
            }
        }
    }
}
