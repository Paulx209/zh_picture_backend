package com.get.zh_picture_backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/test")
@Api(tags="testController")
@Slf4j
public class TestController {
    @Autowired
    private UserService userService;
    @GetMapping("")
    @ApiOperation("测试api接口")
    public BaseResponse<Boolean> test(HttpServletRequest httpServletRequest){
        return ResultUtils.success(true);
    }

    @GetMapping("/validUserName")
    @ApiOperation("测试用户名是否可用")
    public BaseResponse<String> validUserName(@RequestParam("username") String username){
        log.info("测试用户名是否可用:{}",username);
        LambdaQueryWrapper<User> userWrapper=new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserAccount,username);
        User one = userService.getOne(userWrapper);
        if(ObjectUtil.isNotNull(one)){
           throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"该用户名已存在");
        }
        return ResultUtils.success("该用户名可用");
    }

}
