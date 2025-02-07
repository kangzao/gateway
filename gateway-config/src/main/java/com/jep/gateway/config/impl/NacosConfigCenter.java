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

    /**
     * 订阅规则变化
     * 当规则发生变化时，通知监听器
     *
     * @param listener 规则变化监听器，用于处理规则变化事件
     */
    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        try {
            //初始化通知
            //从配置服务中获取初始配置信息
            String config = configService.getConfig(DATA_ID, env, 5000);
            log.info("config from nacos: {}", config);
            //如果配置信息非空，则解析规则并通知监听器 手动触发，添加到缓存中
            if (StringUtils.isNoneBlank(config)) {
                List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
                // 手动刷新 DynamicConfigManager中的规则
                listener.onRulesChange(rules);
            }
            //利用nacos的configService监听变化
            //添加配置监听器，以便在规则变化时收到通知
            configService.addListener(DATA_ID, env, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    //当接收到配置信息时，解析规则并通知监听器  规则发生变化才会触发
                    log.info("com.alibaba.nacos.api.config.ConfigService.addListener:config from nacos: {}", configInfo);
                    List<Rule> rules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
                    listener.onRulesChange(rules);
                }
            });

        } catch (NacosException e) {
            //如果发生Nacos异常，则包装成运行时异常抛出
            throw new RuntimeException(e);
        }
    }
}
