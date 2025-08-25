package com.get.zh_picture_backend.manager.SendSms;

import lombok.Data;

import java.io.Serializable;

@Data
public class SendSmsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String phoneNumbers;

    private String signName;

    private String templateCode;
}
