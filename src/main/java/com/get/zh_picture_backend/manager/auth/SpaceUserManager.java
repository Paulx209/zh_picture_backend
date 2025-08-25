package com.get.zh_picture_backend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.manager.auth.constant.SpaceUserPermissionConstant;
import com.get.zh_picture_backend.manager.auth.model.SpaceUserAuthConfig;
import com.get.zh_picture_backend.manager.auth.model.SpaceUserRole;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.enums.SpaceUserRoleEnum;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SpaceUserManager {
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;


    static{
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG= JSONUtil.toBean(json,SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     * @param userRole
     * @return
     */
    public List<String> getPermissions(String userRole){
        if(StrUtil.isBlank(userRole)){
            return Collections.emptyList();
        }
        //找到匹配的角色
        SpaceUserRole spaceUserRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(role -> role.getKey().equals(userRole)).findFirst().orElse(null);
        List<String> permissions = spaceUserRole.getPermissions();
        return permissions;
    }

    /**
     * 获取当前用户所对应空间的权限列表
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, LoginUserVO loginUser){
        if(loginUser==null){
            return new ArrayList<>();
        }
        //管理员权限
        List<String> adminPermissions = getPermissions(SpaceUserRoleEnum.ADMIN.getValue());
        //公共空间
        if(space==null){
            if(loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
                return adminPermissions;
            }else{
                return new ArrayList<>();
            }
        }
        //私有空间
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getSpaceTypeEnum(space.getSpaceType());
        if(spaceTypeEnum==null)return new ArrayList<>();
        //需要判断是否是团队空间
        switch(spaceTypeEnum){
            case PRIVATE_SPACE:
                if(space.getUserId().equals(loginUser.getId())){
                    return adminPermissions;
                }else{
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            case TEAM_SPACE :
                //团队空间
                SpaceUser one = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .one();
                if(ObjectUtil.isNull(one)){
                    return Collections.emptyList();
                }else{
                    return getPermissions(one.getSpaceRole());
                }
            default:
                return new ArrayList<>();
        }
    }
}
