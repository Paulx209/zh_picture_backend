package com.get.zh_picture_backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureRequest implements Serializable {

    private final static long serialVersionUID=1L;
    /**
     * 前段传来的pictureId.
     */
    private Long pictureId;
}
