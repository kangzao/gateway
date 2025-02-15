package com.jep.gateway.example.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 和
 * @author enping.jep
 * @date 2025/2/13 21:32
 **/
@Component
@ConfigurationProperties(prefix = "app") // 绑定前缀为 "app" 的属性
@Data
public class AppProperties {
    private String name;
    private String version;
    private String description;
}
