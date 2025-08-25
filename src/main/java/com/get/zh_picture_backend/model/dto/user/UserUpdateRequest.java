package com.get.zh_picture_backend.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;


@Data
public class UserUpdateRequest {
    /**
     * id
     */
    private Long id;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色 user/admin
     */
    private String userRole;

    /**
     * 手机号是否能修改 ！待定吧
     */


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
