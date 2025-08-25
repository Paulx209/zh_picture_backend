package com.get.zh_picture_backend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagsAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tag;
    private Long totalCount;
}
