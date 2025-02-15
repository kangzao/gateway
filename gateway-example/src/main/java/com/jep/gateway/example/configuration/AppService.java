package com.jep.gateway.example.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  注入AppProperties
 * @author enping.jep
 * @date 2025/2/13 21:44
 **/
@Service
public class AppService {

    private final AppProperties appProperties;

    @Autowired
    public AppService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void printAppInfo() {
        System.out.println("App Name: " + appProperties.getName());
        System.out.println("App Version: " + appProperties.getVersion());
        System.out.println("App Description: " + appProperties.getDescription());
    }
}
