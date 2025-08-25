package com.get.zh_picture_backend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {
    private final static String pathPrefix="https://graph.baidu.com/upload?uptime=";
    private final static String token ="1740458013286_1740469169593_AwO3kNwLE3I6G0PuPSl2kHX74feJVGVk6h8kzI+NmtJPJqj/CBULmwwSUXNJGRBn53GXf4oOA76mwztstYSYATNqRZX8Jyf9x0giVg043Ma56EGEWNe+fJeKFiKTRLW3yRv76zRQ5xxSjxhFYA6CfAXqcHlVsqaN0K+nlIc0kgEOlg2KfPx2tQP7aaUJRFNlxKGb12ydgVr9PrQ/SwMq/65pE5dSDNFHeWeGFrH7S1j4cggGo4qnN0NhcnayrGNdrLvkuxEoGIBO9DC1DAMvK6xMMnugX/qnxD9WOhqJ9KI5YdGwV6AM7QuKM/Li15PCrhto3NxXE2ebGo3Hw8v1/45Ftp8JJMo1wKx2xOF3k75lSeByy/I0fs+PJiiQcPyzQ8IC+iH3uvxse+bbw6PuU4aKvDrGOVvR0W8nkct/ONmZTdzY7/6p2a5uFJzuDRCQ";
    private final static String KEY="Acs-Token";

    /**
     *
     * @param imgUrl 以图搜图的第一个图
     * @return
     */
    public static String getImagePageUrl(String imgUrl){
        //1.准备请求参数
        Map<String,Object> formData=new HashMap<>();
        formData.put("image",imgUrl);
        formData.put("tn","pc");
        formData.put("from","pc");
        formData.put("image_source","PC_UPLOAD_URL");
        //获取当前时间戳
        Long currentTime= System.currentTimeMillis();
        //拼接请求地址
        String requestUrl=pathPrefix+currentTime;
        String searchResultUrl = null;
        try {
            HttpResponse response=HttpRequest.post(requestUrl).form(formData).header(KEY,token).timeout(5000).execute();
            if(HttpStatus.HTTP_OK!=response.getStatus()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }
            //解析响应值 返回的是一个json串
            String responseBody = response.body();
            //将json串解析成map
            Map result = JSONUtil.toBean(responseBody, Map.class);
            if(result==null||!Integer.valueOf(0).equals(result.get("status"))){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"调用接口失败");
            }
            //然后获取其中的data
            Map<String,Object> data = (Map<String,Object>)result.get("data");
            //获取到一个url地址 访问该url 可以获取到存储 以图搜图的第二个图的内容
            String rawUrl=(String) data.get("url");
            System.out.println("raw:" +rawUrl);
            //对rawUrl进行解码 可能有乱码？
            searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            //如果URL为空
            ThrowUtils.throwIf(StrUtil.isBlank(searchResultUrl), ErrorCode.OTHER_ERROR,"未返回有效结果");
            return searchResultUrl;
        } catch (BusinessException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        log.info("开始测试以图搜图第一个接口");
        String path="https://inews.gtimg.com/om_bt/OdjjDdm3QxgQuVPKxW5O6U9Bn95Oe1QB1fGHpZnhx5YJgAA/641";
        String imagePageUrl = getImagePageUrl(path);
        System.out.println("哈哈"+imagePageUrl);
    }

}
