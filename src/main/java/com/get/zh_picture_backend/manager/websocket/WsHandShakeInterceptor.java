package com.get.zh_picture_backend.manager.websocket;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.get.zh_picture_backend.manager.auth.SpaceUserManager;
import com.get.zh_picture_backend.manager.auth.constant.SpaceUserPermissionConstant;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WsHandShakeInterceptor implements HandshakeInterceptor {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserManager spaceUserManager;
    /**
     * 在握手前我们应该判断的逻辑
     * @param serverHttpRequest
     * @param response
     * @param wsHandler
     * @param attributes
     * @return
     * @throws Exception
     */

    @Override
    public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //1.获取请求参数  并且判断 比如说pictureId\loginUser\该用户是否有该图片的操作权限
        if(serverHttpRequest instanceof ServletServerHttpRequest){
            //1.1 判断pictureId
            HttpServletRequest request = ((ServletServerHttpRequest) serverHttpRequest).getServletRequest();
            String pictureId = request.getParameter("pictureId");
            if(StrUtil.isBlank(pictureId)){
                log.info("缺少参数，拒绝握手");
                return false;
            }
            //1.2 判断登录用户
            LoginUserVO loginUser = userService.getLoginUser(request);
            User user=userService.getById(loginUser.getId());
            //如何把这个loginUser转换为User
            if(ObjectUtil.isNull(loginUser)){
                log.info("用户未登录，拒绝握手");
            }
            //1.3 判断当前图片是否存在
            Picture picture = pictureService.getById(Long.valueOf(pictureId));
            if(ObjectUtil.isNull(picture)){
                log.info("图片不存在，拒绝握手");
                return false;
            }
            //1.4 判断当前图片是公共图库/个人空间/团队空间的
            Long spaceId = picture.getSpaceId();
            Space space=null;
            if(spaceId!=null){
                space=spaceService.getById(spaceId);
                if(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE_SPACE.getValue())){
                    log.info("不是团队空间，拒绝握手");
                    return false;
                }
            }
            //1.5 如果是团队空间 判断当前该用户是否有edit的权限
            List<String> permissionList = spaceUserManager.getPermissionList(space, loginUser);
            if(!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)){
                log.info("没有操作该空间的权限,拒绝握手");
                return false;
            }
            //2.参数校验完毕 向attributes添加参数
            attributes.put("pictureId",Long.valueOf(pictureId));
            attributes.put("userId",loginUser.getId());
            attributes.put("user",user);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
