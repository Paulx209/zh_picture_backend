package com.get.zh_picture_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteAddRequest;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteQueryRequest;
import com.get.zh_picture_backend.model.dto.spaceInvite.SpaceInviteUpdateRequest;
import com.get.zh_picture_backend.model.entity.SpaceInvite;
import com.baomidou.mybatisplus.extension.service.IService;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceInviteVO;

import java.util.List;

/**
* @author Paul
* @description 针对表【space_invite】的数据库操作Service
* @createDate 2025-03-09 11:10:43
*/
public interface SpaceInviteService extends IService<SpaceInvite> {
    /**
     * 根据前端传来的queryRequest 创建构造器
     *
     * @param queryRequest
     * @return
     */
    QueryWrapper<SpaceInvite> getQueryWrapper(SpaceInviteQueryRequest queryRequest);

    /**
     * 校验space
     * @param spaceInvite
     */
    void validSpaceInvite(SpaceInvite spaceInvite);

    /**
     * 获取SpaceInviteVOList
     * @param spaceInviteList
     * @return
     */
    List<SpaceInviteVO> getSpaceInviteVOList(List<SpaceInvite> spaceInviteList);

    /**
     * 用户查询自己的邀请记录
     */
    List<SpaceInviteVO> getUserSpaceInviteList(SpaceInviteQueryRequest spaceQueryRequest, LoginUserVO loginUser);

    /**
     * 新增一条数据
     */
    Long addSpaceInvite(SpaceInviteAddRequest spaceInviteAddRequest,LoginUserVO loginUserVO);

    boolean updateSpaceInvite(SpaceInviteUpdateRequest spaceInviteUpdateRequest,LoginUserVO loginUserVO);
}
