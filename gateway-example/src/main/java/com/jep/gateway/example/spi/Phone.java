package com.jep.gateway.example.spi;

/**
 * @author enping.jep
 * @date 2025/2/1 21:44
 **/
public class Phone implements Device {

    @Override
    public void say() {
        System.out.println("Hello, I am Phone");
    }
}
