package com.get.zh_picture_backend.manager.getPicture;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.enums.PictureReviewStatusEnum;
import com.get.zh_picture_backend.model.dto.picture.PictureQueryRequest;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.vo.PictureVO;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

public abstract class GetPictureTemplate {
    /**
     * 分页获取图片信息---缓存版
     */
    @Resource
    private PictureService pictureService;
    public final Page<PictureVO> listPictureVOByPageCache(Object cachedSource,PictureQueryRequest queryRequest, HttpServletRequest request){
        queryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
        long current=queryRequest.getCurrent();
        long size=queryRequest.getPageSize();
        //0.限制爬虫
        ThrowUtils.throwIf(size>20, ErrorCode.PARAMS_ERROR);
        //1.构造存储的key值
        String jsonStr = JSONUtil.toJsonStr(queryRequest);
        String condition = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String key=String.format("/listPictureVOByPageCache/%s",condition);
        //该方法帮助我们从不同的缓存中查询内容
        Page<PictureVO> cachedValue=getCachedValue(key,cachedSource);
        if(!ObjectUtil.isNull(cachedValue))return cachedValue;
        //2.如果没有从redis缓存中查询到 再查询数据库
        Page<Picture> pageRes = pictureService.page(new Page<Picture>(current, size), pictureService.getQueryWrapper(queryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(pageRes, request);
        String value = JSONUtil.toJsonStr(pictureVOPage);
        //3.根据缓存种类的不同 将value放到缓存中
        setCachedValue(key,cachedSource,value);
        return pictureVOPage;
    }
    public abstract Page<PictureVO>  getCachedValue(String key,Object cachedSource);
    public abstract void  setCachedValue(String key,Object cachedSource,String cachedValue);
}
