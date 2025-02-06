package com.jep.gateway.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ConfigurationProperties 注解用于将外部配置（如 YAML 或 properties 文件中的配置）
 * 绑定到一个 POJO（Plain Old Java Object）上
 *
 * @author enping.jep
 * @date 2025/1/28 11:26
 **/
@Data
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    private String registerAddress;

    private String env = "dev";

    private boolean gray;
}
