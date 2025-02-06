package com.jep.gateway.core.netty;

import com.jep.gateway.common.util.RemotingUtil;
import com.jep.gateway.core.LifeCycle;
import com.jep.gateway.core.config.Config;
import com.jep.gateway.core.netty.processor.NettyProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 接收外部请求并在内部进行流转
 * @author enping.jep
 * @date 2025/1/27 21:36
 **/
@Slf4j
public class NettyHttpServer implements LifeCycle {
    private final Config config;
    private final NettyProcessor nettyProcessor;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;

    @Getter
    private EventLoopGroup eventLoopGroupWoker;


    public NettyHttpServer(Config config, NettyProcessor nettyProcessor) {
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        init();
    }


    /**
     * 初始化Netty服务器配置
     * 本方法根据环境选择合适的事件循环组（EventLoopGroup）以支持不同的网络传输
     * 使用Epoll或NioEventLoopGroup取决于当前环境是否支持Epoll
     * 这是为了确保在不同的运行环境下都能达到最优的性能表现
     */
    @Override
    public void init() {
        // 创建ServerBootstrap实例，用于服务器的配置与启动
        this.serverBootstrap = new ServerBootstrap();

        // 根据环境选择使用Epoll还是Nio
        if (useEpoll()) {
            // 如果环境支持Epoll，创建EpollEventLoopGroup实例
            // 这里为Boss和Worker线程组分别创建实例，以处理不同的网络事件
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWoker = new EpollEventLoopGroup(config.getEventLoopGroupWokerNum(),
                    new DefaultThreadFactory("netty-woker-nio"));
        } else {
            // 如果环境不支持Epoll，回退到NioEventLoopGroup
            // 同样为Boss和Worker线程组分别创建实例
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWoker = new NioEventLoopGroup(config.getEventLoopGroupWokerNum(),
                    new DefaultThreadFactory("netty-woker-nio"));
        }
    }

    public boolean useEpoll() {
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    /**
     * 启动服务器
     * 配置服务器的线程组、通道类型以及各种通道选项，并为子通道设置初始化处理器
     * 尝试绑定端口并启动服务器，如果成功启动，记录日志信息；如果启动过程中发生异常，则抛出运行时异常
     */
    @Override
    public void start() {
        // 配置服务器的线程组、通道类型以及各种通道选项
        this.serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWoker) // 设置线程组，boss线程组用于接受客户端连接，worker线程组处理网络事件
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class) // 根据是否使用Epoll来选择通道类型
                .option(ChannelOption.SO_BACKLOG, 1024) // 设置服务器Socket的监听队列大小，影响并发连接请求的处理能力
                .option(ChannelOption.SO_REUSEADDR, true) // 启用端口重绑定，允许服务器在重启时立即使用上次的端口，避免TIME_WAIT状态的限制
                .option(ChannelOption.SO_KEEPALIVE, false) // 禁用保持连接功能，避免自动发送探测数据报文来维护连接
                .childOption(ChannelOption.TCP_NODELAY, true) // 禁用Nagle算法，提高传输效率，适用于需要低延迟的场景
                .childOption(ChannelOption.SO_SNDBUF, 65535) // 设置子通道的发送缓冲区大小，影响发送数据包的数量和速度
                .childOption(ChannelOption.SO_RCVBUF, 65535) // 设置子通道的接收缓冲区大小，影响接收数据包的数量和速度
                .localAddress(new InetSocketAddress(config.getPort())) // 绑定服务器监听的端口
                .childHandler(new ChannelInitializer<Channel>() { // 为子通道设置一个通道初始化处理器
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        // 配置子通道的管道处理器
                        ch.pipeline().addLast(
                                new HttpServerCodec(), // 负责对HTTP请求和响应进行编解码
                                new HttpObjectAggregator(config.getMaxContentLength()), // 将多个HTTP对象聚合成一个FullHttpRequest或FullHttpResponse
                                new HttpServerExpectContinueHandler(), // 处理HTTP的Expect-Continue请求
                                new NettyHttpServerHandler(nettyProcessor), // 自定义的HTTP请求处理处理器 入站
                                new NettyServerConnectManagerHandler() // 自定义的连接管理处理器
                        );
                    }
                });

        // 尝试绑定端口并启动服务器
        try {
            this.serverBootstrap.bind().sync(); // 同步等待直到绑定操作完成
            log.info("server startup on port {}", this.config.getPort()); // 记录服务器启动成功的日志信息
        } catch (Exception e) {
            throw new RuntimeException(); // 如果启动过程中发生异常，则抛出运行时异常
        }
    }


    @Override
    public void shutdown() {
        if (eventLoopGroupBoss != null) {
            eventLoopGroupBoss.shutdownGracefully();
        }

        if (eventLoopGroupWoker != null) {
            eventLoopGroupWoker.shutdownGracefully();
        }
    }
}
