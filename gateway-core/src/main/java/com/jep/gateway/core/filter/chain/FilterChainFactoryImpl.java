package com.jep.gateway.core.filter.chain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jep.gateway.common.config.Rule;
import com.jep.gateway.common.constant.FilterConst;
import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.Filter;
import com.jep.gateway.core.filter.annotation.FilterAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.ServiceLoader;

/**
 * 过滤器工厂具体实现类
 * 1、根据SPI 动态加载驱动实现的过滤器类对象，并存储到本地内存；
 * 2、根据注册中心配置的规则策略，加载实时可用的过滤器，组装为网关过滤器链。
 * @author enping.jep
 * @date 2025/1/27 22:05
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
    private final Cache<String, FilterChain> chainCache = Caffeine.newBuilder().recordStats().
            expireAfterWrite(10, TimeUnit.MINUTES).build();


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
            log.info("load filter success:{},{},{},{}", filter.getClass(),
                    annotation.id(), annotation.name(), annotation.order());
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


    /**
     * 构建过滤链
     * 根据规则配置和预设的过滤器，构建一个过滤链对象
     * 过滤链中包含了监控过滤器、根据规则配置的过滤器以及路由过滤器
     *
     * @param rule 规则对象，包含过滤器配置信息如果为null，则只构建默认的监控和路由过滤器
     * @return 返回构建好的过滤链对象
     */
    public FilterChain doBuildFilterChain(Rule rule) {
        // 创建一个新的过滤链对象
        FilterChain chain = new FilterChain();
        // 初始化一个过滤器列表
        List<Filter> filters = new ArrayList<>();

        // 添加监控相关的过滤器，用于监控服务请求的开始和结束
        filters.add(getFilterInfo(FilterConst.MONITOR_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MONITOR_END_FILTER_ID));

        // 如果规则对象不为空，则根据规则配置添加相应的过滤器到列表中
        if (rule != null) {
            // 获取规则中的过滤器配置集合
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            // 创建迭代器遍历过滤器配置集合
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            // 遍历过滤器配置集合
            while (iterator.hasNext()) {
                filterConfig = (Rule.FilterConfig) iterator.next();
                // 如果过滤器配置为空，则跳过当前循环
                if (filterConfig == null) {
                    continue;
                }
                // 获取过滤器配置中的ID
                String filterId = filterConfig.getId();
                // 如果过滤器ID不为空且对应的过滤器存在，则添加到过滤器列表中
                if (StringUtils.isNotEmpty(filterId) && getFilterInfo(filterId) != null) {
                    Filter filter = getFilterInfo(filterId);
                    filters.add(filter);
                }
            }
        }
        // 每个服务请求最终最后需要添加路由过滤器，用于确定请求的路由路径
        filters.add(getFilterInfo(FilterConst.ROUTER_FILTER_ID));
        // 对过滤器列表进行排序，根据过滤器的order值决定执行顺序
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        // 将排序后的过滤器列表添加到过滤链对象中
        chain.addFilterList(filters);
        // 返回构建好的过滤链对象
        return chain;
    }

    @Override
    public Filter getFilterInfo(String filterId) {
        return processorFilterIdMap.get(filterId);
    }
}
