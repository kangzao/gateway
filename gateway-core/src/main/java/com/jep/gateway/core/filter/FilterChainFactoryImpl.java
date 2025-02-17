package com.jep.gateway.core.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jep.gateway.common.config.Rule;
import com.jep.gateway.common.constant.FilterConst;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.annotation.FilterAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 过滤器工厂具体实现类
 * 1、根据SPI 动态加载驱动实现的过滤器类对象，并存储到本地内存；
 * 2、根据注册中心配置的规则策略，加载实时可用的过滤器，组装为网关过滤器链。
 *
 * @author enping.jep
 * @date 2025/2/10 22:46
 **/
@Slf4j
public class FilterChainFactoryImpl implements FilterChainFactory {

    /**
     * 单例模式
     */
    private static class SingletonInstance {
        private static final FilterChainFactoryImpl INSTANCE = new FilterChainFactoryImpl();
    }

    /**
     * 饿汉式获取单例对象
     */
    public static FilterChainFactoryImpl getInstance() {
        return SingletonInstance.INSTANCE;
    }

    /**
     * 过滤器链缓存（服务ID ——> 过滤器链）
     * ruleId —— GatewayFilterChain
     */
    private final Cache<String, FilterChain> chainCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build();


    private final Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    /**
     * SPI加载本地过滤器实现类对象
     * 过滤器存储映射 过滤器id - 过滤器对象
     */
    public FilterChainFactoryImpl() {
        //使用 ServiceLoader 加载实现了 Filter 接口的所有类。这是Java服务提供者机制的一部分，允许在运行时动态发现服务提供者。
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        //将 ServiceLoader 转换为流（Stream），然后对每个服务提供者进行操作
        serviceLoader.stream().forEach(filterProvider -> {
            Filter filter = filterProvider.get();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            log.info("load filter success:{},{},{},{}", filter.getClass(), annotation.id(), annotation.name(), annotation.order());
            //添加到过滤集合
            String filterId = annotation.id();
            if (StringUtils.isEmpty(filterId)) {
                filterId = filter.getClass().getName();
            }
            processorFilterIdMap.put(filterId, filter);
        });

    }


    @Override
    public FilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        // 获取规则ID
        String ruleId = ctx.getRule().getId();

        // 从缓存中获取过滤器链
        FilterChain chain = chainCache.getIfPresent(ruleId);

        // 如果缓存中没有过滤器链，那么构建一个新的过滤器链
        if (chain == null) {
            chain = doBuildFilterChain(ctx.getRule());
            // 将新构建的过滤器链添加到缓存中
            chainCache.put(ruleId, chain);
        }

        // 返回过滤器链
        return chain;
    }


    public FilterChain doBuildFilterChain(Rule rule) {
        FilterChain chain = new FilterChain();
        List<Filter> filters = new ArrayList<>();

        //监控相关的filter
        filters.add(getFilterInfo(FilterConst.MONITOR_FILTER_ID));
//        filters.add(getFilterInfo(FilterConst.MONITOR_END_FILTER_ID));
        if (rule != null) {
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while (iterator.hasNext()) {
                filterConfig = (Rule.FilterConfig) iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterId = filterConfig.getId();
                if (StringUtils.isNotEmpty(filterId) && getFilterInfo(filterId) != null) {
                    Filter filter = getFilterInfo(filterId);
                    filters.add(filter);
                }
            }
        }
        //每个服务请求最终最后需要添加路由过滤器
        filters.add(getFilterInfo(FilterConst.ROUTER_FILTER_ID));
        //排序  如果filters中有null值，就会报错
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        //添加到链表中
        chain.addFilterList(filters);
        return chain;
    }

    @Override
    public Filter getFilterInfo(String filterId) {
        return processorFilterIdMap.get(filterId);
    }
}
