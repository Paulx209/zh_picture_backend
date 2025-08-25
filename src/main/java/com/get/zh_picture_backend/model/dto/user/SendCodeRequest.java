package com.get.zh_picture_backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class SendCodeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 待发送验证码的手机号
     */
    private String phoneNumber;

    private String codeType;
}
