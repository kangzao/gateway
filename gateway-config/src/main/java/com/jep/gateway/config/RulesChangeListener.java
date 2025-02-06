package com.jep.gateway.config;

import com.jep.gateway.common.config.Rule;

import java.util.List;

/**
 * 规则变化处理类
 *
 * @author enping.jep
 * @date 2025/1/27 21:02
 **/
public interface RulesChangeListener {
    // 当规则发生变化时调用
    void onRulesChange(List<Rule> rules);
}
