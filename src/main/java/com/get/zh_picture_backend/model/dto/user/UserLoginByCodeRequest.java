package com.get.zh_picture_backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginByCodeRequest implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 用户输入验证码
     */
    private String code;
}
