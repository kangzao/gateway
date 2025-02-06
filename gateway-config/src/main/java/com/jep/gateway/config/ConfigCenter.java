package com.jep.gateway.config;

/**
 * 配置中心
 *
 * @author enping.jep
 * @date 2025/1/27 21:01
 **/
public interface ConfigCenter {

    /**
     * 初始化
     */
    void init(String serverAddr, String env);

    /**
     * 订阅规则变化
     */
    void subscribeRulesChange(RulesChangeListener listener);
}
