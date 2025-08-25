package com.get.zh_picture_backend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.get.zh_picture_backend.configuration.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

//一个帮助我们和Cos打交道的类 是基于cosClient帮助我们上传和下载的
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    private final Integer THUMBNAIL_HEIGHT=512;
    private final Integer THUMBNAIL_WEIGHT=512;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        //传入一个key 和 一个文件 然后封装一个ObjectRequest对象
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     * @param key 通过key获取
     * @return
     */
    public COSObject getObject(String key){
        GetObjectRequest getObjectRequest=new GetObjectRequest(cosClientConfig.getBucket(),key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传对象（附带图片信息）
     * 就是往putObjectRequest对象里面添加了一个图片处理对象属性
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        //2.实现上传图片时实时压缩 给picOperations对象传一个rules对象 rules对象中包括我们压缩的格式
        List<PicOperations.Rule> rules=new ArrayList<>();
        //这里我要将key后缀的.jpg换成.webp
        int start = key.lastIndexOf(".");
        String webpKey=key.substring(0,start)+".webp";
        PicOperations.Rule compressRule=new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);

        //如果文件的长度大于2kb
        if(file.length()>1024*2){
            //2.1 添加第二种规则 缩略图处理
            PicOperations.Rule thumbnailRule=new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",THUMBNAIL_HEIGHT,THUMBNAIL_WEIGHT));
            //设置缩略图的id。
            String suffix= StrUtil.isBlank(FileUtil.getSuffix(key)) ? ".png" :FileUtil.getSuffix(key);
            String thumbnailId= FileUtil.mainName(key)+"_thumbnail."+suffix;
            thumbnailRule.setFileId(thumbnailId);
            rules.add(thumbnailRule);
        }

        picOperations.setRules(rules);
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    public void deleteObject(String key){
        String bucketName = cosClientConfig.getBucket();
        try {
            cosClient.deleteObject(bucketName,key);
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
    }
}
