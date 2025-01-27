package com.jep.gateway.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 一个服务定义会对应多个服务实例
 *
 * @author enping.jep
 * @date 2025/1/27 21:23
 **/
@ToString
@Getter
@Setter
public class ServiceInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = -7559569289189228478L;

    /**
     * 服务实例ID: ip:port
     */
    protected String serviceInstanceId;

    /**
     * 服务定义唯一id： uniqueId
     */
    protected String uniqueId;

    /**
     * 服务实例地址： ip:port
     */
    protected String ip;

    protected int port;

    /**
     * 标签信息
     */
    protected String tags;

    /**
     * 权重信息
     */
    protected Integer weight;

    /**
     * 服务注册的时间戳：后面我们做负载均衡，warmup预热
     */
    protected long registerTime;

    /**
     * 服务实例启用禁用
     */
    protected boolean enable = true;

    /**
     * 服务实例对应的版本号
     */
    protected String version;

    private boolean gray;

    public ServiceInstance() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        ServiceInstance serviceInstance = (ServiceInstance) o;
        return Objects.equals(serviceInstanceId, serviceInstance.serviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceInstanceId);
    }

}
