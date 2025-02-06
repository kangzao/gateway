package com.jep.gateway.example.nacos;

/**
 * @author enping.jep
 * @date 2025/2/1 21:50
 **/

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

public class NacosRegisterDemo {
    public static void main(String[] args) {
        try {
            // 设置Nacos地址
            String serverAddr = "127.0.0.1:8848";
            // 创建命名服务实例，用于服务注册
            NamingService namingService = NacosFactory.createNamingService(serverAddr);

            // 创建服务实例
            Instance instance = new Instance();
            instance.setIp("127.0.0.1"); // 服务实例IP
            instance.setPort(9999); // 服务实例端口
            instance.setServiceName("myService"); // 服务名称
            instance.setClusterName("cluster"); // 服务所在集群

            // 添加其他元数据
            instance.addMetadata("version", "1.0");
            instance.addMetadata("env", "production");

            // 注册服务
            namingService.registerInstance("myService", instance);

            System.out.println("服务注册成功");
        } catch (NacosException e) {
            // 异常处理
            e.printStackTrace();
        }
    }
}
