package com.get.zh_picture_backend.manager.SendSms;

import lombok.Data;

import java.io.Serializable;

@Data
public class SendSmsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private String bizld;
    private String requestId;
}
