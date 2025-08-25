package com.get.zh_picture_backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteAddRequest;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteQueryRequest;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteUpdateRequest;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceInviteVO;
import com.get.zh_picture_backend.service.SpaceInviteService;
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
@RequestMapping("/spaceInvite")
@Api(tags = "spaceInviteController")
@Slf4j
public class SpaceInviteController{
    @Resource
    private SpaceInviteService spaceInviteService;
    @Resource
    private UserService userService;

    @PostMapping("/search")
    @ApiOperation("查询用户邀请记录")
    public BaseResponse<List<SpaceInviteVO>> search(@RequestBody SpaceInviteQueryRequest queryRequest, HttpServletRequest request){
        log.info("查询邀请记录:{}",queryRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(queryRequest), ErrorCode.NOT_FOUND_ERROR,"参数不能为空");
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<SpaceInviteVO> userSpaceInviteList = spaceInviteService.getUserSpaceInviteList(queryRequest, loginUser);
        return ResultUtils.success(userSpaceInviteList);
    }

    /**
     *
     * @param addRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("新增邀请记录")
    public BaseResponse<Long> addSpaceInvite(@RequestBody SpaceInviteAddRequest addRequest,HttpServletRequest request){
        log.info("新增邀请记录:{}",addRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(addRequest), ErrorCode.NOT_FOUND_ERROR,"参数不能为空");
        LoginUserVO loginUser = userService.getLoginUser(request);
        Long spaceInviteId = spaceInviteService.addSpaceInvite(addRequest,loginUser);
        return ResultUtils.success(spaceInviteId);
    }

    /**
     * 更新邀请记录
     * @param updateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("更新邀请记录")
    public BaseResponse<Boolean> updateSpaceInvite(@RequestBody SpaceInviteUpdateRequest updateRequest, HttpServletRequest request){
        log.info("更新邀请记录:{}",updateRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(updateRequest), ErrorCode.NOT_FOUND_ERROR,"参数不能为空");
        LoginUserVO loginUser = userService.getLoginUser(request);
        Boolean result = spaceInviteService.updateSpaceInvite(updateRequest,loginUser);
        return ResultUtils.success(result);
    }

}
