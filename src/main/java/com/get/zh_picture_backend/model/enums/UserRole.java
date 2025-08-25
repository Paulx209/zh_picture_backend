package com.get.zh_picture_backend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

@Getter
public enum UserRole {
    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;
    private final String role;

    UserRole(String text,String role){
        this.text=text;
        this.role=role;
    }
    public static UserRole getUserRoleByRole(String role){
        if(ObjectUtil.isEmpty(role)){
            return null;
        }
        for(UserRole userRole:UserRole.values()){
            if(userRole.getRole().equals(role)){
                return userRole;
            }
        }
        return null;
    }

}
