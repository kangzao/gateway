package com.jep.gateway.config;

/**
 * @author enping.jep
 * @date 2025/1/27 21:01
 **/
public interface ConfigCenter {

    void init(String serverAddr, String env);

    void subscribeRulesChange(RulesChangeListener listener);
}
