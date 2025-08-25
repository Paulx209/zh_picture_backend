package com.get.zh_picture_backend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import com.get.zh_picture_backend.exception.BusinessException;
import lombok.Getter;

@Getter
public enum SpaceTypeEnum {
    PRIVATE_SPACE("私有空间",0),
    TEAM_SPACE("团队空间",1);

    private String text;
    private Integer value;

    SpaceTypeEnum(String text,Integer value){
        this.text=text;
        this.value=value;
    }

    public static SpaceTypeEnum getSpaceTypeEnum(Integer value){
        if(ObjectUtil.isNull(value)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceTypeEnum[] values = SpaceTypeEnum.values();
        for(SpaceTypeEnum spaceTypeEnum:values){
            if(spaceTypeEnum.getValue().equals(value)){
                return spaceTypeEnum;
            }
        }
        return null;
    }

}
