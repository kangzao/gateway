package com.jep.gateway.core.netty.processor;

import com.jep.gateway.core.context.HttpRequestWrapper;

/**
 * 自定义的Netty处理器接口，用于处理接收到的请求
 * @author enping.jep
 * @date 2025/1/27 21:38
 **/
public interface NettyProcessor {

    void process(HttpRequestWrapper wrapper);

    void start();

    void shutDown();
}
