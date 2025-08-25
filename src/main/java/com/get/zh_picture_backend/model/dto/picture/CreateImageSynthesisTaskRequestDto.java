package com.get.zh_picture_backend.model.dto.picture;

import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreateImageSynthesisTaskRequestDto implements Serializable {

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 反向提示词
     */
    private String negativePrompt;

    private CreateImageSynthesisTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;

}
