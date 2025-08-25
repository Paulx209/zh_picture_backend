package com.get.zh_picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片 id(用于修改)
     */
    private Long id;

    /**
     * 所属空间id
     */

    private Long spaceId;

    /**
     * url地址(第二种文件上传方式)
     */
    private String fileUrl;

    private String picName;


    private static final long serialVersionUID=1L;
}
