package com.conductor.gateway.core.context.impl;

import com.conductor.common.config.Rule;
import com.conductor.gateway.common.util.AssertUtil;
import io.micrometer.core.instrument.Timer;
import com.conductor.gateway.core.request.impl.GatewayRequest;
import com.conductor.gateway.core.response.GatewayResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;


/**
 * @author Ian
 * @date 2023/06/28 17:19
 **/
public class GatewayContext extends BasicContext {

    private GatewayRequest request;

    private GatewayResponse response;

    private Rule rule;

    @Setter
    @Getter
    private boolean gray;

    @Setter
    @Getter
    private Timer.Sample timerSample;

    /**
     * 构造函数
     *
     * @param protocol
     * @param nettyCtx
     * @param keepAlive
     */
    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive,
                          GatewayRequest request, Rule rule) {
        super(protocol, nettyCtx, keepAlive);
        this.request = request;
        this.rule = rule;
    }


    //建造者模式构建对象
    public static class Builder {
        private String protocol;
        private ChannelHandlerContext nettyCtx;
        private boolean keepAlive;
        private GatewayRequest request;
        private Rule rule;

        private Builder() {

        }

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder setRequest(GatewayRequest request) {
            this.request = request;
            return this;
        }

        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public GatewayContext build() {
            AssertUtil.notNull(protocol, "protocol 不能为空");

            AssertUtil.notNull(nettyCtx, "nettyCtx 不能为空");

            AssertUtil.notNull(request, "request 不能为空");

            AssertUtil.notNull(rule, "rule 不能为空");
            return new GatewayContext(protocol, nettyCtx, keepAlive, request, rule);
        }
    }

    @Override
    public GatewayRequest getRequest() {
        return request;
    }

    public void setRequest(GatewayRequest request) {
        this.request = request;
    }

    @Override
    public GatewayResponse getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = (GatewayResponse) response;
    }

    @Override
    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /**
     * 根据过滤器ID获取对应的过滤器配置信息
     *
     * @param filterId
     * @return
     */
    public Rule.FilterConfig getFilterConfig(String filterId) {
        return rule.getFilterConfig(filterId);
    }

    public String getUniqueId() {
        return request.getUniqueId();
    }

    /**
     * 重写父类释放资源方法，用于正在释放资源
     */
    public void releaseRequest() {
        if (requestReleased.compareAndSet(false, true)) {
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
    }

    /**
     * 获取原始的请求对象
     *
     * @return
     */
    public GatewayRequest getOriginRequest() {
        return request;

    }

}
