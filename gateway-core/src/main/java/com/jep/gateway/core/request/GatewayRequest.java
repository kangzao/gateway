package com.jep.gateway.core.request;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import com.jep.gateway.common.constant.BasicConst;
import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.ResponseException;
import com.jep.gateway.common.util.TimeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;

import java.nio.charset.Charset;
import java.util.*;

/**
 * @author enping.jep
 * @date 2025/1/27 22:15
 **/
@Slf4j
public class GatewayRequest implements IGatewayRequest {

    @Getter
    @Setter
    private String id;

    /**
     * 服务ID
     */
    @Getter
    private final String uniqueId;

    /**
     * 请求进入网关时间
     */
    @Getter
    private final long beginTime;

    /**
     * 字符集不会变的
     */
    @Getter
    private final Charset charset;

    /**
     * 客户端的IP，主要用于做流控、黑白名单
     */
    @Getter
    private final String clientIp;

    /**
     * 请求的地址：IP：port
     */
    @Getter
    private final String host;

    /**
     * 请求的路径   /XXX/XXX/XX
     */
    @Getter
    private final String path;

    /**
     * URI：统一资源标识符，/XXX/XXX/XXX?attr1=value&attr2=value2
     * URL：统一资源定位符，它只是URI的子集一个实现
     */
    @Getter
    private final String uri;

    /**
     * 请求方法 post/put/GET
     */
    @Getter
    private final HttpMethod method;

    /**
     * 请求的格式
     */
    @Getter
    private final String contentType;

    /**
     * 请求头信息
     */
    @Getter
    private final HttpHeaders headers;

    /**
     * 参数解析器
     */
    @Getter
    private final QueryStringDecoder queryStringDecoder;

    /**
     * FullHttpRequest
     */
    @Getter
    // 定义一个不可变的FullHttpRequest对象，用于存储和处理完整的HTTP请求
    private final FullHttpRequest fullHttpRequest;


    /**
     * 请求体
     */
    @Getter
    private String body;


    @Setter
    @Getter
    private long userId;

    /**
     * 请求Cookie
     */
    @Getter
    private Map<String, Cookie> cookieMap;

    /**
     * post请求定义的参数结合
     */
    @Getter
    private Map<String, List<String>> postParameters;


    /******可修改的请求变量***************************************/
    /**
     * 可修改的Scheme，默认是http://
     */
    private String modifyScheme;

    private String modifyHost;

    private String modifyPath;

    /**
     * 构建下游请求是的http请求构建器
     */
    private final RequestBuilder requestBuilder;


    /**
     * GatewayRequest类的构造函数，用于初始化一个网关请求对象
     *
     * @param uniqueId        请求的唯一标识符，用于跟踪和日志记录
     * @param charset         请求字符集，用于处理请求中的字符编码
     * @param clientIp        发起请求的客户端IP地址，用于识别请求来源
     * @param host            请求的主机地址，用于路由和安全控制
     * @param uri             请求的统一资源标识符，表示请求的具体资源位置
     * @param method          请求方法（如GET, POST等），定义了请求的操作类型
     * @param contentType     请求的内容类型，描述了请求体的数据格式
     * @param headers         请求的头部信息，包含与请求相关的元数据
     * @param fullHttpRequest 完整的HTTP请求对象，封装了请求的所有信息
     */
    public GatewayRequest(String uniqueId, Charset charset, String clientIp, String host, String uri, HttpMethod method, String contentType, HttpHeaders headers, FullHttpRequest fullHttpRequest) {
        this.id = UUID.randomUUID().toString();
        this.uniqueId = uniqueId;
        this.beginTime = TimeUtil.currentTimeMillis();
        this.charset = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.uri = uri;
        this.method = method;
        this.contentType = contentType;
        this.headers = headers;
        this.fullHttpRequest = fullHttpRequest;
        this.queryStringDecoder = new QueryStringDecoder(uri, charset);
        this.path = queryStringDecoder.path();
        this.modifyHost = host;
        this.modifyPath = path;

        this.modifyScheme = BasicConst.HTTP_PREFIX_SEPARATOR;
        this.requestBuilder = new RequestBuilder();
        this.requestBuilder.setMethod(getMethod().name());
        this.requestBuilder.setHeaders(getHeaders());
        this.requestBuilder.setQueryParams(queryStringDecoder.parameters());

        ByteBuf contentBuffer = fullHttpRequest.content();
        if (Objects.nonNull(contentBuffer)) {
            this.requestBuilder.setBody(contentBuffer.nioBuffer());
        }
    }

