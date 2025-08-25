package com.get.zh_picture_backend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 统计各个空间的使用情况
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SpaceUsageAnalyzeRequest extends  SpaceAnalyzeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

}
