package com.get.zh_picture_backend.manager;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.configuration.CosClientConfig;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.dto.file.UploadPictureResult;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile  文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix){
        //1.校验图片
        validPicture(multipartFile);
        //2.构建图片上传地址
        String uuid= RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        //即将要上传的文件名
        String uploadFileName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid,FileUtil.getSuffix(originalFilename));
        //即将要上传的文件的路径
        String uploadPath=String.format("/%s/%s",uploadPathPrefix,uploadFileName);
        //开始上传文件
        File tempFile=null;
        try {
            //创建一个临时的文件 用来将multipartFile的内容写进去
            tempFile = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(tempFile);
            //上传图片 cosManager就是帮我们上传或者下载图片的
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            //获取图片的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //根据图片的信息 封装返回结果
            UploadPictureResult result=new UploadPictureResult();
            int picWidth=imageInfo.getWidth();
            int picHeight=imageInfo.getHeight();
            String picFormat = imageInfo.getFormat();
            double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();
            result.setPicName(FileUtil.mainName(originalFilename));
            result.setPicWidth(picWidth);
            result.setPicHeight(picHeight);
            result.setPicScale(picScale);
            result.setPicFormat(picFormat);
            result.setPicSize(FileUtil.size(tempFile)   );
            result.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deletePath(tempFile);
        }
    }

    /**
     * 上传url
     * @param url
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String url,String uploadPathPrefix){
        //1.校验图片url地址
        validUrl(url);
        //2.构建图片名part1
        String uuid= RandomUtil.randomString(16);
        // String originalFilename = multipartFile.getOriginalFilename();
        // todo 构建图片名part2 但是这个名字不包括后缀名
        String originalFileName=FileUtil.mainName(url);
        //即将要上传的文件名
        String uploadFileName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid,FileUtil.getSuffix(originalFileName));
        //即将要上传文件存储的路径
        String uploadPath=String.format("/%s/%s",uploadPathPrefix,uploadFileName);
        //开始上传文件
        File tempFile=null;
        try {
            //创建一个临时的文件 用来将multipartFile的内容写进去
            tempFile = File.createTempFile(uploadPath, null);
//            multipartFile.transferTo(tempFile);
            HttpUtil.downloadFile(url,tempFile);
            //上传图片 cosManager就是帮我们上传或者下载图片的
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            //获取图片的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //根据图片的信息 封装返回结果
            UploadPictureResult result=new UploadPictureResult();
            int picWidth=imageInfo.getWidth();
            int picHeight=imageInfo.getHeight();
            String picFormat = imageInfo.getFormat();
            double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();
            result.setPicName(FileUtil.mainName(originalFileName));
            result.setPicWidth(picWidth);
            result.setPicHeight(picHeight);
            result.setPicScale(picScale);
            result.setPicFormat(picFormat);
            result.setPicSize(FileUtil.size(tempFile)   );
            result.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deletePath(tempFile);
        }
    }

    /**
     * 删除临时定义的文件
     * @param tempFile
     */
    private static void deletePath(File tempFile) {
        //将临时的文件删除
        if(tempFile !=null){
            boolean isDeleted = tempFile.delete();
            ThrowUtils.throwIf(!isDeleted,ErrorCode.OTHER_ERROR,"文件删除失败");
        }
    }

    /**
     * 校验图片
     * @param multipartFile 图片来源
     */
    public void validPicture(MultipartFile multipartFile){
        ThrowUtils.throwIf(ObjectUtil.isNull(multipartFile), ErrorCode.PARAMS_ERROR);
        //1.校验图片大小
        long size = multipartFile.getSize();
        final long ONE_MB=1024*1024;
        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件大小不能超过2MB");
        //2.校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpg","png","jpeg","webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式不符合要求");
    }

    /**
     * 校验url地址
     * @param url
     */
    public void validUrl(String url){
        //1.首先校验url是否非空
        ThrowUtils.throwIf(StrUtil.isBlank(url),ErrorCode.PARAMS_ERROR,"url为空");
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
            if(response.getStatus()!=HttpStatus.HTTP_OK){
                //如果返回status不是200的话 可能是不支持head请求
                return;
            }
            //5.校验文件的类型
            String type = response.header("Content-Type");
            if(StrUtil.isNotBlank(type)){
                //构造一个数组
                final List<String> typeList=Arrays.asList("image/png","image/jpeg","image/jpg","image/webp");
                ThrowUtils.throwIf(typeList.contains(type.toLowerCase()),ErrorCode.OTHER_ERROR,"文件类型不符合");
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
