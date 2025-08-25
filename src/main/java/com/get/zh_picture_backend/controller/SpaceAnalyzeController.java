package com.get.zh_picture_backend.controller;


import cn.hutool.core.util.ObjectUtil;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.dto.space.analyze.*;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.analyze.*;
import com.get.zh_picture_backend.service.SpaceAnalyzeService;
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
@RequestMapping("/analyze")
@Api(tags = "spaceAnalyzeController")
@Slf4j
public class SpaceAnalyzeController {
    @Resource
    private UserService userService;
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @PostMapping("/usage")
    @ApiOperation("获取空间使用情况")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeRequest analyzeRequest, HttpServletRequest request){
        log.info("获取空间使用情况:{}",analyzeRequest);
        System.out.println(analyzeRequest.getSpaceId());
        ThrowUtils.throwIf(ObjectUtil.isNull(analyzeRequest), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(analyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    @PostMapping("/category")
    @ApiOperation("获取图片分类情况")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest categoryAnalyzeRequest, HttpServletRequest request){
        log.info("获取图片分类情况:{}",categoryAnalyzeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(categoryAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeService.getSpaceCategoryAnalyze(categoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    @PostMapping("/tags")
    @ApiOperation("获取图片标签情况")
    public BaseResponse<List<SpaceTagsAnalyzeResponse>> getSpaceTagsAnalyze(@RequestBody SpaceTagsAnalyzeRequest tagsAnalyzeRequest, HttpServletRequest request){
        log.info("获取图片标签情况:{}",tagsAnalyzeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(tagsAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<SpaceTagsAnalyzeResponse> spaceTagsAnalyze = spaceAnalyzeService.getSpaceTagsAnalyze(tagsAnalyzeRequest,loginUser);
        return ResultUtils.success(spaceTagsAnalyze);
    }


    @PostMapping("/size")
    @ApiOperation("获取图片大小分布情况")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest sizeAnalyzeRequest, HttpServletRequest request){
        log.info("获取图片大小分布情况:{}",sizeAnalyzeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(sizeAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze = spaceAnalyzeService.getSpaceSizeAnalyze(sizeAnalyzeRequest,loginUser);
        return ResultUtils.success(spaceSizeAnalyze);
    }

    @PostMapping("/user")
    @ApiOperation("获取用户空间使用情况")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest userAnalyzeRequest, HttpServletRequest request){
        log.info("获取用户空间使用情况:{}",userAnalyzeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(userAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> spaceUserAnalyze = spaceAnalyzeService.getSpaceUserAnalyze(userAnalyzeRequest,loginUser);
        return ResultUtils.success(spaceUserAnalyze);
    }

    @PostMapping("/top")
    @ApiOperation("获取空间使用排行榜")
    public BaseResponse<List<Space>> getSpaceTopAnalyze(@RequestBody SpaceTopAnalyzeRequest topAnalyzeRequest, HttpServletRequest request){
        log.info("获取空间使用排行榜:{}",topAnalyzeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(topAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        List<Space> spaceTopAnalyze = spaceAnalyzeService.getSpaceTopAnalyze(topAnalyzeRequest,loginUser);
        return ResultUtils.success(spaceTopAnalyze);
    }

}
