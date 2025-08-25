package com.get.zh_picture_backend.model.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {
    private List<String> tagsList;
    private List<String> categoryList;
}
