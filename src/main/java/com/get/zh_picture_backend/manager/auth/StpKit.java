package com.get.zh_picture_backend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class StpKit {
    public static final String SPACE_TYPE="space";

    /**
     * 默认原生会话对象，项目中不会使用，默认的spaceType为login
     */
    public static final StpLogic DEFAULT= StpUtil.stpLogic;

    /**
     * 本来就是对团队空间权限的处理 因此space 没毛病吧
     * Space会话对象，管理Space中所有账号的权限
     */
    public static final StpLogic SPACE= new StpLogic(SPACE_TYPE);
}
