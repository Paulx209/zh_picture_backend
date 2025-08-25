package com.get.zh_picture_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskResponse;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.model.dto.picture.*;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * @author Paul
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-02-18 20:50:53
 */
public interface PictureService extends IService<Picture> {
    //上传图片 最终返回的是解析结果
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, LoginUserVO loginUser);

    /**
     * 根据前端传来的queryRequest 创建构造器
     *
     * @param queryRequest
     * @return
     */
     QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest queryRequest);

    /**
     * 将picture封装为pictureVO
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 将Page<Picture> 转换为 Page<pictureVO>
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验picture
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 获取分类集合、标签集合
     * @return
     */
    PictureTagCategory getTagCategory();

    /**
     * 审核上传图片
     * @param reviewRequest
     * @return
     */

    /**
     * 审核照片的请求
     * @param reviewRequest
     * @param loginUser
     */
    void reviewPictureRequest(PictureReviewRequest reviewRequest, LoginUserVO loginUser);

    /**
     * 给上传的图片添加审核状态
     * @param picture
     * @param loginUser
     */
    void fillReviewStatus(Picture picture,LoginUserVO loginUser);

    /**
     * 批量抓取图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,LoginUserVO loginUser);

    /**
     * 从cos存储库中/数据库中删除图片
     * @param id
     * @return
     */
    void removePictureById(Long id);

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    void deletePictureTotal(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * edit图片 (一般是用户干的事情)
     * @param editRequest
     * @param request
     * @return
     */
    Boolean editPicture(PictureEditRequest editRequest, HttpServletRequest request);

    void checkPictureAuth(Picture picture,LoginUserVO loginUser);

    /**
     * 在对个人私有空间进行颜色搜图的时候 需要用到
     * @param spaceId 私有空间id
     * @param picColor 要进行搜索的颜色
     * @param loginUser 登录的用户
     * @return
     */
    List<PictureVO> getPictureVoByColor(Long spaceId,String picColor,LoginUserVO loginUser);

    /**
     * 批量修改图片信息
     * @param pictureEditByBatchRequest
     * @param loginUser 用来判断当前空间的创始人
     */
    void editPicturesByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, LoginUserVO loginUser);

    /**
     * 创建ai扩展任务请求
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, LoginUserVO loginUser);

    /**
     * 创建 '文生图' 任务
     * @param request
     * @param loginUserVO
     * @return
     */
    CreateImageSynthesisTaskResponse createImageSynthesisTask(CreateImageSynthesisTaskRequestDto request,LoginUserVO loginUserVO);


    /**
     * 将picture对象转换为pictureVo
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    List<PictureVO> transferPictureVO(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);
}
