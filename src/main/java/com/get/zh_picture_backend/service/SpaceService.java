package com.get.zh_picture_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.get.zh_picture_backend.model.dto.space.SpaceAddRequest;
import com.get.zh_picture_backend.model.dto.space.SpaceQueryRequest;
import com.get.zh_picture_backend.model.vo.SpaceVO;
import com.get.zh_picture_backend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;


import javax.servlet.http.HttpServletRequest;

/**
* @author Paul
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-02-22 19:44:54
*/
public interface SpaceService extends IService<Space> {

    /**
     * 根据前端传来的queryRequest 创建构造器
     *
     * @param queryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest queryRequest);

    /**
     * 将space封装为spaceVO
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 将Page<Space> 转换为 Page<spaceVO>
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 校验space
     * @param space
     */
    void validSpace(Space space,boolean add);

    /**
     * 根据spaceLevel填充maxAccount\maxSize
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建个人空间
     * @param spaceAddRequest
     * @return
     */
    Long createPersonSpace(SpaceAddRequest spaceAddRequest,HttpServletRequest request);
}
