package com.get.zh_picture_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {
    private Integer value;
    private String text;
    private long maxCount;
    private long maxSize;
}
