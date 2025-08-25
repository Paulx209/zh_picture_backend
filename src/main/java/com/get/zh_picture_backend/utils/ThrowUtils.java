package com.get.zh_picture_backend.utils;

import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.exception.BusinessException;

public class ThrowUtils {
    /**
     * 条件成立则抛出异常
     * @param condition
     * @param exception
     */
    public static void throwIf(boolean condition,RuntimeException exception){
        if(condition){
            throw exception;
        }
    }

    public static void throwIf(boolean condition, ErrorCode errorCode){
        throwIf(condition,new BusinessException(errorCode));
    }
    public static void throwIf(boolean condition,ErrorCode errorCode,String message){
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
