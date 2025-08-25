package com.get.zh_picture_backend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class FileUrlUpload extends PictureUploadTemplate{
    @Override
    public void processFile(Object inputSource, File tempFile) {
        String url=(String) inputSource;
        HttpUtil.downloadFile(url,tempFile);
    }

    @Override
    public String getOriginFileName(Object inputSource) {
        String url = (String) inputSource;
        String originalFileName = FileUtil.mainName(url).length() > 8 ? FileUtil.mainName(url).substring(0, 8) : FileUtil.mainName(url);
        // 如果以.结尾
        if (originalFileName.endsWith(".")) {
            originalFileName = originalFileName + "png";
        }
        return originalFileName;
    }

    @Override
    public void validPicture(Object inputSource) {
        String url=(String) inputSource;
        //1.首先校验url是否非空
        ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR,"url为空");
        //2.校验url的格式是否正常 可以借助URL类
        try {
            new URL(url); //如果格式不正确的话 new的过程中就会抛出异常
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"格式不正确");
        }
        //3.校验http协议
        boolean isValid=url.startsWith("https://") || url.startsWith("http://");
        ThrowUtils.throwIf(!isValid,ErrorCode.PARAMS_ERROR,"仅支持http或https协议");

        HttpResponse response = null;
        try {
            //4.发送Head请求以验证文件是否存在
            response = HttpUtil.createRequest(Method.HEAD, url).execute();
            if(response.getStatus()!= HttpStatus.HTTP_OK){
                //如果返回status不是200的话 可能是不支持head请求
                return;
            }
            //5.校验文件的类型
            String type = response.header("Content-Type");
            if(StrUtil.isNotBlank(type)){
                //构造一个数组
                System.out.println("Content-Type为 :"+type);
                final List<String> typeList= Arrays.asList("image/png","image/jpeg","image/jpg","image/webp");
                ThrowUtils.throwIf(!typeList.contains(type.toLowerCase()),ErrorCode.OTHER_ERROR,"文件类型不符合");
            }
            //6.校验文件的大小
            String contentLengthStr  = response.header("Content-Length");
            if(StrUtil.isNotBlank(contentLengthStr)){
                try {
                    long size = Long.parseLong(contentLengthStr);
                    final Long ONE_MB=1024*1024L;
                    ThrowUtils.throwIf(size>ONE_MB*2,ErrorCode.OTHER_ERROR,"文件大小超过2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.OTHER_ERROR,"文件大小格式错误");
                }
            }
        } finally {
            //释放资源
            if(response!=null){
                response.close();
            }
        }
    }
}
