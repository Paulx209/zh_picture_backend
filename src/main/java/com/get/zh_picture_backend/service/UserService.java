package com.get.zh_picture_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.get.zh_picture_backend.model.dto.file.UploadPictureResult;
import com.get.zh_picture_backend.model.dto.user.*;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Paul
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-17 17:06:50
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param loginRequest
     * @return
     */
    LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    LoginUserVO getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    Boolean logout(HttpServletRequest request);

    /**
     * 获取UserVO
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取List<UserVO>
     * @param
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取user的条件构造器
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 对密码进行加密
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 当前登录的用户是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 不同场景下发送验证码
     *
     * @param sendCodeRequest
     * @return
     */
    boolean sendCode(SendCodeRequest sendCodeRequest);


    /**
     * 校验手机号验证码
     * @param loginByCodeRequest
     * @return
     */
    boolean checkPhoneCode(UserLoginByCodeRequest loginByCodeRequest,HttpServletRequest request);

    /**
     * 更改用户的头像
     * @param multipartFile
     * @return
     */
    UploadPictureResult updateUserAvatar(HttpServletRequest request,MultipartFile multipartFile);

    /**
     * 编辑用户信息
     */
    boolean editUser(UserEditRequest userEditRequest,HttpServletRequest request);

}
