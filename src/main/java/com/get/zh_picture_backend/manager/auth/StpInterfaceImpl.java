package com.get.zh_picture_backend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.manager.auth.constant.SpaceUserPermissionConstant;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.enums.SpaceUserRoleEnum;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {
    @Resource
    private SpaceUserManager spaceUserManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;

    /**
     * 从我们的呢application.yaml中获取参数 "/api"
     */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 获取当前id用户的权限集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //1.校验登录类型
        if (!loginType.equals("space")) {
            return Collections.emptyList();
        }
        //2.如果该用户是管理员的话
        List<String> ADMIN_PERMISSIONS = spaceUserManager.getPermissions(SpaceUserRoleEnum.ADMIN.getValue());
        //3.获取到上下文对象
        SpaceUserAuthContext authContext = getAuthContext();
        System.out.println("获取到的上下文对象为: "+authContext);
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        //4. 通过loginId获取当前登录用户信息
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        //4.1 如果当前用户未登录的话
        if (ObjectUtil.isNull(user)) {
            return Collections.emptyList();
        }
        //4.2 获取唯一的用户标识
        Long userId = user.getId();
        //5.获取spaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserManager.getPermissions(spaceUser.getSpaceRole());
        }
        //6.获取spaceUserId(必然是团队空间) 根据userId获取权限即可
        Long spaceUserId = authContext.getSpaceUserId();
        if (ObjectUtil.isNotNull(spaceUserId)) {
            SpaceUser spaceUserById = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUserById == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceUserById.getSpaceId())
                    .one();
            if (loginSpaceUser == null) {
                return Collections.emptyList();
            }
            return spaceUserManager.getPermissions(loginSpaceUser.getSpaceRole());
        }
        //7.通过pictureId和spaceId来获取权限
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            System.out.println("查出来的照片为:"+picture);
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            //把该图片的spaceId给了spaceId
            spaceId= picture.getSpaceId();
            if (spaceId == null) {
                //说明是公共图库
                if (picture.getUserId().equals(userId) || userService.getById(userId).getUserRole().equals(UserConstant.ADMIN_ROLE)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        //说明是空间
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间数据");
        }
        //根据space类型判断权限
        if (space.getSpaceType().equals(SpaceTypeEnum.PRIVATE_SPACE.getValue())) {
            //如果是私有空间的话
            if (space.getUserId().equals(userId) || userService.getById(userId).getUserRole().equals(UserConstant.ADMIN_ROLE)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            //如果是公共空间的话
            SpaceUser  spaceUser1= spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .one();
            if(spaceUser1==null){
                return new ArrayList<>();
            }
            return spaceUserManager.getPermissions(spaceUser1.getSpaceRole());
        }
    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }


    /**
     * 获取某个用户的角色 粒度比较大 本次项目我们不获取角色 而是直接获取权限
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContext() {
        //A.用于从请求中获取数据 并且转换为上下文对象
        //1.获取到发送请求的对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        //2.Content - Type请求头用于指示请求体的媒体类型和编码方式，常见的值有application/json
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        //兼容get和post操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            //3.说明数据的编码格式是application/json 那就可以使用JSONUtil将该类转换成对象
            String body = ServletUtil.getBody(request);
            //4.直接将请求体中的参数 转换为对象
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            //如果编码格式不是application/json 可能是以键值对的形式在request中传递的
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        //B.封装成上下文对象之后 再对上下文对象进行处理 这个id在这里就是一个占位符
        Long id = authRequest.getId();
        if (ObjectUtil.isNotNull(id)) {
            //如果id不为空 eg：举个例子 http://localhost:8123/api/picture/add

            //  requestURI: /api/picture/add
            String requestURI = request.getRequestURI();
            //将api/直接替换掉 partUri: picture/add
            String partUri = requestURI.replace(contextPath + "/", "");
            //从/中间截取 取前面部分 并且不包括/   moduleName:picture
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;

    }


}
