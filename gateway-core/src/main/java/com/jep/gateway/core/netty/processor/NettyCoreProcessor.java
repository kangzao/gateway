package com.jep.gateway.core.netty.processor;

import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.BaseException;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.context.HttpRequestWrapper;
import com.jep.gateway.core.filter.FilterChainFactory;
import com.jep.gateway.core.filter.FilterChainFactoryImpl;
import com.jep.gateway.core.helper.RequestHelper;
import com.jep.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author enping.jep
 * @date 2025/1/27 21:50
 **/
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    private FilterChainFactory chainFactory = FilterChainFactoryImpl.getInstance();

    @Override
    public void process(HttpRequestWrapper wrapper) {
        FullHttpRequest request = wrapper.getRequest();
        ChannelHandlerContext ctx = wrapper.getCtx();
        try {
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);
            //执行过滤器逻辑
            chainFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);
        } catch (BaseException e) {
            log.error("process error {} {}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (Throwable t) {
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
