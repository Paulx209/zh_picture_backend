package com.get.zh_picture_backend.api.aliyunai.generateImage.model;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreateImageSynthesisTaskRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 模型，例如 "image-out-painting"
     */
    private String model = "wanx2.1-t2i-turbo";

    /**
     * 输入的基本信息，必选
     */
    private Input input;

    /**
     * 图像处理参数，可选
     */
    private Parameters parameters;

    @Data
    @AllArgsConstructor
    public static class Input{
        /**
         * 正向提示词
         */
        private String prompt;

        /**
         * 反向提示词(不希望在文中看到的内容)
         */
        @Alias("negative_prompt")
        private String negativePrompt;

        public Input(String prompt){
            this.prompt=prompt;
        }

    }
    @Data
    public static class Parameters implements Serializable{
        /**
         * 输出图像的分辨率，可选
         */
        private String size;

        /**
         * 生成图片的数量
         */
        private Integer n=1;

        /**
         * 是否开启prompt智能改写，可选
         */
        @Alias("promptExtend")
        private Boolean promptExtend;

        /**
         * 是否开启水印，可选
         */
        private Boolean watermark=false;
    }
}
