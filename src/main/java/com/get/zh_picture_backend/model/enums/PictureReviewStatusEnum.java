package com.get.zh_picture_backend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {
    REVIEWING(0,"待审核"),
    PASS(1,"通过"),
    REJECT(2,"拒绝");

    private Integer code;
    private String value;

    PictureReviewStatusEnum(Integer code,String value){
        this.code=code;
        this.value=value;
    }

    /**
     * 根据value寻找enum
     * @param status
     * @return
     */
    public static PictureReviewStatusEnum getPictureReviewStatusEnum(String status){
        if(ObjectUtil.isNull(status)){
            return null;
        }
        PictureReviewStatusEnum[] values = PictureReviewStatusEnum.values();
        for(PictureReviewStatusEnum reviewStatus :values){
            if(reviewStatus.getValue().equals(status)){
                return reviewStatus;
            }
        }
        return null;
    }

}
