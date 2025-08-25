package com.get.zh_picture_backend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.configuration.CosClientConfig;
import com.get.zh_picture_backend.manager.CosManager;
import com.get.zh_picture_backend.model.dto.file.UploadPictureResult;

import com.get.zh_picture_backend.utils.ColorSimilarUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;


    /**
     * 上传图片模板方法
     * @param inputSource  文件来源 multipartFile  /  url
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix){
        //1.校验图片
        validPicture(inputSource);
        //2.构建图片上传地址
        String uuid= RandomUtil.randomString(16);
        // 如果是url上传的 后缀没有.jpg/png ; 如果是multipartFile上传的 后缀有.jpg/png
        String originalFilename = getOriginFileName(inputSource);
        //即将要上传的文件名
        String uploadFileName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, FileUtil.getSuffix(originalFilename));
        //即将要上传的文件的路径 这里的前缀就是区分是公共空间还是space个人空间
        String uploadPath=String.format("/%s/%s",uploadPathPrefix,uploadFileName);
        //开始上传文件
        File tempFile=null;
        try {
            //创建一个临时的文件 用来将multipartFile的内容写进去
            tempFile = File.createTempFile(uploadPath, null);
            //将图片的内容 写入我们创建的临时文件中
            processFile(inputSource,tempFile);
            //上传图片 cosManager就是帮我们上传或者下载图片的  传入的是一个路径,临时文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            //获取图片的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            //获取图片处理集
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            if(CollUtil.isNotEmpty(objectList)){
                //获取的是Webp压缩图信息 (下标和对应的规则数一致)
                CIObject ciObject = objectList.get(0);
                CIObject thumbCiObject=ciObject;
                if(objectList.size()>=2){
                    //获取的是缩略图信息
                    thumbCiObject = objectList.get(1);
                    //封装压缩图返回结果
                    uploadPictureResult = getUploadPictureResult(originalFilename, ciObject, thumbCiObject);
                    //todo 将color的格式转换
                    uploadPictureResult.setPicColor(ColorSimilarUtils.toHexColor(imageInfo.getAve()));
                    return uploadPictureResult;
                }
            }
            //根据图片的信息 封装返回结果
            uploadPictureResult = getUploadPictureResult(originalFilename, uploadPath, tempFile, imageInfo);
            //todo 将color的格式转换
            uploadPictureResult.setPicColor(ColorSimilarUtils.toHexColor(imageInfo.getAve()));
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deletePath(tempFile);
        }
    }

    /**
     * 处理文件
     * @param inputSource
     */
    public abstract void processFile(Object inputSource,File tempFile);

    /**
     * 获取文件名称
     * @param inputSource
     */
    public abstract String getOriginFileName(Object inputSource);

    /**
     * 校验图片来源
     * @param inputSource
     */
    public abstract void  validPicture(Object inputSource);



    /**
     * 删除临时定义的文件
     * @param tempFile
     */
    private static void deletePath(File tempFile) {
        //将临时的文件删除
        if(tempFile !=null){
            boolean isDeleted = tempFile.delete();
            ThrowUtils.throwIf(!isDeleted, ErrorCode.OTHER_ERROR,"文件删除失败");
        }
    }

    private UploadPictureResult getUploadPictureResult(String originalFilename, String uploadPath, File tempFile, ImageInfo imageInfo) {
        UploadPictureResult result=new UploadPictureResult();
        int picWidth= imageInfo.getWidth();
        int picHeight= imageInfo.getHeight();
        String picFormat = imageInfo.getFormat();
        double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();
        result.setPicName(FileUtil.mainName(originalFilename));
        result.setPicWidth(picWidth);
        result.setPicHeight(picHeight);
        result.setPicScale(picScale);
        result.setPicFormat(picFormat);
        result.setPicSize(FileUtil.size(tempFile)   );
        result.setUrl(cosClientConfig.getHost()+"/"+ uploadPath);
        return result;
    }

    private UploadPictureResult getUploadPictureResult(String originalFileName,CIObject ciObject,CIObject thumbCiObject){
        UploadPictureResult result=new UploadPictureResult();
        int picWidth= ciObject.getWidth();
        int picHeight= ciObject.getHeight();
        String picFormat = ciObject.getFormat();
        double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();
        result.setPicName(FileUtil.mainName(originalFileName));
        result.setPicWidth(picWidth);
        result.setPicHeight(picHeight);
        result.setPicScale(picScale);
        result.setPicFormat(picFormat);
        result.setPicSize(ciObject.getSize().longValue());
        //设置图片为压缩后的地址
        result.setUrl(cosClientConfig.getHost()+"/"+ ciObject.getKey());
        //设置压缩图url
        String key = thumbCiObject.getKey();
        result.setThumbnailUrl(cosClientConfig.getHost()+"/"+key);
        return result;
    }
}
