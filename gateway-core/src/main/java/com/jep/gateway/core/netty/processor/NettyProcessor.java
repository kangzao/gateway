package com.jep.gateway.core.netty.processor;

import com.jep.gateway.core.context.HttpRequestWrapper;

/**
 * @author enping.jep
 * @date 2025/1/27 21:38
 **/
public interface NettyProcessor {

    void process(HttpRequestWrapper wrapper);

    void start();

    void shutDown();
}
