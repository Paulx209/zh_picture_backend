package com.get.zh_picture_backend.manager.getPicture;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.get.zh_picture_backend.model.vo.PictureVO;

public class GetPictureRedisAndLocalCache extends GetPictureTemplate{
    @Override
    public Page<PictureVO> getCachedValue(String key, Object cachedSource) {

        return null;
    }

    @Override
    public void setCachedValue(String key, Object cachedSource, String cachedValue) {

    }
}
