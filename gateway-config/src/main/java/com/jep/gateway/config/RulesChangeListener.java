package com.jep.gateway.config;

import com.jep.gateway.common.config.Rule;

import java.util.List;

/**
 * @author enping.jep
 * @date 2025/1/27 21:02
 **/
public interface RulesChangeListener {
    void onRulesChange(List<Rule> rules);
}
