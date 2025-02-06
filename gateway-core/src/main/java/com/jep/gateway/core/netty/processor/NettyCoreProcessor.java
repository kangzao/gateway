package com.jep.gateway.core.netty.processor;

import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.BaseException;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.context.HttpRequestWrapper;
import com.jep.gateway.core.filter.chain.FilterChainFactory;
import com.jep.gateway.core.filter.chain.FilterChainFactoryImpl;
import com.jep.gateway.core.helper.RequestHelper;
import com.jep.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty核心处理器
 * @author enping.jep
 * @date 2025/1/27 21:50
 **/
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    // 过滤器链工厂
    private final FilterChainFactory chainFactory = FilterChainFactoryImpl.getInstance();

    /**
     * 处理传入的 HTTP 请求。
     * @param wrapper 包含 FullHttpRequest 和 ChannelHandlerContext 的 HttpRequestWrapper。
     */
    @Override
    public void process(HttpRequestWrapper wrapper) {
        FullHttpRequest request = wrapper.getRequest();
        ChannelHandlerContext ctx = wrapper.getCtx();
        try {
            // 创建并填充 GatewayContext 以保存有关传入请求的信息
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);
            // 组装过滤器并执行过滤操作
            chainFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);
        } catch (BaseException e) {
            // 通过记录日志并发送适当的 HTTP 响应处理已知异常
            log.error("process error {} {}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (Throwable t) {
            // 通过记录日志并发送内部服务器错误响应处理未知异常。
            log.error("process unknown error", t);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            doWriteAndRelease(ctx, request, httpResponse);
        }

    }


    /**
     * 写入HTTP响应并释放请求，然后关闭连接
     * 本方法用于处理那些需要一次性响应并立即断开连接的HTTP请求
     *
     * @param ctx          上下文对象，包含处理网络事件的通用接口
     * @param request      接收到的HTTP请求
     * @param httpResponse 要写入的HTTP响应
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {
        // 写入并刷新响应，然后添加关闭channel的监听器，以便在释放资源后关闭channel
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        // 释放请求的引用计数，以便其可以被垃圾回收
        ReferenceCountUtil.release(request);
    }


    @Override
    public void start() {

    }

    @Override
    public void shutDown() {

    }
}
