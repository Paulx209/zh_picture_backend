package com.get.zh_picture_backend.manager.getPicture;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.get.zh_picture_backend.model.vo.PictureVO;
import org.springframework.stereotype.Service;

@Service
public class GetPictureLocalCache extends GetPictureTemplate{
    @Override
    public Page<PictureVO> getCachedValue(String key, Object cachedSource) {
        Cache<String, String> LOCAL_CACHE=(Cache<String, String>)cachedSource;
        String cachedValue = LOCAL_CACHE.getIfPresent(key);
        if(cachedValue!=null){
            Page<PictureVO> res = JSONUtil.toBean(cachedValue,Page.class);
            return res;
        }
        return null;
    }

    @Override
    public void setCachedValue(String key, Object cachedSource, String cachedValue) {
        Cache<String, String> LOCAL_CACHE=(Cache<String, String>)cachedSource;
        LOCAL_CACHE.put(key,cachedValue);
    }
}
