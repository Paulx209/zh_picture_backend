package com.get.zh_picture_backend.manager.weather.api;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.manager.weather.vo.LocationResponse;
import com.get.zh_picture_backend.manager.weather.vo.WeatherResponse;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class GaoDeApi {
    @Value("${gaode.key}")
    private String key;

    private static String getLocationUrl="https://restapi.amap.com/v3/ip?key=%s&output=json";

    private static String getWeatherUrl="https://restapi.amap.com/v3/weather/weatherInfo?output=json";

    private static String getRandomAvatarUrl="https://api.dwo.cc/api/s2tou";

    private String randomAvatarUrl="https://cn.apihz.cn/api/img/apihzimgtx.php";

    private String randomUserName="https://cn.apihz.cn/api/zici/sjwm.php";
    private String apiKey="e0412635d5fe1371e6053918cfdec2b9";
    private Integer type=1;

    //会根据ip进行定位分析
    public LocationResponse getLocationResponse(HttpServletRequest request){
        String realIp = request.getHeader("X-Real-Ip");
        if(StrUtil.isBlank(realIp)){
            realIp=request.getHeader("X-Forwarded-For");
        }
        //"X-Forwarded-For" request.getHeader"X-Real-IP"
        String requestUrl = String.format(getLocationUrl, key);
        HttpRequest httpRequest=null;
        System.out.println("获取到真正的ip为:"+realIp);
        if(!StrUtil.isBlank(realIp)){
            httpRequest=new HttpRequest(requestUrl).form("ip",realIp);
        }else{
            httpRequest=new HttpRequest(requestUrl);
        }
        LocationResponse locationResponse=null;
        try(HttpResponse response = httpRequest.execute()){
            if(!response.isOk()){
                throw new BusinessException(ErrorCode.OTHER_ERROR,"获取当前位置失败");
            }
            String body = response.body();
            locationResponse = JSONUtil.toBean(body, LocationResponse.class);
            if(Integer.valueOf(locationResponse.getStatus()).equals(0)){
                throw new BusinessException(ErrorCode.OTHER_ERROR,"获取当前位置失败");
            }
        }
        return locationResponse ;
    }

    public WeatherResponse getWeatherResponse(LocationResponse locationResponse){
        Map<String,Object> paramsMap=new HashMap<>();
        paramsMap.put("city",locationResponse.getAdcode());
        paramsMap.put("key",key);
        HttpRequest httpRequest=new HttpRequest(getWeatherUrl).form(paramsMap);
        WeatherResponse weatherResponse=null;
        try(HttpResponse response = httpRequest.execute()){
            if(!response.isOk()){
                throw new BusinessException(ErrorCode.OTHER_ERROR,"获取当前天气失败");
            }
            String body = response.body();
            weatherResponse= JSONUtil.toBean(body, WeatherResponse.class);
            if(Integer.valueOf(weatherResponse.getStatus()).equals(0)){
                throw new BusinessException(ErrorCode.OTHER_ERROR,"获取当前位置失败");
            }
        }
        return weatherResponse ;
    }

    public String getRandomAvatar(){
        HttpRequest httpRequest=new HttpRequest(getRandomAvatarUrl);
        String avatarUrl="";
        try(HttpResponse response = httpRequest.execute()){
            if(!response.isOk()){
                throw new BusinessException(ErrorCode.OTHER_ERROR,"获取随机头像失败");
            }
            String body = response.body();
            JSONObject jsonObject=JSONUtil.parseObj(body);
            Object data = jsonObject.get("数据");
            JSONObject jsonObject1 = JSONUtil.parseObj(data);
            avatarUrl = (String) jsonObject1.get("pic");
        }
        return avatarUrl;
    }

    public String getRandomAvatarUrl(){
        Map map=new HashMap();
        map.put("id","10003103");
        map.put("key",apiKey);
        map.put("type",type);
        //头像的风格一共有17种
        map.put("imgtype",RandomUtil.randomInt(0, 17));
        //然后调用api
        HttpRequest request = HttpRequest.get(randomAvatarUrl).form(map);
        String imgUrl="";
        try(HttpResponse response = request.execute()){
            ThrowUtils.throwIf(!response.isOk(),ErrorCode.OTHER_ERROR,"获取随机头像失败");
            String body = response.body();
            ThrowUtils.throwIf(StrUtil.isBlank(body),ErrorCode.NOT_FOUND_ERROR);
            JSONObject jsonObject = JSONUtil.parseObj(body);
            String code = String.valueOf(jsonObject.get("code"));
            ThrowUtils.throwIf(!code.equals("200"),ErrorCode.OTHER_ERROR,"获取随机头像失败");
            imgUrl = (String)jsonObject.get("msg");
        }
        return imgUrl;
    }

    public String getRandomUserName(){
        Map map=new HashMap();
        map.put("id","10003103");
        map.put("key",apiKey);
        map.put("length1",2);
        map.put("length2",5);
        HttpRequest request = HttpRequest.get(randomUserName).form(map);
        try(HttpResponse response = request.execute()){
            ThrowUtils.throwIf(!response.isOk(),ErrorCode.OTHER_ERROR,"获取随机用户名失败");
            String body = response.body();
            ThrowUtils.throwIf(StrUtil.isBlank(body),ErrorCode.NOT_FOUND_ERROR);
            JSONObject jsonObject = JSONUtil.parseObj(body);
            String code = String.valueOf(jsonObject.get("code"));
            ThrowUtils.throwIf(!code.equals("200"),ErrorCode.OTHER_ERROR,"获取随机用户名失败");
            String msg = (String)jsonObject.get("msg");
            return msg;
        }
    }

    public static void main(String[] args) {
        GaoDeApi deApi=new GaoDeApi();
        String randomAvatar = deApi.getRandomAvatar();
        System.out.println(randomAvatar);
    }
}
