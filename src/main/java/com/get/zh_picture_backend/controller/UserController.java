package com.get.zh_picture_backend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.get.zh_picture_backend.annotation.AuthCheck;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.model.dto.file.UploadPictureResult;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.dto.user.*;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.UserVO;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ResultUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@Slf4j
@Api(tags="userController")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest){
        log.info("用户注册:{}",registerRequest);
        Long res = userService.userRegister(registerRequest);
        return ResultUtils.success(res);
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request){
        log.info("用户登录:{}",loginRequest);
        LoginUserVO loginUserVO=userService.userLogin(loginRequest,request);
        return ResultUtils.success(loginUserVO);
    }

    @PostMapping("/send_code")
    @ApiOperation("发送验证码")
    public BaseResponse<Boolean> sendPhoneCode(@RequestBody SendCodeRequest sendCodeRequest,HttpServletRequest request){
        log.info("发送手机验证码:{}",sendCodeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(sendCodeRequest),ErrorCode.PARAMS_ERROR,"参数为空");
        boolean flag=userService.sendCode(sendCodeRequest);
        return ResultUtils.success(flag);
    }

    @PostMapping("/login/phone_code")
    @ApiOperation("手机验证码登录")
    public BaseResponse<Boolean> userLoginByPhoneCode(@RequestBody UserLoginByCodeRequest loginByCodeRequest,HttpServletRequest request){
        log.info("手机验证码登录:{}",loginByCodeRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(loginByCodeRequest),ErrorCode.PARAMS_ERROR,"参数为空");
        boolean flag=userService.checkPhoneCode(loginByCodeRequest,request);
        return ResultUtils.success(flag);
    }

    @GetMapping("/get/login")
    @ApiOperation("获取当前登录用户")
    @AuthCheck(mustRole = "user")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        log.info("获取当前登录用户...");
        LoginUserVO loginUserVO=userService.getLoginUser(request);
        return ResultUtils.success(loginUserVO);
    }

    @PostMapping("/logout")
    @ApiOperation("用户注销")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        log.info("用户注销");
        Boolean isLogout=userService.logout(request);
        return ResultUtils.success(isLogout);
    }

    @PostMapping("/add")
    @ApiOperation("管理员添加用户")
    @AuthCheck(mustRole=UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest addRequest){
        log.info("管理员添加用户:{}",addRequest);
        ThrowUtils.throwIf(addRequest==null, ErrorCode.PARAMS_ERROR,"参数为空");
        User user= BeanUtil.copyProperties(addRequest,User.class);
        final String defaultPassword="123456789";
        String encryptPassword = userService.getEncryptPassword(defaultPassword);
        user.setUserPassword(encryptPassword);
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"添加失败");
        return ResultUtils.success(user.getId());
    }

    @GetMapping("/get")
    @ApiOperation("根据id获取用户")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id){
        log.info("根据id获取用户:{}",id);
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        User userById = userService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(userById),ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userById);
    }

    @GetMapping("/get/vo")
    @ApiOperation("根据id获取用户包装类")
    public BaseResponse<UserVO> getUserVOById(Long id){
        log.info("根据id获取用户包装类:{}",id);
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        User userById = userService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(userById),ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(userById));
    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("删除用户")
    @AuthCheck(mustRole =UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        log.info("删除用户:{}",deleteRequest);
        if (deleteRequest == null||deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(deleteRequest.getId());
        if(ObjectUtil.isNull(user)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在");
        }
        boolean res = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(res);
    }

    @PostMapping("/update/avatar")
    @ApiOperation("更改用户的头像")
    public BaseResponse<UploadPictureResult> updateUserAvatar(@RequestPart MultipartFile multipartFile,HttpServletRequest request){
        log.info("更改用户的头像:{}",multipartFile);
        ThrowUtils.throwIf(ObjectUtil.isNull(multipartFile),ErrorCode.PARAMS_ERROR,"参数为空");
        UploadPictureResult uploadPictureResult=userService.updateUserAvatar(request,multipartFile);
        return ResultUtils.success(uploadPictureResult);
    }
    /**
     * 更新用户
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("更新用户")
    @AuthCheck(mustRole =UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        log.info("更新用户:{}",userUpdateRequest);
        if (userUpdateRequest == null||userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户编辑自己的信息
     * @param userEditRequest
     * @return
     */

    @PostMapping("/edit")
    @ApiOperation("编辑用户")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest,HttpServletRequest request){
        log.info("编辑用户:{}",userEditRequest);
        if (userEditRequest == null||userEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean flag=userService.editUser(userEditRequest,request);
        return ResultUtils.success(flag);
    }

    @PostMapping("/list/page/vo")
    @ApiOperation("分页查询请求参数")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(UserQueryRequest queryRequest){
        log.info("分页查询请求参数:{}",queryRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(queryRequest),ErrorCode.PARAMS_ERROR);
        int pageSize = queryRequest.getPageSize();
        int current = queryRequest.getCurrent();
        Page<User> page=new Page<>(current,pageSize);
        Page<User> resPage = userService.page(page, userService.getQueryWrapper(queryRequest));
        Page<UserVO> res=new Page<>(current,pageSize,resPage.getTotal());
        res.setRecords(userService.getUserVOList(resPage.getRecords()));
        return ResultUtils.success(res);
    }
}
