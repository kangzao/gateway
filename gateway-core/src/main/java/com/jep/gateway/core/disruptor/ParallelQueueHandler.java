package com.jep.gateway.core.disruptor;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于disruptor实现多生产者多消费者，无锁队列的处理类
 * ParallelQueueHandler 类实现了 ParallelQueue 接口，用于处理并行任务。
 * 它使用 Disruptor 框架来实现高性能的并发处理。
 *
 * @author enping.jep
 * @date 2025/1/27 22:22
 **/
public class ParallelQueueHandler<E> implements ParallelQueue<E> {

    // 环形缓冲区，用于存储事件
    private RingBuffer<Holder> ringBuffer;

    // 事件监听器，用于处理事件
    private EventListener<E> eventListener;

    // 工作池，用于管理工作线程
    private WorkerPool<Holder> workerPool;

    // 线程池，用于执行任务
    private ExecutorService executorService;

    // 事件翻译器，用于将事件数据翻译到环形缓冲区的事件中
    private EventTranslatorOneArg<Holder, E> eventTranslator;

    /**
     * 构造函数，初始化 ParallelQueueHandler。
     *
     * @param builder 构建器，用于配置 ParallelQueueHandler
     */
    public ParallelQueueHandler(Builder<E> builder) {
        // 创建线程池
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                new ThreadFactoryBuilder().setNameFormat("ParallelQueueHandler" + builder.namePrefix + "-pool-%d").build());

        // 设置事件监听器
        this.eventListener = builder.listener;
        this.eventTranslator = new HolderEventTranslator();

        // 创建环形缓冲区
        RingBuffer<Holder> ringBuffer = RingBuffer.create(builder.producerType,
                new HolderEventFactory(),
                builder.bufferSize,
                builder.waitStrategy);

        // 创建屏障，用于同步生产者和消费者
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        // 创建工作处理器数组
        WorkHandler<Holder>[] workHandlers = new WorkHandler[builder.threads];
        for (int i = 0; i < workHandlers.length; i++) {
            workHandlers[i] = new HolderWorkHandler();
        }

        //创建多消费者线程池
        WorkerPool<Holder> workerPool = new WorkerPool<>(ringBuffer,
                sequenceBarrier,
                new HolderExceptionHandler(),
                workHandlers);
        //设置多消费者的Sequence序号，主要用于统计消费进度，
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        this.workerPool = workerPool;
    }

    /**
     * 添加单个事件到环形缓冲区。
     *
     * @param event 要添加的事件
     */
    @Override
    public void add(E event) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if (holderRing == null) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is close"), event);
        }
        try {
            ringBuffer.publishEvent(this.eventTranslator, event);
        } catch (NullPointerException e) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is close"), event);
        }
    }

    /**
     * 添加多个事件到环形缓冲区。
     *
     * @param events 要添加的事件数组
     */
    @Override
    public void add(E... events) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if (holderRing == null) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is close"), events);
        }
        try {
            ringBuffer.publishEvents(this.eventTranslator, events);
        } catch (NullPointerException e) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is close"), events);
        }
    }

    /**
     * 尝试添加单个事件到环形缓冲区。
     *
     * @param event 要添加的事件
     * @return 是否添加成功
     */
    @Override
    public boolean tryAdd(E event) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if (holderRing == null) {
            return false;
        }
        try {
            return ringBuffer.tryPublishEvent(this.eventTranslator, event);
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * 尝试添加多个事件到环形缓冲区。
     *
     * @param events 要添加的事件数组
     * @return 是否添加成功
     */
    @Override
    public boolean tryAdd(E... events) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if (holderRing == null) {
            return false;
        }
        try {
            return ringBuffer.tryPublishEvents(this.eventTranslator, events);
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * 启动并行队列处理器。
     */
    @Override
    public void start() {
        this.ringBuffer = workerPool.start(executorService);
    }

    /**
     * 关闭并行队列处理器。
     */
    @Override
    public void shutDown() {
        RingBuffer<Holder> holder = ringBuffer;
        ringBuffer = null;
        if (holder == null) {
            return;
        }
        if (workerPool != null) {
            workerPool.drainAndHalt();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * 检查并行队列处理器是否已关闭。
     *
     * @return 是否已关闭
     */
    @Override
    public boolean isShutDown() {
        return ringBuffer == null;
    }

    /**
     * 处理事件监听器的异常。
     *
     * @param listener 事件监听器
     * @param e        异常对象
     * @param event    事件对象
     */
    private static <E> void process(EventListener<E> listener, Throwable e, E event) {
        listener.onException(e, -1, event);
    }

    /**
     * 处理事件监听器的异常。
     *
     * @param listener 事件监听器
     * @param e        异常对象
     * @param events   事件对象数组
     */
    private static <E> void process(EventListener<E> listener, Throwable e, E... events) {
        for (E event : events) {
            process(listener, e, event);
        }
    }

    /**
     * 构建器类，用于配置并创建 ParallelQueueHandler 实例。
     */
    public static class Builder<E> {

        private ProducerType producerType = ProducerType.MULTI;

        private int bufferSize = 1024 * 16;

        private int threads = 1;

        private String namePrefix = "";

        private WaitStrategy waitStrategy = new BlockingWaitStrategy();

        private EventListener<E> listener;


        public Builder<E> setProducerType(ProducerType producerType) {
            Preconditions.checkNotNull(producerType);
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            Preconditions.checkArgument(Integer.bitCount(bufferSize) == 1);
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            Preconditions.checkArgument(threads > 0);
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            Preconditions.checkNotNull(namePrefix);
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            Preconditions.checkNotNull(waitStrategy);
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setListener(EventListener<E> listener) {
            Preconditions.checkNotNull(listener);
            this.listener = listener;
            return this;
        }

        public ParallelQueueHandler<E> build() {
            return new ParallelQueueHandler<>(this);
        }
    }


    public class Holder {
        private E event;

        public void setValue(E event) {
            this.event = event;
        }

        @Override
        public String toString() {
            return "Holder{" +
                    "event=" + event +
                    '}';
        }
    }

    /**
     * 异常处理器，用于处理环形缓冲区中的事件异常。
     */
    private class HolderExceptionHandler implements ExceptionHandler<Holder> {

        @Override
        public void handleEventException(Throwable throwable, long l, Holder event) {
            Holder holder = (Holder) event;
            try {
                eventListener.onException(throwable, l, holder.event);
            } catch (Exception e) {

            } finally {
                holder.setValue(null);
            }

        }

        @Override
        public void handleOnStartException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }

        @Override
        public void handleOnShutdownException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }
    }


    private class HolderWorkHandler implements WorkHandler<Holder> {
        @Override
        public void onEvent(Holder holder) throws Exception {
            eventListener.onEvent(holder.event);
            holder.setValue(null);
        }
    }

    /**
     * 事件工厂，用于创建新的事件对象。
     */
    private class HolderEventFactory implements EventFactory<Holder> {

        @Override
        public Holder newInstance() {
            return new Holder();
        }
    }

    /**
     * 事件翻译器，用于将事件数据翻译到环形缓冲区的事件中。
     */
    private class HolderEventTranslator implements EventTranslatorOneArg<Holder, E> {
        @Override
        public void translateTo(Holder holder, long l, E e) {
            holder.setValue(e);
        }
    }
}
