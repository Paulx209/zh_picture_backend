package com.get.zh_picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {
    private final static Long serialVersionUid=1L;

    /**
     * 搜索关键词
     */
    private String searchText;
    /**
     * 抓取数量
     */
    private Integer count=10;

    /**
     * 抓取到那个空间id里面
     */
    private Long spaceId;

    /**
     *批量抓取名字的前缀。
     */
    private String namePrefix;
}
