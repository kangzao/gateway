package com.jep.gateway.common.exception;

import com.jep.gateway.common.enums.ResponseCode;

/**
 * @author enping.jep
 * @date 2025/1/27 22:42
 **/
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = -5658789202563433456L;

    public BaseException() {
    }

    protected ResponseCode code;

    public BaseException(String message, ResponseCode code) {
        super(message);
        this.code = code;
    }

    public BaseException(String message, Throwable cause, ResponseCode code) {
        super(message, cause);
        this.code = code;
    }

    public BaseException(ResponseCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public BaseException(String message, Throwable cause,
                         boolean enableSuppression, boolean writableStackTrace, ResponseCode code) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }

    public ResponseCode getCode() {
        return code;
    }

}
