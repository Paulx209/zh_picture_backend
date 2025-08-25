package com.get.zh_picture_backend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图片大小范围
     */
    private String sizeRange;

    /**
     * 范围中图片数量
     */
    private Long count;
}
