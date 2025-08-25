package com.get.zh_picture_backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.manager.auth.annotation.SaSpaceCheckPermission;
import com.get.zh_picture_backend.manager.auth.constant.SpaceUserPermissionConstant;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserAddRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserBreakTeamRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserEditRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceUserVO;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ResultUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Api(tags="spaceUserController")
@Slf4j
@RequestMapping("/spaceuser")
public class SpaceUserController {
    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;


    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @ApiOperation("添加成员到空间")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        Long id = spaceUserService.addSpaceUser(spaceUserAddRequest,loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @ApiOperation("从空间移除成员")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        log.info("从空间移除成员:{}",deleteRequest);
        ThrowUtils.throwIf(deleteRequest==null,ErrorCode.PARAMS_ERROR,"参数为空");
        Long id = deleteRequest.getId();
        LoginUserVO loginUser = userService.getLoginUser(request);
        SpaceUser spaceUserServiceById = spaceUserService.getById(id);
        if(spaceUserServiceById.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无法删除自己,可选择解散空间");
        }
        ThrowUtils.throwIf(spaceUserServiceById==null,ErrorCode.NOT_FOUND_ERROR,"未找到该成员");
        //操作数据库
        boolean res = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @ApiOperation("查询某个成员在某个空间的信息")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @ApiOperation("查询成员信息列表")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        log.info("查询成员信息列表:"+spaceUserQueryRequest);
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //校验当前用户是不是管理员 他妈的 加的注解没有生效 todo:有时间再改
        spaceUserService.checkAuth(spaceUserQueryRequest,userService.getLoginUser(request));
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @ApiOperation("编辑成员信息（设置权限）")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        log.info("编辑成员信息:{}", spaceUserEditRequest);
        // 1.参数校验
        if (spaceUserEditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUser = userService.getLoginUser(request);
        //2.交给SpaceUserService处理
        Boolean flag=spaceUserService.editSpaceUser(spaceUserEditRequest,loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 查询我加入的共享空间列表
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    @ApiOperation("查询我加入的共享空间列表")
    public BaseResponse<List<SpaceUserVO>> getMySpaceList(HttpServletRequest request){
        LoginUserVO loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest=new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        //拿到一个条件构造器  在spaceUser表中拿userId进行查找
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        List<SpaceUser> list = spaceUserService.list(queryWrapper);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);
        return ResultUtils.success(spaceUserVOList);
    }

    @PostMapping("/break/team")
    @ApiOperation("解除我的团队")
    public BaseResponse<Boolean> breakMyTeam(@RequestBody SpaceUserBreakTeamRequest breakTeamRequest,HttpServletRequest request){
        log.info("解散我的团队:{}",breakTeamRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(breakTeamRequest),ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        Boolean flag=spaceUserService.breakTeam(breakTeamRequest,loginUser);
        return ResultUtils.success(flag);
    }

}
