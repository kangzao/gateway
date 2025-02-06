package com.jep.http.server.controller;

import com.jep.gateway.client.ApiInvoker;
import com.jep.gateway.client.ApiProperties;
import com.jep.gateway.client.ApiProtocol;
import com.jep.gateway.client.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author enping.jep
 * @date 2025/1/28 18:20
 **/
@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {

    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() throws InterruptedException {
//        Thread.sleep(2 * 1000); //模拟请求超时的时候打开
        Thread.sleep(200);
        log.info("调用成功");
        return "pong";
    }

    @ApiInvoker(path = "/http-server/ping1")
    @GetMapping("/http-server/ping1")
    public String ping1() {
        return "pong1";
    }

    @ApiInvoker(path = "/http-server/ping2")
    @GetMapping("/http-server/ping2")
    public String ping2() {
        return "pong2";
    }
}
