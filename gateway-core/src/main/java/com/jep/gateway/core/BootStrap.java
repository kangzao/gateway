package com.jep.gateway.core;

import com.jep.gateway.common.config.DynamicConfigManager;
import com.jep.gateway.common.config.Rule;
import com.jep.gateway.config.ConfigCenter;
import com.jep.gateway.config.RulesChangeListener;
import com.jep.gateway.core.config.Config;
import com.jep.gateway.core.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author enping.jep
 * @date 2025/1/27 18:57
 **/
@Slf4j
public class BootStrap {

    public static void main(String[] args) {
        //加载网关核心静态配置
        Config config = ConfigLoader.getInstance().load(args);

        //插件初始化
        //配置中心管理器初始化，连接配置中心，监听配置的新增、修改、删除
        ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);

        final ConfigCenter configCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("can't found ConfigCenter impl");
            return new RuntimeException("can't found ConfigCenter impl");
        });

        //从配置中心获取数据
        configCenter.init(config.getRegistryAddress(), config.getEnv());

        configCenter.subscribeRulesChange(new RulesChangeListener() {
            @Override
            public void onRulesChange(List<Rule> rules) {
                DynamicConfigManager.getInstance().putAllRule(rules);
            }
        });

        //启动容器
        Container container = new Container(config);
        container.start();
    }

}
