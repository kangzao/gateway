package com.jep.gateway.config.impl;

import com.jep.gateway.config.ConfigCenter;
import com.jep.gateway.config.RulesChangeListener;

/**
 * @author enping.jep
 * @date 2025/1/27 21:07
 **/
public class NacosConfigCenter implements ConfigCenter {
    @Override
    public void init(String serverAddr, String env) {

    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {

    }
}
