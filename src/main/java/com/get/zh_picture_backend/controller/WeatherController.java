package com.get.zh_picture_backend.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.manager.weather.api.GaoDeApi;
import com.get.zh_picture_backend.manager.weather.vo.LocationResponse;
import com.get.zh_picture_backend.manager.weather.vo.WeatherResponse;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ResultUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/weather")
@Slf4j
@Api(tags = "weatherController")
public class WeatherController {
    @Resource
    private UserService userService;
    @Resource
    private GaoDeApi gaoDeApi;
    @GetMapping("/get/location")
    @ApiOperation("获取当前地理位置")
    public BaseResponse<LocationResponse> getLocation(HttpServletRequest request){
        log.info("获取当前地理位置");
        //直接调用天气接口Api
        LocationResponse locationResponse = gaoDeApi.getLocationResponse(request);
        return ResultUtils.success(locationResponse);
    }

    @GetMapping("/get/weather")
    @ApiOperation("获取当前天气信息")
    public BaseResponse<WeatherResponse> getWeather(HttpServletRequest request){
        log.info("获取当前天气信息");
        //直接调用天气接口Api
        LocationResponse locationResponse = gaoDeApi.getLocationResponse(request);
        WeatherResponse weatherResponse = gaoDeApi.getWeatherResponse(locationResponse);
        return ResultUtils.success(weatherResponse);
    }

    @GetMapping("/get/randomAvatar")
    @ApiOperation("获取随机头像1")
    public BaseResponse<String> getRandomAvatar(){
        log.info("获取随机头像");
        //直接调用api
        String avatarUrl = gaoDeApi.getRandomAvatar();
        ThrowUtils.throwIf(StrUtil.isBlank(avatarUrl), ErrorCode.PARAMS_ERROR,"获取随机头像失败");
        return ResultUtils.success(avatarUrl);
    }

    @GetMapping("/get/randomAvatar2")
    @ApiOperation("获取随机头像2")
    public BaseResponse<String> getRandomAvatar2(Long userId){
        log.info("获取随机头像");
        //1.首先校验参数
        User user = userService.getById(userId);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ErrorCode.PARAMS_ERROR);
        String randomAvatarUrl = gaoDeApi.getRandomAvatarUrl();
        ThrowUtils.throwIf(StrUtil.isBlank(randomAvatarUrl), ErrorCode.PARAMS_ERROR,"获取随机头像失败");
        return ResultUtils.success(randomAvatarUrl);
    }

    @GetMapping("/get/randomUserName")
    @ApiOperation("获取随机用户名")
    public BaseResponse<String> getRandomUserName(Long userId){
        log.info("获取随机用户名");
        User user = userService.getById(userId);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ErrorCode.PARAMS_ERROR);
        //直接调用api
        String randomUserName = gaoDeApi.getRandomUserName();
        ThrowUtils.throwIf(StrUtil.isBlank(randomUserName), ErrorCode.PARAMS_ERROR,"获取随机用户名失败");
        return ResultUtils.success(randomUserName);
    }

}
