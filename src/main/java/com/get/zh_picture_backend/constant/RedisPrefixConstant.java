package com.get.zh_picture_backend.constant;

public interface RedisPrefixConstant {
    //存储redis键的一些前缀
     String LOGIN_PHONE_CODE_PREFIX="user:loginCode:";

    String REGISTER_PHONE_CODE_PREFIX="user:registerCode:";

    String REGISTER_PHONE_CODE="registerCode";
    String LOGIN_PHONE_CODE="loginCode";

    String PUBLIC_PICTURE_CACHE="picture:space:public";
    String PRIVATE_PICTURE_CACHE="picture:space:private";
}
