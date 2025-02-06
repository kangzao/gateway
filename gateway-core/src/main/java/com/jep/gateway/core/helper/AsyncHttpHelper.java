package com.jep.gateway.core.helper;

import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;

/**
 * 异步的http辅助类
 * @author enping.jep
 * @date 2025/1/27 21:46
 **/
public class AsyncHttpHelper {

    private static final class SingletonHolder {
        private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
    }

    private AsyncHttpHelper() {

    }
    /**
     * 获取AsyncHttpHelper的单例实例
     *
     * @return AsyncHttpHelper的单例实例
     */
    public static AsyncHttpHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private AsyncHttpClient asyncHttpClient;

    /**
     * 初始化异步HTTP客户端
     *
     * @param asyncHttpClient 异步HTTP客户端实例，用于执行异步HTTP请求
     */
    public void initialized(AsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }

    /**
     * 使用异步HTTP客户端执行请求，并返回一个CompletableFuture对象
     * 此方法通过异步方式执行HTTP请求，避免了阻塞当前线程，适用于需要执行耗时网络操作的场景
     *
     * @param request 请求对象，包含请求的详细信息，如URL、方法、头信息等
     * @return CompletableFuture对象，表示异步计算的结果，调用者可以通过这个对象获取请求的响应结果
     */
    public CompletableFuture<Response> executeRequest(Request request) {
        // 异步执行HTTP请求，返回一个ListenableFuture对象
        ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
        // 将ListenableFuture对象转换为CompletableFuture对象并返回，以便于后续的异步处理和链式调用
        return future.toCompletableFuture();
    }

    /**
     * 使用异步HTTP客户端执行HTTP请求，并通过CompletableFuture处理响应结果
     * 该方法允许通过异步方式执行HTTP请求，并使用AsyncHandler处理响应，适用于需要异步处理HTTP响应的场景
     *
     * @param request 代表HTTP请求的对象，包含请求的URL、方法、头信息等
     * @param handler 异步处理响应的处理器，用于处理HTTP响应结果
     * @param <T>     泛型参数，表示CompletableFuture完成时的结果类型
     * @return 返回一个CompletableFuture对象，用于后续的异步处理或链式调用
     */
    public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
        // 使用异步HTTP客户端执行HTTP请求，并返回一个ListenableFuture对象
        ListenableFuture<T> future = asyncHttpClient.executeRequest(request, handler);
        // 将ListenableFuture对象转换为CompletableFuture对象，以便于进行链式调用和异步处理
        return future.toCompletableFuture();
    }
}