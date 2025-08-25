package com.get.zh_picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColorRequest implements Serializable {
    private final static long serialVersionUID = 1L;
    /**
     * 前端需要返回一个颜色 String类型的
     */
    private String picColor;

    /**
     * 空间id
     */
    private Long spaceId;

}
