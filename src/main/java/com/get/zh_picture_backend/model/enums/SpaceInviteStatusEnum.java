package com.get.zh_picture_backend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

@Getter
public enum SpaceInviteStatusEnum {
    WAITING(0,"待处理"),
    PASS(1,"通过"),
    REJECT(-1,"拒绝");

    private Integer code;
    private String value;

    SpaceInviteStatusEnum(Integer code,String value){
        this.code=code;
        this.value=value;
    }

    /**
     * 根据value寻找enum
     * @param status
     * @return
     */
    public static SpaceInviteStatusEnum getSpaceInviteStatusEnum(String status){
        if(ObjectUtil.isNull(status)){
            return null;
        }
        SpaceInviteStatusEnum[] values = SpaceInviteStatusEnum.values();
        for(SpaceInviteStatusEnum reviewStatus :values){
            if(reviewStatus.getValue().equals(status)){
                return reviewStatus;
            }
        }
        return null;
    }
}
