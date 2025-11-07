package com.jep.gateway.example.disruptor.compare;

import java.util.concurrent.ArrayBlockingQueue;

import static com.jep.gateway.example.disruptor.compare.Constants.EVENT_NUM_OHM;

public class ArrayBlockingQueue4Test {


    public static void main(String[] args) {
        // 创建一个容量为100000000的ArrayBlockingQueue队列实例
        final ArrayBlockingQueue<Data> queue = new ArrayBlockingQueue<Data>(EVENT_NUM_OHM);
        // 记录程序开始执行的时间戳
        final long startTime = System.currentTimeMillis();

        // 启动生产者线程，向队列中添加元素
        new Thread(new Runnable() {
            public void run() {
                long i = 0;
                // 循环添加Constants.EVENT_NUM_OHM个数据项到队列
                while (i < EVENT_NUM_OHM) {
                    // 创建Data对象实例
                    Data data = new Data(i, "c" + i);
                    try {
                        // 将数据放入队列，如果队列满则阻塞等待
                        queue.put(data);
                    } catch (InterruptedException e) {
                        // 处理中断异常
                        e.printStackTrace();
                    }
                    i++;
                }
            }
        }).start();

        // 启动消费者线程，从队列中取出元素
        new Thread(new Runnable() {
            public void run() {
                int k = 0;
                // 循环从队列中取出Constants.EVENT_NUM_OHM个数据项
                while (k < EVENT_NUM_OHM) {
                    try {
                        // 从队列中取出数据，如果队列空则阻塞等待
                        queue.take();
                    } catch (InterruptedException e) {
                        // 处理中断异常
                        e.printStackTrace();
                    }
                    k++;
                }
                // 记录程序结束执行的时间戳
                long endTime = System.currentTimeMillis();
                // 输出ArrayBlockingQueue操作的总耗时
                System.out.println("ArrayBlockingQueue costTime = " + (endTime - startTime) + "ms");
            }
        }).start();
    }

}