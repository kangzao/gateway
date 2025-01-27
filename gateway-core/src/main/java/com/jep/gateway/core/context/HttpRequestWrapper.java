package com.jep.gateway.core.context;

import lombok.Data;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @author enping.jep
 * @date 2025/1/27 21:38
 **/
@Data
public class HttpRequestWrapper {
    private FullHttpRequest request;
    private ChannelHandlerContext ctx;
}
