package com.get.zh_picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

//实现接口 来实现序列化
@Data
public class PictureEditByBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片id列表
     */
    private List<Long> pictureIdList;

    /**
     * 图片姓名
     */
    private String name;
    /**
     * 图片的tags
     */
    private List<String> tags;
    /**
     * 图片的分类
     */
    private String category;

    /**
     * 空间id
     */
    private Long spaceId;


}
