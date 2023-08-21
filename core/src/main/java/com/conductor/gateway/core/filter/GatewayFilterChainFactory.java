package com.conductor.gateway.core.filter;

import com.conductor.common.config.Rule;
import com.conductor.gateway.common.constants.FilterConst;
import com.conductor.gateway.core.context.impl.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 过滤器工厂实现类
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterFactory {

    private static class SingletonInstance {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    public static GatewayFilterChainFactory getInstance() {
        return SingletonInstance.INSTANCE;
    }

    private Cache<String, GatewayFilterChain> chainCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build();


    private Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    public GatewayFilterChainFactory() {
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        Iterator<Filter> filtersIterator = serviceLoader.iterator();
        while (filtersIterator.hasNext()) {
            Filter filter = filtersIterator.next();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            log.info("load filter success:{},{},{},{}", filter.getClass(),
                    annotation.id(), annotation.name(), annotation.order());
            if (annotation != null) {
                //添加到过滤集合
                String filterId = annotation.id();
                if (StringUtils.isEmpty(filterId)) {
                    filterId = filter.getClass().getName();
                }
                processorFilterIdMap.put(filterId, filter);
            }
        }
        System.out.println(processorFilterIdMap);
    }

    public static void main(String[] args) {
        new GatewayFilterChainFactory();
    }


    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        return chainCache.get(ctx.getRule().getId(), k -> doBuildFilterChain(ctx.getRule()));
    }


    public GatewayFilterChain doBuildFilterChain(Rule rule) {
        GatewayFilterChain chain = new GatewayFilterChain();
        List<Filter> filters = new ArrayList<>();
        filters.add(getFilterInfo(FilterConst.GRAY_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MONITOR_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MONITOR_END_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MOCK_FILTER_ID));
        if (rule != null) {
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();
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
        //todo 添加路由过滤器-这是最后一步
        filters.add(getFilterInfo(FilterConst.ROUTER_FILTER_ID));
        //排序
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