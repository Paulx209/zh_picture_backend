package com.get.zh_picture_backend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String category;
    private Long  totalCount;
    private Long totalSize;
}
