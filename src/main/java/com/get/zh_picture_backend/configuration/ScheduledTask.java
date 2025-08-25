package com.get.zh_picture_backend.configuration;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.get.zh_picture_backend.constant.RedisPrefixConstant;
import com.get.zh_picture_backend.model.dto.picture.PictureQueryRequest;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.vo.PictureVO;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@Slf4j
public class ScheduledTask {
    @Resource
    private PictureService pictureService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private UserService userService;
//    每天的凌晨12点执行一个函数 函数的功能是将数据库中isDelete=1的数据删除掉
    @Scheduled(cron = "0 0 0 * * ?")
    public void execute(){
        log.info("定时查询缓存 缓存预热...");
        PictureQueryRequest request=new PictureQueryRequest();
        request.setSortField("createTime");
        request.setSortOrder("desc");
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(request);
        List<Picture> pictureList = pictureService.list(queryWrapper);
        List<PictureVO> pictureVOList = pictureList.stream().limit(200).map(PictureVO::objToVO).collect(Collectors.toList());
        //收集一下用户的id
        Set<Long> idSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        ThrowUtils.throwIf(CollUtil.isEmpty(idSet), ErrorCode.OTHER_ERROR,"用户id集合为空");
        Map<Long, List<User>> listMap = userService.listByIds(idSet).stream().collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVo->{
            Long userId = pictureVo.getUserId();
            if(listMap.containsKey(userId)){
                pictureVo.setCreateUser(userService.getUserVO(listMap.get(userId).get(0)));
            }
        });
        //获取到所有的pictureVoList之后 我们就可以往里面写了
        String key= RedisPrefixConstant.PUBLIC_PICTURE_CACHE;
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(pictureVOList),1, TimeUnit.DAYS);
    }
}
