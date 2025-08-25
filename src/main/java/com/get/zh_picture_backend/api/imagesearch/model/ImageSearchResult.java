package com.get.zh_picture_backend.api.imagesearch.model;

import lombok.Data;

@Data
public class ImageSearchResult {
    /**
     * 缩略图地址？
     */
    private String thumbUrl;
    /**
     * 来源地址
     */
    private String fromUrl;
}
