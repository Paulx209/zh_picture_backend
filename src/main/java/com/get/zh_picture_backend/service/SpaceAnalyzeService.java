package com.get.zh_picture_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.get.zh_picture_backend.model.dto.space.analyze.*;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceAnalyzeRequest request, LoginUserVO loginUser);
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest request, LoginUserVO loginUser);
    List<SpaceTagsAnalyzeResponse> getSpaceTagsAnalyze(SpaceTagsAnalyzeRequest request, LoginUserVO loginUser);

    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest request, LoginUserVO loginUser);


    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest request, LoginUserVO loginUser);

    List<Space> getSpaceTopAnalyze(SpaceTopAnalyzeRequest request, LoginUserVO loginUser);
}
