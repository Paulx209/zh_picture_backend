package com.get.zh_picture_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.get.zh_picture_backend.constant.RedisPrefixConstant;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.manager.SendSms.api.SendSmsUtils;
import com.get.zh_picture_backend.manager.auth.StpKit;
import com.get.zh_picture_backend.manager.upload.FilePictureUpload;
import com.get.zh_picture_backend.manager.upload.PictureUploadTemplate;
import com.get.zh_picture_backend.manager.weather.api.GaoDeApi;
import com.get.zh_picture_backend.model.dto.file.UploadPictureResult;
import com.get.zh_picture_backend.model.dto.user.*;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.UserRole;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.UserVO;
import com.get.zh_picture_backend.service.SpaceInviteService;
import com.get.zh_picture_backend.service.UserService;


import com.get.zh_picture_backend.mapper.UserMapper;

import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author Paul
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-02-17 17:06:50
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final String SALT = "getian";

    @Resource
    private SendSmsUtils sendSmsUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private GaoDeApi gaoDeApi;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Lazy
    @Resource
    private SpaceInviteService spaceInviteService;

    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String phoneCode = userRegisterRequest.getPhoneCode();
        String userPhone = userRegisterRequest.getUserPhone();
        String regex = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";

        //1.校验参数的完整性
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword, userPhone, phoneCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPhone.matches(regex)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
        }
        //2.检查两次密码输入是否一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        }
        //2.1 校验验证码是否一致
        String key = RedisPrefixConstant.REGISTER_PHONE_CODE_PREFIX + userPhone;
        String redisCode = redisTemplate.opsForValue().get(key);
        System.out.println("redisCode:" + redisCode);
        System.out.println("phoneCode:" + phoneCode);
        ThrowUtils.throwIf(ObjUtil.isNull(redisCode), ErrorCode.PARAMS_ERROR, "验证码已失效，请重新发送");
        ThrowUtils.throwIf(!phoneCode.equals(redisCode), ErrorCode.PARAMS_ERROR, "验证码错误");
        //3.检查用户账号是否和数据库中的重复
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUserAccount, userAccount);
        User one = getOne(userLambdaQueryWrapper);
        if (!ObjectUtil.isNull(one)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        //4.密码一定要加密
        User user = BeanUtil.copyProperties(userRegisterRequest, User.class, "checkPassword");
        user.setUserPassword(getEncryptPassword(userPassword));
        user.setUserName("无名");
        user.setUserRole(UserRole.USER.getRole());
        //5.插入数据到数据库中/再新增一条消息 让用户登录之后就能看见
        //插入数据到数据库中
        boolean save = save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库异常");
        } else {
            //7.将验证码从redis中删除
            redisTemplate.delete(key);
        }
        return user.getId();
    }


    /**
     * @param loginRequest
     * @return 脱敏后的用户信息
     */
    public LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request) {
        //1.校验参数
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8 || userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        //2.根据Account查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        User one = getOne(queryWrapper);
        if (ObjectUtil.isNull(one)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户未注册");
        }
        //3.校验账号密码是否正确
        String encryptPassword = getEncryptPassword(userPassword);
        queryWrapper.eq(User::getUserPassword, encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (!ObjectUtil.isNotNull(user)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        //4.首先我们先更新一下user
        User user1;
        try {
            user1 = fillUserInfo(user.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //4.向当前的session中存储用户
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user1);
        //5.记录用户登录态到sa-token中 便于用户鉴权使用 这里就是同一个用户
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return getLoginUserVO(user);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = BeanUtil.copyProperties(user, LoginUserVO.class);
        return loginUserVO;
    }

    /**
     * 对密码进行加密
     *
     * @param password
     * @return
     */
    public String getEncryptPassword(String password) {
        String res = DigestUtils.md5DigestAsHex((password + SALT).getBytes());
        return res;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未登录,无权限");
        }
        return this.getLoginUserVO(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public Boolean logout(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户为空");
        }
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ErrorCode.PARAMS_ERROR, "参数为空");
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        return userVO;
    }

    /**
     * 获取用户VO集合
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(userList), ErrorCode.PARAMS_ERROR, "参数为空");
        List<UserVO> userVOS = BeanUtil.copyToList(userList, UserVO.class);
        return userVOS;
    }

    /**
     * 获取一个条件构造器
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    /**
     * 不同场景下发送验证码
     *
     * @param sendCodeRequest
     * @return
     */

    @Override
    public boolean sendCode(SendCodeRequest sendCodeRequest) {
        try {
            //1.1校验手机号格式是否正确 (前端也会校验一次)
            String regex = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";
            boolean matchFlag = sendCodeRequest.getPhoneNumber().matches(regex);
            ThrowUtils.throwIf(!matchFlag, ErrorCode.PARAMS_ERROR, "手机号格式错误");
            ThrowUtils.throwIf(StrUtil.isBlank(sendCodeRequest.getCodeType()), ErrorCode.PARAMS_ERROR, "codeType不能为空");
            //1.2 如果是注册用户的话 校验一下该手机号是否已经被注册过
            if (sendCodeRequest.getCodeType().equals(RedisPrefixConstant.REGISTER_PHONE_CODE)) {
                User one = lambdaQuery().eq(User::getUserPhone, sendCodeRequest.getPhoneNumber()).one();
                System.out.println("查询到的用户为：" + one);
                ThrowUtils.throwIf(ObjectUtil.isNotNull(one), ErrorCode.PARAMS_ERROR, "该手机号已被注册");
            }
            //2.获取随机验证码
            String randomCode = getRandomCode();
            //3.调用api发送
            SendSmsResponse sendSmsResponse = sendSmsUtils.sendSmsCode(sendCodeRequest.getPhoneNumber(), randomCode);
            if (!sendSmsResponse.getStatusCode().equals(200)) {
                String message = sendSmsResponse.getBody().getMessage();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, message);
            }
            //4.根据不同的发送模式 进行校验
            String key = null;
            if (sendCodeRequest.getCodeType().equals(RedisPrefixConstant.REGISTER_PHONE_CODE)) {
                key = RedisPrefixConstant.REGISTER_PHONE_CODE_PREFIX + sendCodeRequest.getPhoneNumber();
            } else {
                key = RedisPrefixConstant.LOGIN_PHONE_CODE_PREFIX + sendCodeRequest.getPhoneNumber();
            }
            System.out.println("redis存储的key为:" + key);
            redisTemplate.opsForValue().set(key, randomCode, 60 * 5, TimeUnit.SECONDS);
            String code = redisTemplate.opsForValue().get(key);
            System.out.println("获取到的code为:" + code);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.OTHER_ERROR, e.getMessage());
        }
        return true;
    }


    /**
     * 随机生成六位数code
     *
     * @return
     */
    public String getRandomCode() {
        //随机生成六位数code
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 6; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    /**
     * 验证码登录时校验手机号验证码
     *
     * @param loginByCodeRequest
     * @return
     */
    @Override
    public boolean checkPhoneCode(UserLoginByCodeRequest loginByCodeRequest, HttpServletRequest request) {
        //1.判断手机号格式
        String regex = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";
        boolean matchFlag = loginByCodeRequest.getPhoneNumber().matches(regex);
        ThrowUtils.throwIf(!matchFlag, ErrorCode.PARAMS_ERROR, "手机号格式错误");
        //2.从缓存中获取验证码 并且进行校验
        String key = RedisPrefixConstant.LOGIN_PHONE_CODE_PREFIX + loginByCodeRequest.getPhoneNumber();
        String code = redisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.NOT_FOUND_ERROR, "验证码已过期，请重新发送");
        ThrowUtils.throwIf(!code.equals(loginByCodeRequest.getCode()), ErrorCode.PARAMS_ERROR, "验证码错误");
        //3.校验成功 如何存储？ 因为这个方法是对应登录来说的 我们只需要根据手机号查询用户 然后存储到session中即可
        User one = lambdaQuery().eq(User::getUserPhone, loginByCodeRequest.getPhoneNumber()).one();
        User user = fillUserInfo(one.getId());

        //存入session
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //todo 存入sa-token
        StpKit.SPACE.login(one.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, one);
        return true;
    }

    /**
     * 判断登录的用户是否是管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        //用户不为空 并且用户的角色是管理员
        return user != null && user.getUserRole().equals(UserConstant.ADMIN_ROLE);
    }

    /**
     * 更改用户的头像
     *
     * @param multipartFile
     * @return
     */
    @Override
    public UploadPictureResult updateUserAvatar(HttpServletRequest request, MultipartFile multipartFile) {
        //1.模板设计模式中 文件上传对象
        PictureUploadTemplate template = filePictureUpload;
        LoginUserVO loginUser = getLoginUser(request);
        //2.定义一个用户头像在cos中存储的前缀
        String prefix = String.format("user/avatar/%s", loginUser.getId());
        //3.调用文件上传对象的上传文件的方法
        UploadPictureResult uploadPictureResult = template.uploadPicture(multipartFile, prefix);
        //4.然后获取到图片解析对象
        return uploadPictureResult;
    }

    /**
     * 更改用户信息
     *
     * @param userEditRequest
     * @param request
     * @return
     */
    @Override
    public boolean editUser(UserEditRequest userEditRequest, HttpServletRequest request) {
        //1.参数已经校验过了
        //2.判断当前用户是否有权限
        LoginUserVO loginUser = getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.getId().equals(userEditRequest.getId()), ErrorCode.PARAMS_ERROR, "无权限修改");
        //3.更新数据库
        User user = BeanUtil.copyProperties(userEditRequest, User.class);
        boolean res = this.updateById(user);
        ThrowUtils.throwIf(!res, ErrorCode.OTHER_ERROR, "更新失败");
        return true;
    }

    /**
     * 填充用户信息
     *
     * @param userId
     */

    public User fillUserInfo(Long userId) {
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.PARAMS_ERROR, "参数为空");
        User user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户信息不存在");
        if (user.getUserName().equals("无名") || StrUtil.isEmpty(user.getUserName())) {
            String randomUserName = gaoDeApi.getRandomUserName();
            user.setUserName(randomUserName);
        }
        if (StrUtil.isEmpty(user.getUserAvatar())) {
            String randomAvatarUrl = gaoDeApi.getRandomAvatarUrl();
            user.setUserAvatar(randomAvatarUrl);
        }
        //开始更新用户
        boolean res = updateById(user);
        ThrowUtils.throwIf(!res, ErrorCode.OTHER_ERROR, "更新失败");
        return user;
    }
}




