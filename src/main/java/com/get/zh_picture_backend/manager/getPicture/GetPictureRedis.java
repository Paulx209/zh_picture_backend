package com.get.zh_picture_backend.manager.getPicture;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.get.zh_picture_backend.model.vo.PictureVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class GetPictureRedis extends GetPictureTemplate{
    @Override
    public Page<PictureVO> getCachedValue(String key, Object cachedSource) {
        StringRedisTemplate redisTemplate=(StringRedisTemplate)cachedSource;
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        String cachedValue=opsForValue.get(key);
        if(cachedValue!=null){
            Page<PictureVO> res = JSONUtil.toBean(cachedValue, Page.class);
            return res;
        }
        return null;
    }

    @Override
    public void setCachedValue(String key, Object cachedSource, String cachedValue) {
        StringRedisTemplate redisTemplate=(StringRedisTemplate)cachedSource;
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        Long expireTime=300+ RandomUtil.randomLong(0,300);
        opsForValue.set(key,cachedValue,expireTime, TimeUnit.SECONDS);
    }
}
