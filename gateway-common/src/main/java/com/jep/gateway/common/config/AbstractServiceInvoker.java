package com.jep.gateway.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @author enping.jep
 * @date 2025/1/27 22:46
 **/
@Getter
@Setter
public class AbstractServiceInvoker implements ServiceInvoker {

    protected String invokerPath;

    protected int timeout = 5000;
}
