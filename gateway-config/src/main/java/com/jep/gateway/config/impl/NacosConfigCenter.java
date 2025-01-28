package com.jep.gateway.config.impl;

import com.jep.gateway.common.config.Rule;
import com.jep.gateway.config.ConfigCenter;
import com.jep.gateway.config.RulesChangeListener;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author enping.jep
 * @date 2025/1/27 21:07
 **/
@Slf4j
public class NacosConfigCenter implements ConfigCenter {
    private static final String DATA_ID = "api-gateway";

    private String serverAddr;

    private String env;

    private ConfigService configService;

    @Override
    public void init(String serverAddr, String env) {
        this.serverAddr = serverAddr;
        this.env = env;

        try {
            //NacosFactory.createConfigService 是 Nacos 客户端 SDK 提供的一个方法，用于创建 ConfigService 实例，
            //这个实例提供了操作配置的功能，比如获取、发布、监听配置信息等
            configService = NacosFactory.createConfigService(serverAddr);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        try {
            //初始化通知
            String config = configService.getConfig(DATA_ID, env, 5000);
            //{"rules":[{}, {}]}
            log.info("config from nacos: {}", config);
            if (StringUtils.isNoneBlank(config)) {
                List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
                listener.onRulesChange(rules);//手动触发一次
            }
            //监听变化
            configService.addListener(DATA_ID, env, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("com.alibaba.nacos.api.config.ConfigService.addListener:config from nacos: {}", configInfo);
                    List<Rule> rules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
                    listener.onRulesChange(rules);
                }
            });

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }
}
