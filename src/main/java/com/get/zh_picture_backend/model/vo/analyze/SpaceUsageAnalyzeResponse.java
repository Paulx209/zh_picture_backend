package com.get.zh_picture_backend.model.vo.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 使用的空间大小
     */
    private Long usedSize;
    /**
     * 总共的空间大小
     */
    private Long totalSize;
    /**
     * 空间的使用率
     */
    private Double sizeUsageRation;

    /**
     * 使用的空间数量
     */
    private Long usedCount;
    /**
     * 总共的空间数量
     */
    private Long totalCount;

    /**
     * 空间数量的使用率
     */
    private Double countUsageRation;
}
