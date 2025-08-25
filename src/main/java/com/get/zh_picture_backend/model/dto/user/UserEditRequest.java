package com.get.zh_picture_backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户头像
     */
    private String userAvatar;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

}
