package com.jep.gateway.register.impl;

import com.alibaba.nacos.api.naming.NamingService;
import com.jep.gateway.common.config.ServiceDefinition;
import com.jep.gateway.common.config.ServiceInstance;
import com.jep.gateway.common.constant.GatewayConst;
import com.jep.gateway.register.RegisterCenter;
import com.jep.gateway.register.RegisterCenterListener;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.naming.listener.EventListener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * nacos注册中心
 * @author enping.jep
 * @date 2025/1/29 16:52
 **/
@Slf4j
public class NacosRegisterCenter implements RegisterCenter {
    //注册地址
    private String registerAddress;

    //环境
    private String env;

    //NamingService在服务启动和调用过程中使用，是服务发现和通信的核心。
    private NamingService namingService;

    //NamingMaintainService在服务运行期间使用，用于维护服务实例的状态
    private NamingMaintainService namingMaintainService;

    //监听器列表
    private final List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();

    /**
     * 初始化方法
     * @param registerAddress 注册中心地址，用于配置和服务注册
     * @param env             环境信息，如开发、测试或生产环境
     * @throws RuntimeException 如果在创建命名服务维护或命名服务对象时发生Nacos异常，将抛出运行时异常
     */
    @Override
    public void init(String registerAddress, String env) {
        // 配置注册中心地址和环境信息
        this.registerAddress = registerAddress;
        this.env = env;

        try {
            // NamingService 是 Nacos 提供的核心服务接口，主要用于服务注册、服务发现、服务订阅等功能
            this.namingService = NamingFactory.createNamingService(registerAddress);
            // NamingMaintainService 是一个用于维护服务实例元数据的接口
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
        } catch (NacosException e) {
            // 将Nacos异常转换为运行时异常抛出，因为Nacos异常是检查型异常，无法直接抛出
            throw new RuntimeException(e);
        }
    }


    /**
     * 注册服务实例
     * 此方法的主要作用是将服务实例注册到服务治理平台（如Nacos）上，以便服务发现和管理
     * 它首先构造一个表示服务实例的nacosInstance对象，然后使用namingService对象的registerInstance方法进行注册
     * 同时，它还会更新对应的服务定义，最后记录注册操作的日志
     *
     * @param serviceDefinition 服务定义，包含服务ID等信息
     * @param serviceInstance   服务实例，包含实例ID、端口和IP等信息
     */
    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            // 构造nacos实例信息
            Instance nacosInstance = new Instance();
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setPort(serviceInstance.getPort());
            nacosInstance.setIp(serviceInstance.getIp());
            // 实例信息可以放入到metadata中
            nacosInstance.setMetadata(Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceInstance)));

            /**
             * 在 Nacos 2.1.0 版本中，NamingService 接口的 registerInstance 方法用于将服务实例注册到 Nacos 服务注册中心。
             */
            namingService.registerInstance(serviceDefinition.getServiceId(), env, nacosInstance);

            /**
             *  Nacos 服务注册与发现组件中的一个方法，用于更新服务的相关信息。
             *  namespaceId：命名空间ID，用于隔离不同环境的配置信息。
             *  serviceName：服务名称，用于标识服务。
             *  protectThreshold：保护阈值，用于控制服务的流量保护。
             *  metadata：服务的元数据信息，可以包含多个键值对。
             */
            namingMaintainService.updateService(serviceDefinition.getServiceId(), env, 0, Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition)));

            log.info("register {} {}", serviceDefinition, serviceInstance);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            namingService.deregisterInstance(serviceDefinition.getServiceId(), env, serviceInstance.getIp(), serviceInstance.getPort());
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 订阅所有服务
     * 此方法用于向注册中心请求所有已知服务的订阅，并设置定时任务以检查新服务的加入
     *
     * @param registerCenterListener 注册中心监听器，用于监听服务变化
     */
    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        // 将传入的注册中心监听器添加到监听器列表中
        registerCenterListenerList.add(registerCenterListener);

        // 执行订阅所有服务的操作
        doSubscribeAllServices();

        // 可能有新服务加入，所以需要有一个定时任务来检查
        // 创建一个名为“doSubscribeAllServices”的单线程定时任务执行器
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1, new NameThreadFactory("doSubscribeAllServices"));

        // 设置定时任务，每10秒执行一次订阅所有服务的操作
