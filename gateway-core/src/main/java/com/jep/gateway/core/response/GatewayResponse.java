package com.jep.gateway.core.response;

import com.jep.gateway.common.enums.ResponseCode;
import lombok.Data;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jep.gateway.common.util.JSONUtil;
import io.netty.handler.codec.http.*;
import lombok.Data;
import org.asynchttpclient.Response;

/**
 * @author enping.jep
 * @date 2025/1/27 22:32
 **/
@Data
public class GatewayResponse {

    /**
     * 响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    /**
     * 额外的响应结果
     */
    private final HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();
    /**
     * 响应内容
     */
    private String content;

    /**
     * 异步返回对象
     */
    private Response futureResponse;

    /**
     * 响应返回码
     */
    private HttpResponseStatus httpResponseStatus;


    public GatewayResponse() {

    }

    /**
     * 设置响应头信息
     *
     * @param key
     * @param val
     */
    public void putHeader(CharSequence key, CharSequence val) {
        responseHeaders.add(key, val);
    }

    /**
     * 构建异步响应对象
     *
     * @param futureResponse
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Response futureResponse) {
        GatewayResponse response = new GatewayResponse();
        response.setFutureResponse(futureResponse);
        response.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return response;
    }

    /**
     * 处理返回json对象，失败时调用
     *
     * @param code
     * @return
     */
    public static GatewayResponse buildGatewayResponse(ResponseCode code) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, code.getStatus().code());
        objectNode.put(JSONUtil.CODE, code.getCode());
        objectNode.put(JSONUtil.MESSAGE, code.getMessage());

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(code.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));

        return response;
    }

    /**
     * 构建并返回一个包含成功状态的JSON对象的GatewayResponse
     * 主要用于处理成功时返回的数据封装
     *
     * @param data 要返回的数据对象
     * @return 包含成功状态和数据的GatewayResponse对象
     */
    public static GatewayResponse buildGatewayResponse(Object data) {
        // 创建一个JSON对象节点
        ObjectNode objectNode = JSONUtil.createObjectNode();
        // 设置状态码，表示操作成功
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        // 设置自定义的成功代码
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getCode());
        // 将数据对象以POJO的方式添加到JSON对象中
        objectNode.putPOJO(JSONUtil.DATA, data);

        // 创建一个GatewayResponse对象
        GatewayResponse response = new GatewayResponse();
        // 设置HTTP响应状态为成功
        response.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        // 设置响应头的内容类型为JSON格式
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        // 将JSON对象序列化为字符串并设置为响应内容
        response.setContent(JSONUtil.toJSONString(objectNode));
        // 返回构建的响应对象
        return response;
    }


}
