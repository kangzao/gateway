package com.jep.gateway.common.exception;

import com.jep.gateway.common.enums.ResponseCode;

/**
 * @author enping.jep
 * @date 2025/1/31 21:25
 **/
public class NotFoundException extends BaseException {

    private static final long serialVersionUID = -5534700534739261761L;

    public NotFoundException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public NotFoundException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }

}
