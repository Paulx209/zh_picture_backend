package com.get.zh_picture_backend.exception;

import com.get.zh_picture_backend.model.enums.ErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException{
    private final Integer code;
    public BusinessException(String message,Integer code){
        super(message);
        this.code=code;
    }
    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code=errorCode.getCode();
    }
    public BusinessException(ErrorCode errorCode,String message){
        super(message);
        this.code=errorCode.getCode();
    }
}
