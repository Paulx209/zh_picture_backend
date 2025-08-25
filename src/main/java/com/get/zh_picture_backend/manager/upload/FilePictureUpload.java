package com.get.zh_picture_backend.manager.upload;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate{
    @Override
    public void processFile(Object inputSource, File tempFile) {
        try {
            MultipartFile multipartFile=(MultipartFile) inputSource;
            multipartFile.transferTo(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOriginFileName(Object inputSource) {
        MultipartFile multipartFile=(MultipartFile)inputSource;
        return  multipartFile.getOriginalFilename();
    }

    @Override
    public void validPicture(Object inputSource) {
        MultipartFile multipartFile=(MultipartFile)inputSource;
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
}
