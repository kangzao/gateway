package com.jep.gateway.core.filter.monitor;

import com.jep.gateway.core.context.GatewayContext;
import com.jep.gateway.core.filter.Filter;
import com.jep.gateway.core.filter.annotation.FilterAspect;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import static com.jep.gateway.common.constant.FilterConst.*;

/**
 * @author enping.jep
 * @date 2025/2/17 22:05
 **/
@Slf4j
@FilterAspect(id = MONITOR_FILTER_ID, name = MONITOR_FILTER_NAME, order = MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {

    /**
     * 执行过滤器
     */
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        ctx.setTimerSample(Timer.start());
    }
}
