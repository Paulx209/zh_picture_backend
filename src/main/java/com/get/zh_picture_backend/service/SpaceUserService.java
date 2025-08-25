package com.get.zh_picture_backend.service;



import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserAddRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserBreakTeamRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserEditRequest;
import com.get.zh_picture_backend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Paul
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-02-28 18:01:53
*/
public interface SpaceUserService extends IService<SpaceUser> {

    //上传图片 最终返回的是新增空间用户记录的id
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, LoginUserVO loginUser);

    /**
     * 根据前端传来的queryRequest 创建构造器
     *
     * @param queryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest queryRequest);

    /**
     * 将spaceUser封装为spaceUserVO
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);


    /**
     * 校验spaceUser
     * @param spaceUser
     */
    void validSpaceUser(SpaceUser spaceUser,boolean add);



    /**
     * 删除用户空间表中的记录
     * @param deleteRequest
     * @param request
     * @return
     */
    void deleteSpaceUser(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 校验用户权限
     * @param spaceUserQueryRequest
     * @param loginUser
     */

    void checkAuth(SpaceUserQueryRequest spaceUserQueryRequest, LoginUserVO loginUser);

    /**
     * 解除我的团队
     * @param breakTeamRequest
     * @param loginUser
     * @return
     */
    Boolean breakTeam(SpaceUserBreakTeamRequest breakTeamRequest, LoginUserVO loginUser);

    /**
     * 编辑空间用户表
     * @param spaceUser
     * @param loginUser
     * @return
     */
    Boolean editSpaceUser(SpaceUserEditRequest spaceUser, LoginUserVO loginUser);
}
