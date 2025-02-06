package com.jep.gateway.example.spi;

/**
 * @author enping.jep
 * @date 2025/2/1 21:43
 **/
public class Computer implements Device {

    @Override
    public void say() {
        System.out.println("Hello, I am Computer");
    }
}

