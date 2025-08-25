package com.get.zh_picture_backend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间分析公共类
 */

@Data
public class SpaceTopAnalyzeRequest  implements Serializable {

    private Integer topN=10;

    private static final long serialVersionUID = 1L;
}