    /**
     * 获取请求体
     */
    public String getBody() {
        if (StringUtils.isEmpty(body)) {
            body = fullHttpRequest.content().toString(charset);
        }
        return body;
    }

    /**
     * 获取Cookie
     *
     * @param name
     * @return
     */
    public io.netty.handler.codec.http.cookie.Cookie getCookie(String name) {
        if (cookieMap == null) {
            cookieMap = new HashMap<String, Cookie>();
            String cookieStr = getHeaders().get(HttpHeaderNames.COOKIE);
            if (StringUtils.isBlank(cookieStr)) {
                throw new ResponseException(ResponseCode.UNAUTHORIZED);
            }
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieStr);
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                cookieMap.put(name, cookie);
            }
        }
        return cookieMap.get(name);
    }

    /**
     * 获取指定名词参数值
     *
     * @param name
     * @return
     */
    public List<String> getQueryParametersMultiple(String name) {
        return queryStringDecoder.parameters().get(name);
    }

    /**
     * post请求获取指定名词参数值
     *
     * @param name
     * @return
     */
    public List<String> getPostParametersMultiples(String name) {
        String body = getBody();
        if (isFormPost()) {
            if (postParameters == null) {
                QueryStringDecoder paramDecoder = new QueryStringDecoder(body, false);
                postParameters = paramDecoder.parameters();
            }
            if (postParameters == null || postParameters.isEmpty()) {
                return null;
            } else {
                return postParameters.get(name);
            }
        } else if (isJsonPost()) {
            try {
                return Lists.newArrayList(JsonPath.read(body, name).toString());
            } catch (Exception e) {
                log.error("JsonPath解析失败，JsonPath:{},Body:{},", name, body, e);
            }
        }
        return null;
    }


    @Override
    public void setModifyHost(String modifyHost) {
        this.modifyHost = modifyHost;
    }

    @Override
    public String getModifyHost() {
        return modifyHost;
    }

    @Override
    public void setModifyPath(String modifyPath) {
        this.modifyPath = modifyPath;
    }

    @Override
    public String getModifyPath() {
        return modifyPath;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        requestBuilder.addHeader(name, value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        requestBuilder.setHeader(name, value);
    }

    @Override
    public void addQueryParam(String name, String value) {
        requestBuilder.addQueryParam(name, value);
    }

    @Override
    public void addFormParam(String name, String value) {
        if (isFormPost()) {
            requestBuilder.addFormParam(name, value);
        }
    }

    @Override
    public void addOrReplaceCookie(org.asynchttpclient.cookie.Cookie cookie) {
        requestBuilder.addOrReplaceCookie(cookie);
    }

    @Override
    public void setRequestTimeout(int requestTimeout) {
        requestBuilder.setRequestTimeout(requestTimeout);
    }

    @Override
    public String getFinalUrl() {
        return modifyScheme + modifyHost + modifyPath;
    }

    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalUrl());
        requestBuilder.addHeader("userId", String.valueOf(userId));
        return requestBuilder.build();
    }

    /**
     * 判断当前请求是否为表单提交的HTTP POST请求
     *
     * @return 如果是表单提交的POST请求，则返回true；否则返回false
     * 表单提交的POST请求需要满足以下条件：
     * 1. 请求方法为POST
     * 2. 内容类型为表单数据（form-data）或应用/x-www-form-urlencoded
     * <p>
     * multipart/form-data：表示数据是多部分表单数据，用于文件上传或发送多部分信息。
     * application/x-www-form-urlencoded：表示数据是表单数据，通常用于 HTML 表单的 POST 请求
     */
    public boolean isFormPost() {
        return HttpMethod.POST.equals(method) &&
                (contentType.startsWith(HttpHeaderValues.FORM_DATA.toString()) ||
                        contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()));
    }

    public boolean isJsonPost() {
        //application/json：表示数据是 JSON 格式的文本，常用于 RESTful API 交互
        return HttpMethod.POST.equals(method) && contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString());
    }


}
