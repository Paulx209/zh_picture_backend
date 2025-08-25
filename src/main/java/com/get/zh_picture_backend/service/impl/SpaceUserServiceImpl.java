package com.get.zh_picture_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserAddRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserBreakTeamRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserEditRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.enums.SpaceUserRoleEnum;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceUserVO;
import com.get.zh_picture_backend.model.vo.SpaceVO;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.mapper.SpaceUserMapper;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author Paul
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-02-28 18:01:53
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, LoginUserVO loginUser) {
        //1.参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceUserAddRequest), ErrorCode.PARAMS_ERROR,"参数不能为空");
        User user = userService.getById(spaceUserAddRequest.getUserId());
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        Space space = spaceService.getById(spaceUserAddRequest.getSpaceId());
        ThrowUtils.throwIf(ObjectUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        //2.转换格式
        SpaceUser spaceUser = BeanUtil.copyProperties(spaceUserAddRequest, SpaceUser.class);
        boolean res = saveOrUpdate(spaceUser);
        ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"添加或者更新失败");
        return spaceUser.getId();
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserqueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserqueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserqueryRequest.getId();
        Long spaceId = spaceUserqueryRequest.getSpaceId();
        Long userId = spaceUserqueryRequest.getUserId();
        String spaceRole = spaceUserqueryRequest.getSpaceRole();
        //首先判断searchText

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjectUtil.isNotNull(spaceRole),"spaceType",spaceRole);
        return queryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceUser), ErrorCode.PARAMS_ERROR,"参数不能为空");
        SpaceUserVO spaceUserVO = BeanUtil.copyProperties(spaceUser, SpaceUserVO.class);
        //1.往spaceUserVo传递用户信息和空间信息
        Long userId = spaceUser.getUserId();
        User user = userService.getById(userId);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        Long spaceId = spaceUser.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjectUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        spaceUserVO.setUserVO(userService.getUserVO(user));
        spaceUserVO.setSpaceVO(spaceService.getSpaceVO(space,request));
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        System.out.println("spaceUserList:"+spaceUserList);
        //1.判断参数
        ThrowUtils.throwIf(CollUtil.isEmpty(spaceUserList),ErrorCode.PARAMS_ERROR);
        List<SpaceUser> newSpaceUserList = spaceUserList.stream().filter(spaceUser -> {
            Long spaceId = spaceUser.getSpaceId();
            if(spaceId==null)return false;
            Space space = spaceService.getById(spaceId);
            if (ObjectUtil.isNull(space) || space.getSpaceType().equals(SpaceTypeEnum.PRIVATE_SPACE.getValue())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        //2.首先获取到没有user和space的spaceUserVOList
        List<SpaceUserVO> spaceUserVOList=newSpaceUserList.stream().map(SpaceUserVO::objToVO).collect(Collectors.toList());
        //3.获取到userId和spaceId的set 整体查询的效率和性能要比单个查询快
        Set<Long> userIdSet = newSpaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = newSpaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        //4.批量查询用户集合和空间集合
        Map<Long, List<User>> usersMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spacesMap = spaceService.listByIds(spaceIdSet).stream().collect(Collectors.groupingBy(Space::getId));
        //5.遍历循环
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            User user=null;
            if(userIdSet.contains(userId)){
                user=usersMap.get(userId).get(0);
            }
            spaceUserVO.setUserVO(userService.getUserVO(user));
            Long spaceId=spaceUserVO.getSpaceId();
            Space space=null;
            if(spaceIdSet.contains(spaceId)&&CollUtil.isNotEmpty(spacesMap)){
                //这里为空？
                List<Space> spaceList = spacesMap.get(spaceId);
                if(CollUtil.isNotEmpty(spaceList)){
                    space=spacesMap.get(spaceId).get(0);
                    spaceUserVO.setSpaceVO(SpaceVO.objToVO(space));
                }
            }
        });
//        List<SpaceUserVO> collect = spaceUserVOList.stream().filter(spaceUserVO -> ObjectUtil.isNotNull(spaceService.getById(spaceUserVO.getSpaceId()))).collect(Collectors.toList());
        return spaceUserVOList;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser,boolean add) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceUser), ErrorCode.PARAMS_ERROR, "参数为空");
        //1.对用户id和空间id进行校验
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        System.out.println("传递来的参数为:"+spaceUser);
        if(ObjectUtil.hasEmpty(userId,spaceId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果是创建空间
        if (add) {
            User user = userService.getById(userId);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            ThrowUtils.throwIf(ObjectUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        //校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        if(StrUtil.isNotBlank(spaceRole)){
            SpaceUserRoleEnum spaceUserRoleEnum = SpaceUserRoleEnum.getEnum(spaceRole);
            ThrowUtils.throwIf(ObjectUtil.isNull(spaceUserRoleEnum), ErrorCode.PARAMS_ERROR, "空间角色不合法");
        }
    }

    @Override
    public void deleteSpaceUser(DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(deleteRequest),ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        SpaceUser spaceUser = this.getById(id);
        //todo 要判断该用户的权限是否可以删除其他用户
    }


    @Override
    public void checkAuth(SpaceUserQueryRequest spaceUserQueryRequest, LoginUserVO loginUser) {
        //查看当前用户是不是该空间的创始人
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        Long userId = loginUser.getId();
        if(!userId.equals(space.getUserId())){
            throw new BusinessException(ErrorCode.OTHER_ERROR,"没有权限查看空间成员");
        }
    }

    /**
     * 解除我的团队
     * @param breakTeamRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean breakTeam(SpaceUserBreakTeamRequest breakTeamRequest, LoginUserVO loginUser) {
        //1.首先校验该空间是否存在
        Long spaceId = breakTeamRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR,"该空间不存在");
        //2.校验是否有该记录表存在
        SpaceUser one = this.lambdaQuery().eq(SpaceUser::getSpaceId, spaceId).eq(SpaceUser::getUserId, loginUser.getId()).one();
        ThrowUtils.throwIf(ObjectUtil.isNull(one),ErrorCode.NOT_FOUND_ERROR,"该团队空间不存在");
        //3.然后校验该用户是否是该空间的创始人
        Long userId = one.getUserId();
        if(!userId.equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限解散该空间");
        }
        //4. todo 进行删除逻辑 需要加一个事务控制器
        transactionTemplate.execute(status->{
            //4.1 首先先将spaceUser中相关的数据都删除
            List<SpaceUser> list = this.lambdaQuery().eq(SpaceUser::getSpaceId, spaceId).list();
            Set<Long> collect = list.stream().map(SpaceUser::getId).collect(Collectors.toSet());
            boolean res = this.removeBatchByIds(collect);
            ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"解散出现异常，服务器都在挽留你哦");
            //4.2然后要将space删除，以保证用户可以创建一个新的该种类空间
            boolean res1 = spaceService.removeById(spaceId);
            ThrowUtils.throwIf(!res1,ErrorCode.OTHER_ERROR,"解散出现异常，服务器都在挽留你哦");
            //4.3然后将所有和该空间相关联的picture都删除
            List<Picture> pictureList = pictureService.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                    .select(spaceId != null, Picture::getId).list();
            //如果pictureList不为空 才能去删除
            if(CollUtil.isNotEmpty(pictureList)){
                Set<Long> pictureIdSet = pictureList.stream().map(Picture::getId).collect(Collectors.toSet());
                boolean res2 = pictureService.removeByIds(pictureIdSet);
                ThrowUtils.throwIf(!res2,ErrorCode.OTHER_ERROR,"解散出现异常，服务器都在挽留你哦");
            }
            return true;
        });
        return true;
    }

    /**
     * 编辑空间用户表
     * @param spaceUserEditRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, LoginUserVO loginUser) {
        //1.校验参数
        Long spaceId = spaceUserEditRequest.getSpaceId();
        Long userId = spaceUserEditRequest.getUserId();
        if(ObjectUtil.hasEmpty(spaceId,userId)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"请求参数不存在");
        }
        //2.第二步 校验我们要更改userRole
        String spaceRole = spaceUserEditRequest.getSpaceRole();
        SpaceUserRoleEnum anEnum = SpaceUserRoleEnum.getEnum(spaceRole);
        ThrowUtils.throwIf(ObjectUtil.isNull(anEnum),ErrorCode.NOT_FOUND_ERROR,"角色不合法");
        //3.第三步 根据userId和spaceId来查询该条记录
        SpaceUser spaceUser = this.lambdaQuery().eq(SpaceUser::getUserId, userId).eq(SpaceUser::getSpaceId, spaceId).one();
        System.out.println("查询到的spaceUser为:"+spaceUser);
        Space space=spaceService.getById(spaceId);
        //4.第四步 校验该用户是不是该空间的创始人 校验是不是修改管理员的身份 校验是不是要修改成管理员
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限修改用户的role");
        }

        if (spaceUser.getSpaceRole().equals(SpaceUserRoleEnum.ADMIN.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"管理员不能修改自己的角色");
        }

        if(anEnum.equals(SpaceUserRoleEnum.ADMIN)){
            throw new BusinessException(ErrorCode.OTHER_ERROR,"管理员数量已达到上限");
        }
        //5. 第五步 更新
        SpaceUser newSpaceUser = BeanUtil.copyProperties(spaceUserEditRequest, SpaceUser.class);
        newSpaceUser.setId(spaceUser.getId());
        boolean res = this.updateById(newSpaceUser);
        ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"更新过程中出现异常");
        return true;
    }
}




