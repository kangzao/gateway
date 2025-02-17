package com.jep.gateway.core.filter.router;


import com.jep.gateway.common.config.Rule;
import com.jep.gateway.common.enums.ResponseCode;
import com.jep.gateway.common.exception.ConnectException;
import com.jep.gateway.common.exception.ResponseException;
import com.jep.gateway.core.config.ConfigLoader;
import com.jep.gateway.core.context.ContextStatus;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.Filter;
import com.jep.gateway.core.filter.annotation.FilterAspect;
import com.jep.gateway.core.helper.AsyncHttpHelper;
import com.jep.gateway.core.helper.ResponseHelper;
import com.jep.gateway.core.response.GatewayResponse;
import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jep.gateway.common.constant.FilterConst.*;

/**
 * @author enping.jep
 * @date 2025/2/17 22:48
 **/
@Slf4j
@FilterAspect(id = ROUTER_FILTER_ID, name = ROUTER_FILTER_NAME, order = ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {

    private static final Logger accessLog = LoggerFactory.getLogger("accessLog");

    /**
     * 执行过滤器
     */
    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
        //首先获取熔断降级的配置
        Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(gatewayContext);
        //如果存在对应配置就走熔断降级的逻辑
        if (hystrixConfig.isPresent()) {
            routeWithHystrix(gatewayContext, hystrixConfig);
        } else {
            route(gatewayContext, hystrixConfig);
        }

    }

    /**
     * 获取熔断降级 hystrix 的配置
     * 对比请求路径和注册中心注册的路径参数，判断当前请求是否需要走熔断策略分支
     */
    private static Optional<Rule.HystrixConfig> getHystrixConfig(GatewayContext gatewayContext) {
        Rule rule = gatewayContext.getRule();

        return rule.getHystrixConfigs().stream().filter(config -> StringUtils.equals(config.getPath(), gatewayContext.getRequest().getPath())).findFirst();
    }

    /**
     * 默认路由逻辑：
     * 根据 whenComplete 判断执行回调的线程是否阻塞执行
     * whenComplete 		当异步操作完成时（无论成功还是失败），会立即执行回调函数
     * whenCompleteAsync 	当异步操作完成时，会创建一个新的异步任务来执行回调函数
     */
    private CompletableFuture<Response> route(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        log.info("request id : {}", gatewayContext.getRequest().getId());
        // 执行 HTTP 请求，并返回一个 CompletableFuture 对象
        Request request = gatewayContext.getRequest().build();
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);

        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();

        // 单异步/双异步模型
        if (whenComplete) {
            future.whenComplete(new BiConsumer<Response, Throwable>() {
                @Override
                public void accept(Response response, Throwable throwable) {
                    complete(request, response, throwable, gatewayContext, hystrixConfig);
                }
            });
        } else {
            future.whenCompleteAsync(new BiConsumer<Response, Throwable>() {
                @Override
                public void accept(Response response, Throwable throwable) {
                    complete(request, response, throwable, gatewayContext, hystrixConfig);
                }
            });
        }
        return future;
    }

    /**
     * 熔断降级请求策略：
     * 1.命令执行超过配置超时时间；
     * 2.命令执行出现异常或错误；
     * 3.连续失败率达到配置的阈值；
     */
    private void routeWithHystrix(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(gatewayContext.getUniqueId())).andCommandKey(HystrixCommandKey.Factory.asKey(gatewayContext.getRequest().getPath()))
                //核心线程数
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(hystrixConfig.get().getCoreThreadSize()))
                //线程隔离类型
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        // 命令执行超时
                        .withExecutionTimeoutInMilliseconds(hystrixConfig.get().getTimeoutInMilliseconds())
                        // 超时中断
                        .withExecutionIsolationThreadInterruptOnTimeout(true)
                        //启用执行超时功能
                        .withExecutionTimeoutEnabled(true));

        new HystrixCommand<Object>(setter) {
            @Override
            protected Object run() throws Exception {
                route(gatewayContext, hystrixConfig).get();
                return null;
            }

            @Override
            protected Object getFallback() {
                Exception exception = (Exception) getExecutionException();
                //打印异常信息
                if (null != exception) {
                    log.error("通过hystrix {}", exception.getMessage());
                }
                gatewayContext.setThrowable(new ResponseException(ResponseCode.HYSTRIX_PROTECTION));
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HYSTRIX_PROTECTION));
                gatewayContext.setContextStatus(ContextStatus.Written);
                ResponseHelper.writeResponse(gatewayContext);
                return null;
            }
        }.execute();
    }

    /**
     * 响应回调处理
     */
    private void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        // 请求已经处理完毕 释放请求资源
        gatewayContext.releaseRequest();

        // 获取上下文请求配置规则
        Rule rule = gatewayContext.getRule();

        // 获取已经重试调用的次数
        int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
        //配置的调用次数
        int confRetryTimes = rule.getRetryConfig().getTimes();

        // 异常发生后进行重试
        if ((throwable instanceof TimeoutException || throwable instanceof IOException) && currentRetryTimes < confRetryTimes && hystrixConfig.isEmpty()) {
            doRetry(gatewayContext, currentRetryTimes);
            return;
        }

        // 处理响应
        handleResponse(request, response, throwable, gatewayContext);
    }

    /**
     * 处理HTTP响应
     */
    private void handleResponse(Request request, Response response, Throwable throwable, GatewayContext gatewayContext) {
        String url = request.getUrl();
        String reqId = gatewayContext.getUniqueId();

        try {
            if (Objects.nonNull(throwable)) {
                // 如果是超时异常
                if (throwable instanceof TimeoutException) {
                    log.warn("handleResponse TimeoutException---complete timeout {} reqId : {}", url, reqId);

                    gatewayContext.setThrowable(throwable);
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                } else if (throwable instanceof IOException) {
                    log.warn("handleResponse IOException---complete io exception {} reqId : {}", url, reqId);

                    gatewayContext.setThrowable(new ConnectException(throwable, gatewayContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Exception e) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            log.error("complete process failed", e);
        } finally {
            gatewayContext.setContextStatus(ContextStatus.Written);
            ResponseHelper.writeResponse(gatewayContext);
            accessLog.info("{} {} {} {} {} {}", System.currentTimeMillis() - gatewayContext.getRequest().getBeginTime(), gatewayContext.getRequest().getClientIp(), gatewayContext.getRequest().getUniqueId(), gatewayContext.getRequest().getMethod(), gatewayContext.getRequest().getPath(), gatewayContext.getResponse().getHttpResponseStatus().code(), gatewayContext.getResponse().getFutureResponse().getResponseBodyAsBytes().length);
        }
    }

    /**
     * 重试策略
     */
    private void doRetry(GatewayContext gatewayContext, int retryTimes) {
        gatewayContext.setCurrentRetryTimes(retryTimes + 1);

        log.info("当前请求重试次数为{}", gatewayContext.getCurrentRetryTimes());

        try {
            // 重新执行过滤器逻辑
            doFilter(gatewayContext);
        } catch (Exception e) {
            log.warn("重试请求失败, requestId={}", gatewayContext.getUniqueId(), e);
            throw new RuntimeException(e);
        }
    }

}
