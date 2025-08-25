package com.get.zh_picture_backend.model.dto.picture;

import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 扩展参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
