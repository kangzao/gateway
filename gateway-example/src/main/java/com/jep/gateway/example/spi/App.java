package com.jep.gateway.example.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

public class App {
    public static void main(String[] args) {
        ServiceLoader<Device> loader = ServiceLoader.load(Device.class);
        Iterator<Device> iterator = loader.iterator();
        while (iterator.hasNext()) {
            Device device = iterator.next();
            device.say();
        }
    }
}