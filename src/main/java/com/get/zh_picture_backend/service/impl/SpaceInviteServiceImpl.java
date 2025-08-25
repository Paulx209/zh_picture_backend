package com.get.zh_picture_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.get.zh_picture_backend.exception.BusinessException;

import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteAddRequest;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteQueryRequest;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteUpdateRequest;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.SpaceInvite;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.enums.SpaceInviteStatusEnum;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.enums.SpaceUserRoleEnum;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceInviteVO;
import com.get.zh_picture_backend.model.vo.SpaceVO;
import com.get.zh_picture_backend.service.SpaceInviteService;
import com.get.zh_picture_backend.mapper.SpaceInviteMapper;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Paul
 * @description 针对表【space_invite】的数据库操作Service实现
 * @createDate 2025-03-09 11:10:43
 */
@Service
public class SpaceInviteServiceImpl extends ServiceImpl<SpaceInviteMapper, SpaceInvite>
        implements SpaceInviteService {
    @Resource
    private UserService userService;
    @Lazy
    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    public SpaceInviteServiceImpl() {
        super();
    }

    @Override
    public QueryWrapper<SpaceInvite> getQueryWrapper(SpaceInviteQueryRequest queryRequest) {
        QueryWrapper<SpaceInvite> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNull(queryRequest)) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = queryRequest.getId();
        Long spaceId = queryRequest.getSpaceId();
        Long inviterId = queryRequest.getInviterId();
        Long inviteeId = queryRequest.getInviteeId();
        Integer status = queryRequest.getStatus();
        //首先判断searchText

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(inviterId), "inviterId", inviterId);
        //将spacerType该属性也作为查询条件
        queryWrapper.eq(ObjectUtil.isNotNull(inviteeId), "inviteeId", inviteeId);
        queryWrapper.eq(ObjectUtil.isNotNull(status), "status", status);

        return queryWrapper;
    }

    @Override
    public void validSpaceInvite(SpaceInvite spaceInvite) {
        ThrowUtils.throwIf(spaceInvite == null, ErrorCode.NOT_FOUND_ERROR, "空间邀请不能为空");
        Long inviterId = spaceInvite.getInviterId();
        User inviter = userService.getById(inviterId);
        ThrowUtils.throwIf(inviter == null, ErrorCode.NOT_FOUND_ERROR, "邀请人不存在");
        Long spaceId = spaceInvite.getSpaceId();
        ThrowUtils.throwIf(spaceId == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(inviterId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "邀请人暂未权限");
        }
    }

    @Override
    public List<SpaceInviteVO> getSpaceInviteVOList(List<SpaceInvite> spaceInviteList) {
        // 1. 首先将SpaceInvite中已有的属性 赋值给SpaceInviteVO
        List<SpaceInviteVO> spaceInviteVOSet = spaceInviteList.stream().map(SpaceInviteVO::objToVO).collect(Collectors.toList());
        //2.先整体查询邀请人和被邀请人的id
        Set<Long> inviteeList = spaceInviteVOSet.stream().map(SpaceInviteVO::getInviteeId).collect(Collectors.toSet());
        Set<Long> invitorList = spaceInviteVOSet.stream().map(SpaceInviteVO::getInviterId).collect(Collectors.toSet());
        Set<Long> spaceIdList = spaceInviteVOSet.stream().map(SpaceInviteVO::getSpaceId).collect(Collectors.toSet());
        //3.然后根据id查询实体类
        Map<Long, List<User>> inviteeMap=new HashMap<>();
        Map<Long, List<User>> invitorMap=new HashMap<>();
        Map<Long, List<Space>> spaceIdMap=new HashMap<>();
        if(CollUtil.isNotEmpty(inviteeList)){
            inviteeMap = userService.listByIds(inviteeList).stream().collect(Collectors.groupingBy(User::getId));
        }
        if(CollUtil.isNotEmpty(invitorList)){
            invitorMap = userService.listByIds(invitorList).stream().collect(Collectors.groupingBy(User::getId));
        }
        if(CollUtil.isNotEmpty(spaceIdList)){
            spaceIdMap = spaceService.listByIds(spaceIdList).stream().collect(Collectors.groupingBy(Space::getId));
        }
        //4.然后再封装

        Map<Long, List<User>> finalInviteeMap = inviteeMap;
        Map<Long, List<User>> finalInvitorMap = invitorMap;
        Map<Long, List<Space>> finalSpaceIdMap = spaceIdMap;

        spaceInviteVOSet.forEach(spaceInviteVO -> {
            Long inviterId = spaceInviteVO.getInviterId();
            Long inviteeId = spaceInviteVO.getInviteeId();
            Long spaceId = spaceInviteVO.getSpaceId();
            //填充invitee
            if (inviteeId != null && finalInviteeMap.containsKey(inviteeId)) {
                spaceInviteVO.setInvitee(userService.getUserVO(finalInviteeMap.get(inviteeId).get(0)));
            }
            //填充inviter
            if (inviterId != null && finalInvitorMap.containsKey(inviterId)) {
                spaceInviteVO.setInviter(userService.getUserVO(finalInvitorMap.get(inviterId).get(0)));
            }
            //填充space
            if(spaceId!=null&&finalSpaceIdMap.containsKey(spaceId)){
                Space space = finalSpaceIdMap.get(spaceId).get(0);
                SpaceVO spaceVO=BeanUtil.copyProperties(space, SpaceVO.class);
                spaceInviteVO.setSpaceVO(spaceVO);
            }
            SpaceUser spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, spaceInviteVO.getInviteeId())
                    .one();
            if(spaceUser!=null){
                SpaceUserRoleEnum anEnum = SpaceUserRoleEnum.getEnum(spaceUser.getSpaceRole());
                ThrowUtils.throwIf(ObjectUtil.isNull(anEnum),ErrorCode.NOT_FOUND_ERROR,"角色不存在");
                spaceInviteVO.setUserRole(anEnum.getValue());
            }
        });
        return spaceInviteVOSet;
    }


    @Override
    public List<SpaceInviteVO> getUserSpaceInviteList(SpaceInviteQueryRequest queryRequest, LoginUserVO loginUser) {
        validSpaceInviteQueryRequests(queryRequest, loginUser);
        QueryWrapper<SpaceInvite> queryWrapper = getQueryWrapper(queryRequest);
        System.out.println(queryWrapper);
        List<SpaceInvite> spaceInviteList = this.list(queryWrapper);
        //然后将查询到的spaceInviteList 封装为VO
        List<SpaceInviteVO> spaceInviteVOList = getSpaceInviteVOList(spaceInviteList);
        return spaceInviteVOList;
    }

    /**
     * 校验 SpaceInviteQueryRequest
     *
     * @param request
     */
    public void validSpaceInviteQueryRequests(SpaceInviteQueryRequest request, LoginUserVO loginUser) {
        //校验spaceInviteQueryRequests
        Long inviterId = request.getInviterId();
        //邀请人
        User inviter = userService.getById(inviterId);
        //受邀人
        Long inviteeId = request.getInviteeId();
        ThrowUtils.throwIf(inviterId==null&&inviteeId==null, ErrorCode.NOT_FOUND_ERROR,"邀请人和受邀人不能同时为空");
        if(inviterId!=null){
            if (!inviter.getId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "操作用户和登录用户不一致!");
            }
        }
        Long spaceId = request.getSpaceId();
        Space space = spaceService.getById(spaceId);
        if(space==null&&inviterId!=null){
            //我们可以手动去spaceUser表中查一下
            SpaceUser one = spaceUserService.lambdaQuery().eq(SpaceUser::getUserId, inviterId).one();
            Long spaceId1 = one.getSpaceId();
            Space space1 = spaceService.getById(spaceId1);
            space=space1;
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (space!=null&& !space.getUserId().equals(inviterId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
    }
    /**
     * addSpaceInvite
     * @param spaceInviteAddRequest
     * @param loginUserVO
     */

    @Override
    public Long addSpaceInvite(SpaceInviteAddRequest spaceInviteAddRequest, LoginUserVO loginUserVO) {
        //1.对条件进行判断
        Long inviteeId = spaceInviteAddRequest.getInviteeId();
        Long inviterId = spaceInviteAddRequest.getInviterId();
        Long spaceId = spaceInviteAddRequest.getSpaceId();
        if(ObjectUtil.hasEmpty(inviteeId,inviterId,spaceId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数不能为空");
        }
        //2.然后对三者进行查询
        User invitee = userService.getById(inviterId);
        User inviter = userService.getById(inviterId);
        Space space = spaceService.getById(spaceId);
        if(ObjectUtil.hasEmpty(invitee,inviter,space)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数不能为空");
        }
        //3.然后对space进行校验
        if (!space.getSpaceType().equals(SpaceTypeEnum.TEAM_SPACE.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"非团队空间无法邀请");
        }
        if (!space.getUserId().equals(inviterId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前登录用户与操作用户不符");
        }
        //4.还要看该记录是否已经存在
        SpaceInvite exist = this.lambdaQuery().
                eq(SpaceInvite::getInviterId, inviterId).
                eq(SpaceInvite::getInviteeId, inviteeId).
                eq(SpaceInvite::getSpaceId,spaceId).
                one();

        Boolean flag=ObjectUtil.isNotEmpty(exist) && !(exist.getStatus().equals(SpaceInviteStatusEnum.REJECT.getCode()));
        ThrowUtils.throwIf(flag,ErrorCode.OTHER_ERROR,"该邀请记录已经存在");
        //5.新增SpaceInvite
        SpaceInvite spaceInvite= BeanUtil.copyProperties(spaceInviteAddRequest,SpaceInvite.class);
        spaceInvite.setStatus(SpaceInviteStatusEnum.WAITING.getCode());
        boolean save = this.save(spaceInvite);
        ThrowUtils.throwIf(!save,ErrorCode.OTHER_ERROR,"服务器异常,请联系管理员");
        return spaceInvite.getId();
    }

    /**
     * 更新spaceInvite
     * @param updateRequest
     * @param loginUserVO
     * @return
     */
    @Override
    public boolean updateSpaceInvite(SpaceInviteUpdateRequest updateRequest, LoginUserVO loginUserVO) {
        Long id = updateRequest.getId();
        SpaceInvite spaceInvite = getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceInvite), ErrorCode.NOT_FOUND_ERROR, "邀请记录不存在");
        Long inviteeId = spaceInvite.getInviteeId();
        if (!inviteeId.equals(loginUserVO.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改");
        }
        //然后开始update
        SpaceInvite newSpaceInvite = BeanUtil.copyProperties(updateRequest, SpaceInvite.class);
        boolean res = updateById(newSpaceInvite);
        //如果我同意了 我还要在spaceUser里面添加一条数据
        if(updateRequest.getStatus().equals(SpaceInviteStatusEnum.PASS.getCode())){
            SpaceUser spaceUser = new SpaceUser();
            spaceUser.setSpaceId(spaceInvite.getSpaceId());
            spaceUser.setUserId(inviteeId);
            spaceUser.setSpaceRole(SpaceUserRoleEnum.REVIEWER.getValue());
            boolean save = spaceUserService.save(spaceUser);
            ThrowUtils.throwIf(!save,ErrorCode.OTHER_ERROR,"添加失败");
        };
        return res;
    }
}




