package com.jep.gateway.example.spring.appcontext;

import org.springframework.stereotype.Component;

/**
 * @author enping.jep
 * @date 2025/2/17 21:22
 **/
@Component
public class OtherBean {
    public void doSomethingElse() {
        System.out.println("OtherBean is doing something else...");
    }
}
