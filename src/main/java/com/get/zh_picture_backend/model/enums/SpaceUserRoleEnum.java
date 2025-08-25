package com.get.zh_picture_backend.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum SpaceUserRoleEnum {
    REVIEWER("浏览者","viewer"),
    EDITOR("编辑者","editor"),
    ADMIN("管理者","admin");
    /**
     *  内容
     */
    private final String text;

    /**
     *  值
     */
    private final String value;

    SpaceUserRoleEnum(String text, String value){
        this.text=text;
        this.value=value;
    }

    public static SpaceUserRoleEnum getEnum(String value){
        for(SpaceUserRoleEnum e:SpaceUserRoleEnum.values()){
            if(e.getValue().equals(value)){
                return e;
            }
        }
        return null;
    }

    public static List<String> getAllTexts(){
        return Arrays.stream(SpaceUserRoleEnum.values()).map(SpaceUserRoleEnum::getText).collect(Collectors.toList());
    }
    public static List<String> getAllValues(){
        return Arrays.stream(SpaceUserRoleEnum.values()).map(SpaceUserRoleEnum::getValue).collect(Collectors.toList());
    }


    }
