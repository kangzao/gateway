package com.conductor.gateway.core.netty.Processor;

import com.conductor.gateway.core.HttpRequestWrapper;

/**
 * @author enping.jep
 * @date 2023/08/15 17:06
 **/
public interface NettyProcessor {

    void process(HttpRequestWrapper wrapper);

    void start();

    void shutDown();
}
