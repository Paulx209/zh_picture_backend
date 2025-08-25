package com.get.zh_picture_backend.aop;

import cn.hutool.core.util.ObjectUtil;
import com.get.zh_picture_backend.annotation.AuthCheck;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.UserRole;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    //只要方法上添加了@Authcheck注解的 都要执行该方法 相当于设置了一个关卡
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //首先呢 获取登录用户信息
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User user = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(ObjectUtil.isNull(user),ErrorCode.PARAMS_ERROR,"用户未登录");
        //然后获取方法上的注解里的mustRole(可以理解为 要执行该函数的门槛)
        String role = authCheck.mustRole();
        //注解上需要的role 如果role为空的话 说明任何权限的用户都可以执行该函数
        UserRole userRoleByAnnotation = UserRole.getUserRoleByRole(role);
        if (ObjectUtil.isNull(userRoleByAnnotation)) {
            return joinPoint.proceed();
        }
        //user身上的role
        UserRole userRoleByUser = UserRole.getUserRoleByRole(user.getUserRole());
        if(ObjectUtil.isNull(userRoleByUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //如果注解上需要的role的权限>user身上的role的话
        if(userRoleByAnnotation.equals(UserRole.ADMIN)&&!userRoleByUser.equals(UserRole.ADMIN)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }


}
