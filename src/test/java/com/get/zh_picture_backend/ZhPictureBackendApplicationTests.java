package com.get.zh_picture_backend;

import cn.hutool.core.io.FileUtil;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.get.zh_picture_backend.configuration.CosClientConfig;
import com.get.zh_picture_backend.manager.CosManager;
import com.get.zh_picture_backend.manager.SendSms.api.SendSmsUtils;
import com.get.zh_picture_backend.manager.weather.api.GaoDeApi;
import com.get.zh_picture_backend.mapper.UserMapper;

import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class ZhPictureBackendApplicationTests {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private PictureService pictureService;
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    @Resource
    private SendSmsUtils sendSmsUtils;

    @Test
    void contextLoads() {
        User user = userMapper.selectById(1);
        System.out.println(user);
    }

    @Test
    void testRedis(){
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        opsForValue.set("name","getian");
        String s = opsForValue.get("name");
        System.out.println(s);
        opsForValue.set("name","葛恩涛");
        String s1 = opsForValue.get("name");
        System.out.println(s1);
    }

    @Test
    void test(){
        String str1="image/jpeg";
        final List<String> typeList= Arrays.asList("image/png","image/jpeg","image/jpg","image/webp");
        System.out.println(typeList.contains(str1.toLowerCase()));
    }

    @Test
    void testDeleteFile(){
        String webKey="https://getian1-1343102113.cos.ap-beijing.myqcloud.com/picture/1891681198103601153/2025-02-22_iuzq3vferyh5282s.png";
//        cosManager.deleteObject("picture/1891681198103601153/2025-02-22_iuzq3vferyh5282s.png");
        getPngKey(webKey);
    }
    public String getPngKey(String webpKey){
        int startIndex = webpKey.indexOf("picture");
        String suffix = webpKey.substring(startIndex);
        int i = suffix.lastIndexOf(".");
        String pngKey = suffix.substring(0,i)+".png";
        return pngKey;
    }

    @Test
    void testFileUtils(){
        String str="hello.jpg";
        String suffix = FileUtil.getSuffix(str);
        System.out.println(suffix);
    }

    @Test
    void testColor(){
        GaoDeApi deApi=new GaoDeApi();
    }

    @Test
    void testPhone(){
        String regex="^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";
        String phoneNumber="19935482215";
        boolean matches = phoneNumber.matches(regex);
        System.out.println(matches);
    }

    @Test
    void deleteObj(){
        try {
            String key="picture/1891476269271449602/2025-02-24_8ulj5q1njpcmhqeb.png";
            cosManager.deleteObject(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSms222(){
        SendSmsResponse sendSmsResponse = sendSmsUtils.sendSmsCode("19935482215", "666666");
    }


    @Test
    void testString(){
        Map map=new HashMap();
        map.getOrDefault(1,0);
    }
}
