package com.get.zh_picture_backend.common;

import com.get.zh_picture_backend.model.enums.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    public BaseResponse(int code,T data,String message){
        this.code=code;
        this.data=data;
        this.message=message;
    }
    public BaseResponse(int code,T data){
        this(code,data,"");
    }
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
