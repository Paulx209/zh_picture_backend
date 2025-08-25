package com.get.zh_picture_backend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 该类是给用户使用的 用户进行编辑；管理员进行更新
 */
@Data
public class SpaceEditRequest implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;
}
