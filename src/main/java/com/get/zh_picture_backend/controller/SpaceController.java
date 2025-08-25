package com.get.zh_picture_backend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.get.zh_picture_backend.annotation.AuthCheck;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.manager.auth.SpaceUserManager;
import com.get.zh_picture_backend.model.enums.ErrorCode;

import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;

import com.get.zh_picture_backend.model.dto.space.SpaceAddRequest;
import com.get.zh_picture_backend.model.dto.space.SpaceEditRequest;
import com.get.zh_picture_backend.model.dto.space.SpaceQueryRequest;
import com.get.zh_picture_backend.model.dto.space.SpaceUpdateRequest;

import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.vo.LoginUserVO;

import com.get.zh_picture_backend.model.vo.SpaceVO;

import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ResultUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Api(tags = "spaceController")
@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    @Resource
    private SpaceUserManager spaceUserManager;
    /**
     * 删除picture
     * @param deleteRequest
     * @param request
     * @return
     */
    @DeleteMapping("/delete")
    @ApiOperation("删除空间")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        log.info("删除空间:{}",deleteRequest.getId());
        Long id=deleteRequest.getId();
        //1.判断空间是否存在
        Space Space = spaceService.getById(id);
        if(ObjectUtil.isNull(Space)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        //2.仅本人或者管理员可以删除
        LoginUserVO loginUser = userService.getLoginUser(request);
        if(!loginUser.getId().equals(Space.getUserId())&&!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限删除该空间");
        }
        //3.操作数据库
        spaceService.removeById(id);
        return ResultUtils.success(true);
    }

    /**
     * 更新space 管理员干的事情 (一般是用户申请更新额度 然后。。。)
     * @param updateRequest
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("更新空间")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest updateRequest, HttpServletRequest request){
        log.info("更新空间:{}",updateRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(updateRequest)||updateRequest.getId()<=0,ErrorCode.PARAMS_ERROR,"参数为空");
        //判断该id的space是否存在
        Long id=updateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(oldSpace),ErrorCode.PARAMS_ERROR,"space不存在");
        //将dto转换成space
        Space newSpace= BeanUtil.copyProperties(updateRequest,Space.class);
        spaceService.fillSpaceBySpaceLevel(newSpace);
        //对数据进行校验
        spaceService.validSpace(newSpace,false);
        //操作数据库
        boolean res = spaceService.updateById(newSpace);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(res);
    }

    /**
     * 编辑图片是用户干的事情
     * @param editRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("编辑空间")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest editRequest, HttpServletRequest request){
        log.info("编辑空间:{}",editRequest);
        ThrowUtils.throwIf(editRequest==null||editRequest.getId()<=0,ErrorCode.PARAMS_ERROR);
        //要更新的space是否存在
        Long id= editRequest.getId();
        Space oldSpace = spaceService.getById(id);
        if(ObjectUtil.isNull(oldSpace)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //转变成Space类
        Space newSpace=BeanUtil.copyProperties(oldSpace,Space.class);
        newSpace.setSpaceName(editRequest.getSpaceName());
        newSpace.setEditTime(new Date());
        //是否是管理员或者本人
        //todo 是否要修改 只能是空间的创始人才能edit
        LoginUserVO loginUser = userService.getLoginUser(request);
        if(!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)&&!oldSpace.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限修改图片");
        }
        //补充审核参数
        spaceService.fillSpaceBySpaceLevel(newSpace);
        //操作数据库
        LambdaUpdateWrapper<Space> updateWrapper=new LambdaUpdateWrapper<>();
        updateWrapper.eq(Space::getId,id);
        boolean res = spaceService.update(newSpace,updateWrapper);
        ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"更新失败");
        return ResultUtils.success(true);
    }


    /**
     * 根据id查询(仅限管理员查询) 返回原类
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("根据id查询")
    public BaseResponse<Space> getSpaceById(Long id,HttpServletRequest request){
        log.info("根据id查询:{}",id);
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR,"该图片不存在");
        //获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据id查询，返回封装类
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation("根据id查询spaceVo")
    public BaseResponse<SpaceVO> getSpaceVoById(Long id,HttpServletRequest request){
        log.info("根据id查询:{}",id);
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR,"该图片不存在");
        //获取封装类
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserManager.getPermissionList(space, loginUser);
        System.out.println("获取到的权限列表为:"+permissionList);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取图片列表(仅管理员可用)
     */
    @PostMapping("/list/page")
    @ApiOperation("分页获取空间列表")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest){
        log.info("分页获取图片列表:{}",spaceQueryRequest);
        Page<Space> spacePage=new Page<>(spaceQueryRequest.getCurrent(),spaceQueryRequest.getPageSize());
        Page<Space> pageRes = spaceService.page(spacePage, spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(pageRes);
    }

    /**
     * 分页获取图片列表(封装类)
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("分页获取空间列表---vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,HttpServletRequest request){
        log.info("分页获取空间列表---vo:{}",spaceQueryRequest);
        long current=spaceQueryRequest.getCurrent();
        long size=spaceQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        //查询数据库
        Page<Space> pageRes = spaceService.page(new Page<Space>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(pageRes, request);
        return ResultUtils.success(spaceVOPage);
    }

    @PostMapping("/add")
    @ApiOperation("创建个人空间")
    public BaseResponse<Long> createPersonSpace(@RequestBody SpaceAddRequest spaceAddRequest,HttpServletRequest request){
        log.info("创建个人空间");
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceAddRequest),ErrorCode.PARAMS_ERROR);
        Long spaceId=spaceService.createPersonSpace(spaceAddRequest,request);
        return ResultUtils.success(spaceId);
    }



}
