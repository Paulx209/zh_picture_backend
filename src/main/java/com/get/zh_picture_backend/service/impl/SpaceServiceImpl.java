package com.get.zh_picture_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.enums.*;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.dto.space.SpaceAddRequest;
import com.get.zh_picture_backend.model.dto.space.SpaceQueryRequest;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceVO;
import com.get.zh_picture_backend.model.vo.UserVO;
import com.get.zh_picture_backend.service.SpaceInviteService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.mapper.SpaceMapper;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Paul
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-02-22 19:44:54
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {
    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceInviteService spaceInviteService;

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(ObjectUtil.isNull(space), ErrorCode.PARAMS_ERROR, "参数为空");
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        //空间类型是否合法 0 私有空间 1 团队空间
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getSpaceTypeEnum(space.getSpaceType());
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceTypeEnum), ErrorCode.PARAMS_ERROR, "空间类型不合法");
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //如果是创建空间
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(ObjectUtil.isNull(enumByValue), ErrorCode.PARAMS_ERROR, "空间等级不合法");
        }
        //如果是修改空间
        //修改数据时，id不能为空，
        if (spaceLevel != null && enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不存在");
        }
        if (StrUtil.isNotEmpty(spaceName) && StrUtil.length(spaceName) > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间姓名过长");
        }
    }

    /**
     * 填充maxSize/maxAccount
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        System.out.println("空间等级为:" + spaceLevel);
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceLevelEnum), ErrorCode.PARAMS_ERROR);
        long maxCount = spaceLevelEnum.getMaxCount();
        long maxSize = spaceLevelEnum.getMaxSize();
        //只有count和size都为空的时候 才能set
        if (space.getMaxCount() == null) {
            space.setMaxCount(maxCount);
        }
        if (space.getMaxSize() == null) {
            space.setMaxSize(maxSize);
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        //首先判断searchText

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        //将spacerType该属性也作为查询条件
        queryWrapper.eq(ObjectUtil.isNotNull(spaceType), "spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(space), ErrorCode.PARAMS_ERROR);
        SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
        LoginUserVO loginUser = userService.getLoginUser(request);
        UserVO userVO = BeanUtil.copyProperties(loginUser, UserVO.class);
        spaceVO.setCreateUser(userVO);
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        //如果为空 直接返回
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVO).collect(Collectors.toList());
        // 1. 存储用户id的Set
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setCreateUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public Long createPersonSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        //1.填充参数默认值 (可能前端有些值就没传过来)
        Space space = BeanUtil.copyProperties(spaceAddRequest, Space.class);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName(SpaceLevelEnum.COMMON.getText());
        }
        if (ObjectUtil.isNull(space.getSpaceLevel())) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (ObjectUtil.isNull(space.getSpaceType())) {
            //如果前端没有声明空间type的话 默认为私有空间
            space.setSpaceType(SpaceTypeEnum.PRIVATE_SPACE.getValue());
        }
        space.setUserId(loginUser.getId());
        //2.校验参数
        this.validSpace(space, true);
        //3.校验成功之后 可以给他fill size和count了
        this.fillSpaceBySpaceLevel(space);
        //3.检验权限 非管理员只能创建普通的个人空间
        if (!(space.getSpaceLevel().equals(SpaceLevelEnum.COMMON.getValue())) && !(loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户无权限创建该等级的空间");
        }
        //4.控制同一个用户只能创建一个个人空间
        //intern():会将字符串放到常量池中,当再次调用该方法时，就查询与该字符串相同的常量 确保lock是唯一的。
        Long id = loginUser.getId();
        //方式一
//        String lock=String.valueOf(id).intern();
//        synchronized (lock){
//            //加一个颗粒度更小的事务管理
//            Long spaceId = transactionTemplate.execute(status -> {
//                //5.1首先判断 该用户是否创建过空间
//                boolean exists = this.lambdaQuery().eq(Space::getUserId, id).exists();
//                ThrowUtils.throwIf(!exists, ErrorCode.OTHER_ERROR, "该用户创建空间数上限");
//                //5.2 写入数据库中
//                boolean save = this.save(space);
//                ThrowUtils.throwIf(!save, ErrorCode.OTHER_ERROR, "数据库添加失败");
//                return space.getId();
//            });
//            //避免NPE
//            return Optional.ofNullable(spaceId).orElse(-1L);
//        }
        //方式二：
        Map<Long, Object> lockHashMap = new ConcurrentHashMap<>();
        Object lock = lockHashMap.computeIfAbsent(id, key -> new Object());
        synchronized (lock) {
            //使用transactionTemplate来进行事务管理
            Long spaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, id)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OTHER_ERROR, "该用户创建该种空间数上限");
                //5.2 写入数据库中
                boolean save = this.save(space);
                //5.3 不仅要在space表中添加一条数据；还要在spaceUser表中添加一条数据
                SpaceUser spaceUser = new SpaceUser();
                spaceUser.setSpaceId(space.getId());
                spaceUser.setUserId(loginUser.getId());
                spaceUser.setSpaceRole(SpaceUserRoleEnum.ADMIN.getValue());
                boolean save1 = spaceUserService.save(spaceUser);
                ThrowUtils.throwIf(!save1, ErrorCode.OTHER_ERROR, "空间成员记录添加失败");
                //todo 添加到数据库中
                ThrowUtils.throwIf(!save, ErrorCode.OTHER_ERROR, "数据库添加失败");
                return space.getId();
            });
            return Optional.ofNullable(spaceId).orElse(-1L);
        }
    }
}