//        scheduledThreadPool.scheduleWithFixedDelay(this::doSubscribeAllServices, 10, 10, TimeUnit.SECONDS);
    }


    /**
     * 订阅所有服务
     * 此方法旨在订阅Nacos注册中心中所有未被订阅的服务
     * 它通过分页查询服务列表，对每个未订阅的服务设置订阅，并通过事件监听器监听服务变化
     */
    private void doSubscribeAllServices() {
        try {
            //拿到当前服务已经订阅的服务
            Set<String> subscribeServiceSet = namingService.getSubscribeServices().stream().map(ServiceInfo::getName).collect(Collectors.toSet());
            int pageNo = 1;
            int pageSize = 100;
            //分页从nacos拿到服务列表
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, env).getData();
            while (CollectionUtils.isNotEmpty(serviceList)) {
                log.info("service list size {}", serviceList.size());
                for (String service : serviceList) {
                    //判断是否已经订阅了当前服务
                    if (subscribeServiceSet.contains(service)) {
                        continue;
                    }
                    //nacos 事件监听器 订阅当前服务
                    //这里需要自己实现一个 nacos 的事件订阅类 来具体执行订阅执行时的操作
                    EventListener eventListener = new NacosRegisterListener();
                    //当前服务之前不存在 调用监听器方法进行添加处理
                    eventListener.onEvent(new NamingEvent(service, null));
                    // 订阅指定运行环境下对应的服务名，注册中心服务发生变动时调用 onEvent() 方法更新本地缓存的服务信息
                    namingService.subscribe(service, env, eventListener);
                    log.info("subscribe a service, serviceName {} env {}", service, env);
                }

                //遍历下一页的服务列表
                serviceList = namingService.getServicesOfServer(++pageNo, pageSize, env).getData();
            }

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 实现对 nacos 事件的监听器 这个事件监听器会在 nacos 发生事件变化的时候进行回调
     * NamingEvent 是一个事件对象，用于表示与服务命名空间（Naming）相关的事件。
     * NamingEvent 的作用是用于监听和处理命名空间中的服务实例（Service Instance）的变化，
     * 以便应用程序可以根据这些变化来动态地更新服务实例列表，以保持与注册中心的同步。
     */
    public class NacosRegisterListener implements EventListener {
        @Override
        public void onEvent(Event event) {
            // 检查事件是否为注册中心事件
            if (event instanceof NamingEvent namingEvent) {
                log.info("nacos event {}", namingEvent);
                // 获取变更服务名称
                String serviceName = namingEvent.getServiceName();
                try {
                    // 通过服务名称获取服务定义信息
                    Service service = namingMaintainService.queryService(serviceName, env);
                    // 解析服务元数据中的服务定义
                    ServiceDefinition serviceDefinition = JSON.parseObject(service.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);
                    // 获取所有服务实例
                    List<Instance> allInstances = namingService.getAllInstances(service.getName(), env);
                    // 创建一个集合存储服务实例
                    List<ServiceInstance> list = new ArrayList<>();

                    // 遍历所有实例，解析元数据中的服务实例信息并添加到集合中
                    for (Instance instance : allInstances) {
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        list.add(serviceInstance);
                    }

                    //调用订阅监听器接口
                    registerCenterListenerList.forEach(registerCenterListener -> {
                        registerCenterListener.onChange(serviceDefinition, list);
                    });
                } catch (NacosException e) {
                    // 处理Nacos异常
                    throw new RuntimeException(e);
                }
            }
        }

    }
}
