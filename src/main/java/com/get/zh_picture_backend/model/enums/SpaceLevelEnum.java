package com.get.zh_picture_backend.model.enums;


import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON("普通版",0,100L,100L*1024*1024),

    PROFESSIONAL("专业版",1,1000L,1000L*1024*1024),
    FLAGSHIP("旗舰版",2,10000L,10000L*1024*1024);

    private final String text;
    private final Integer value;
    private final Long maxCount;
    private final Long maxSize;

    SpaceLevelEnum(String text,Integer value,Long maxCount,Long maxSize){
        this.text=text;
        this.value=value;
        this.maxCount=maxCount;
        this.maxSize=maxSize;
    }
    public static SpaceLevelEnum getEnumByValue(Integer value){
        if(ObjectUtil.isNull(value)){
            return null;
        }
        for(SpaceLevelEnum levelEnum:SpaceLevelEnum.values())
        {
            if(levelEnum.getValue().equals(value)){
                return levelEnum;
            }
        }
        return null;
    }

}
