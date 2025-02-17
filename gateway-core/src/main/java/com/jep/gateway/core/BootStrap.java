package com.jep.gateway.core;

import com.alibaba.fastjson.JSON;
import com.jep.gateway.common.config.DynamicConfigManager;
import com.jep.gateway.common.config.Rule;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.constant.BasicConst;
import com.jep.gateway.common.util.NetUtil;
import com.jep.gateway.common.util.TimeUtil;
import com.jep.gateway.config.ConfigCenter;
import com.jep.gateway.config.RulesChangeListener;
import com.jep.gateway.core.config.Config;
import com.jep.gateway.core.config.ConfigLoader;
import com.jep.gateway.register.RegisterCenter;
import com.jep.gateway.register.RegisterCenterListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * 启动
 *
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

        //初始化配置中心的接口  初始化com.alibaba.nacos.api.config.ConfigService
        configCenter.init(config.getRegistryAddress(), config.getEnv());

        //实际执行的是com.jep.gateway.config.impl.NacosConfigCenter的方法
        /**
         * 创建了一个新的 RulesChangeListener 匿名内部类，并重写了 onRulesChange 方法。当配置中心的规则发生变化时，
         * onRulesChange 方法会被调用，然后将新的规则放入 DynamicConfigManager 的规则映射中。
         */
        configCenter.subscribeRulesChange(new RulesChangeListener() {
            /**
             * 当规则发生变化时调用的方法
             * @param rules 新的规则列表 这些规则代表了配置的最新状态，需要被更新到动态配置管理器中
             * 此方法的作用是将接收到的新规则更新到动态配置管理器中，确保应用能够根据最新的规则进行操作
             */
            @Override
            public void onRulesChange(List<Rule> rules) {
                DynamicConfigManager.getInstance().putAllRule(rules);
            }
        });

        //启动容器
        Container container = new Container(config);
        container.start();

        //连接注册中心
        final RegisterCenter registerCenter = registerAndSubscribe(config);

        //服务停机
        //收到 kill 信号时触发，服务停机
        Runtime.getRuntime().addShutdownHook(new Thread() {

            /**
             * 服务停机
             */
            @Override
            public void run() {
                registerCenter.deregister(buildGatewayServiceDefinition(config), buildGatewayServiceInstance(config));
                container.shutdown();
            }
        });
    }


    /**
     * 服务注册和订阅服务变更信息通知, spi 方式实现服务注册
     */
    private static RegisterCenter registerAndSubscribe(Config config) {
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        final RegisterCenter registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("not found RegisterCenter impl");
            return new RuntimeException("not found RegisterCenter impl");
        });


        registerCenter.init(config.getRegistryAddress(), config.getEnv());

        //构造网关服务定义和服务实例
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);

        //注册
        registerCenter.register(serviceDefinition, serviceInstance);

        //订阅
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, List<ServiceInstance> serviceInstances) {
                log.info("refresh service and instance: {} {}", serviceDefinition.getUniqueId(), JSON.toJSON(serviceInstances));
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                //将这次变更事件影响之后的服务实例再次添加到对应的服务实例集合
                manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstances);

                //修改发生对应的服务定义
                manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
            }
        });
        return registerCenter;
    }

    /**
     * 获取服务定义信息
     * 此方法用于构建网关的服务定义，根据配置信息设置服务的基本属性
     *
     * @param config 配置对象，包含应用名称和环境类型等信息
     * @return 返回构建的服务定义对象
     */
    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        // 创建一个新的服务定义对象
        ServiceDefinition definition = new ServiceDefinition();

        // 设置服务的标识符为应用名称
        definition.setServiceId(config.getServiceId());

        // 设置环境类型，根据配置信息
        definition.setEnvType(config.getEnv());

        // 设置唯一的标识符为应用名称，确保服务的唯一性
        definition.setUniqueId(config.getServiceId());

        // 初始化调用者映射，此处为空映射，表示尚未定义任何调用者
        definition.setInvokerMap(Map.of());

        // 返回构建完成的服务定义对象
        return definition;
    }

    /**
     * 获取服务实例信息
     * 该方法用于构建当前网关服务的实例信息，包括IP地址、端口号、服务实例ID和注册时间
     *
     * @param config 系统配置对象，用于获取网关服务的端口号
     * @return 返回一个包含服务实例信息的ServiceInstance对象
     */
    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        // 获取本地IP地址
        String localIp = NetUtil.getLocalIp();
        // 从配置对象中获取服务端口号
        int port = config.getPort();

        // 创建一个新的ServiceInstance对象
        ServiceInstance instance = new ServiceInstance();
        // 设置服务实例的端口
        instance.setPort(port);
        // 设置服务实例的IP地址
        instance.setIp(localIp);
        // 生成并设置服务实例ID，格式为“IP:端口”
        instance.setServiceInstanceId(localIp + BasicConst.COLON_SEPARATOR + port);
        // 设置服务实例的注册时间，使用当前时间戳
        instance.setRegisterTime(TimeUtil.currentTimeMillis());

        // 返回构建完成的服务实例对象
        return instance;
    }

}
