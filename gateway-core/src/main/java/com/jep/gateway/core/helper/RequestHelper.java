package com.jep.gateway.core.helper;

import com.jep.gateway.common.config.*;
import com.jep.gateway.common.constant.BasicConst;
import com.jep.gateway.common.constant.GatewayConst;
import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.ResponseException;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.request.GatewayRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @author enping.jep
 * @date 2025/1/27 22:14
 **/
public class RequestHelper {

    /**
     * 封装网关上下文
     */
    public static GatewayContext doContext(FullHttpRequest request, ChannelHandlerContext context) {
        // 封装网关内部请求对象
        GatewayRequest gatewayRequest = doRequest(request, context);

        // 根据请求id获取请求服务定义信息
        ServiceDefinition definition = DynamicConfigManager.getInstance().getServiceDefinition(gatewayRequest.getUniqueId());

        // 服务调用对象初始化
        ServiceInvoker serviceInvoker = new HttpServiceInvoker();
        serviceInvoker.setInvokerPath(gatewayRequest.getPath());
        serviceInvoker.setTimeout(500);

        // 获取具体服务对象访问规则
        Rule rule = getRule(gatewayRequest, definition.getServiceId());

        return GatewayContext.newBuilder()
                .setProtocol(definition.getProtocol())
                .setKeepAlive(HttpUtil.isKeepAlive(request))
                .setNettyCtx(context)
                .setRequest(gatewayRequest)
                .setRule(rule).build();
    }

    /**
     * 封装 GatewayRequest
     */
    private static GatewayRequest doRequest(FullHttpRequest request, ChannelHandlerContext context) {
        HttpHeaders headers = request.headers();
        String uniqueId = headers.get(GatewayConst.UNIQUE_ID);
        String host = headers.get(HttpHeaderNames.HOST);

        HttpMethod method = request.method();
        String uri = request.uri();
        String clientIp = getClientIp(context, request);
        Charset charset = HttpUtil.getCharset(request, StandardCharsets.UTF_8);
        String contentType = HttpUtil.getMimeType(request) == null ? null : HttpUtil.getMimeType(request).toString();

        return new GatewayRequest(uniqueId, charset, clientIp, host, uri, method, contentType, headers, request);
    }

    /**
     * 获取客户端IP地址
     */
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
        String clientIp = null;

        // X-Forwarded-For 是 http 的一个标准规范，其值是X-Forwarded-For: client, proxy1, proxy2... 用户真实 IP 为 client
        // 由于可以伪造，所以在一些安全场景下获取用户真实 IP 不可靠
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(","));
            if (!values.isEmpty() && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }

        // 得不到就从 Channel 中得到
        if (clientIp == null) {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            clientIp = address.getAddress().getHostAddress();
        }

        return clientIp;
    }

    /**
     * 解析请求 url 对应的规则匹配
     */
    private static Rule getRule(GatewayRequest request, String serviceId) {
        // 拼接服务ID + 请求路径
        String key = serviceId + "." + request.getPath();
        Rule rule = DynamicConfigManager.getInstance().getRulePath(key);
        if (rule != null) {
            return rule;
        }

        List<Rule> rules = DynamicConfigManager.getInstance().getRuleByServiceId(serviceId);
        Rule matchedRule = null;

        for (Rule r : rules) {
            if (request.getPath().startsWith(r.getPrefix())) {
                matchedRule = r;
                break;
            }
        }

        if (matchedRule == null) {
            throw new ResponseException(ResponseCode.PATH_NO_MATCHED);
        }
        return matchedRule;
    }

//    /user/private/user-info
}
