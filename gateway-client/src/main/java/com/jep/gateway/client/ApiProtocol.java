package com.jep.gateway.client;

import lombok.Getter;

/**
 * @author enping.jep
 * @date 2025/1/28 11:30
 **/
@Getter
public enum ApiProtocol {
    HTTP("http", "http协议"), DUBBO("dubbo", "dubbo协议");

    private String code;

    private String desc;

    ApiProtocol(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
