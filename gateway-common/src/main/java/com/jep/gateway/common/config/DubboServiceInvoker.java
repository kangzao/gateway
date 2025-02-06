package com.jep.gateway.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * dubbo协议的注册服务调用模型类
 *
 * @author enping.jep
 * @date 2025/1/28 11:36
 **/
@Getter
@Setter
public class DubboServiceInvoker extends AbstractServiceInvoker {

    //	注册中心地址
    private String registerAddress;

    //	接口全类名
    private String interfaceClass;

    //	方法名称
    private String methodName;

    //	参数名字的集合
    private String[] parameterTypes;

    //	dubbo服务的版本号
    private String version;

}
