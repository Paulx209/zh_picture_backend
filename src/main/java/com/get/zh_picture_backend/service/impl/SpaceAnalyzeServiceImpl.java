package com.get.zh_picture_backend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.mapper.SpaceMapper;
import com.get.zh_picture_backend.model.dto.space.analyze.*;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.analyze.*;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.SpaceAnalyzeService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl  extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

     /**
     * 校验权限
     */
    private  void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest request, LoginUserVO loginUserVO){
        System.out.println(request);
        //1.首先分离公共空间和所有空间
        if(request.isQueryAll()||request.isQueryPublic()){
            if(!loginUserVO.getUserRole().equals(UserConstant.ADMIN_ROLE)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else{
            //2.判断私有空间
            Long spaceId = request.getSpaceId();
            ThrowUtils.throwIf(ObjectUtil.isNull(spaceId),ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            if (!loginUserVO.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限访问该空间");
            }
        }
    }
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest analyzeRequest, QueryWrapper<Picture> wrapper){
        Long spaceId = analyzeRequest.getSpaceId();
        boolean queryPublic = analyzeRequest.isQueryPublic();
        boolean queryAll = analyzeRequest.isQueryAll();
        if(queryAll){
            //如果要查询所有的空间的话 就不需要添加spaceId这个条件
            return ;
        }else if(queryPublic){
            //如果要查询公共空间的话，那么spaceId就为null
            wrapper.isNull("spaceId");
            return ;
        }else if(ObjectUtil.isNotNull(spaceId)){
            wrapper.eq("spaceId",spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.OTHER_ERROR,"未指定查询范围");
    }

    /**
     * 分析空间使用情况
     * @param request
     * @param loginUser
     * @return
     */
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceAnalyzeRequest request,LoginUserVO loginUser){
        //1.校验参数
        ThrowUtils.throwIf(ObjectUtil.isNull(request),ErrorCode.PARAMS_ERROR);
        //2.如果查询的是公共空间或者是所有空间
        if(request.isQueryAll()||request.isQueryPublic()){
            boolean isAdmin = loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE);
            ThrowUtils.throwIf(!isAdmin,ErrorCode.NO_AUTH_ERROR);
            //2.1查询
            QueryWrapper<Picture> wrapper=new QueryWrapper<>();
            if(request.isQueryPublic()){
                wrapper.isNull("spaceId");
            }
            //添加构造条件picSize
            wrapper.select("picSize");
            //只查询特定的字段，获取到底层baseMapper，然后传入条件构造器 一般只获取select的字段
            List<Object> pictureList = pictureService.getBaseMapper().selectObjs(wrapper);
            Long usedSize = pictureList.stream().mapToLong(size -> size instanceof Long ? (Long) size : 0).sum();
            long usedCount= pictureList.size();

            //2.2封装返回对象
            SpaceUsageAnalyzeResponse response=new SpaceUsageAnalyzeResponse();
            response.setUsedSize(usedSize);
            response.setUsedCount(usedCount);
            //tips:公共图库size和count是没有限制的
            response.setCountUsageRation(null);
            response.setTotalSize(null);
            response.setTotalCount(null);
            response.setSizeUsageRation(null);
            return response;
        }else{
            //3.如果查询的是私有空间
            Long spaceId = request.getSpaceId();
            System.out.println("查询处理的spaceId为:"+spaceId);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR);
            //3.1 权限校验
            if(!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)&&!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //3.1如果存在的话 就要统计信息
            SpaceUsageAnalyzeResponse response=new SpaceUsageAnalyzeResponse();
            Long maxCount = space.getMaxCount();
            Long maxSize = space.getMaxSize();
            Long totalCount = space.getTotalCount();
            Long totalSize = space.getTotalSize();
            Double sizeUsageRation = NumberUtil.round(totalSize * 100.0 / maxSize, 2).doubleValue();
            Double countUsageRation=NumberUtil.round(totalCount*100.0/maxCount,2).doubleValue();
            response.setUsedSize(totalSize);
            response.setUsedCount(totalCount);
            response.setCountUsageRation(countUsageRation);
            response.setSizeUsageRation(sizeUsageRation);
            response.setTotalSize(maxSize);
            response.setTotalCount(maxCount);
            return response;
        }
    }

    /**
     * 统计图片分类情况
     * @param analyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest analyzeRequest, LoginUserVO loginUser) {
        //1.校验权限
        checkSpaceAnalyzeAuth(analyzeRequest,loginUser);
        //2.定义sql
        QueryWrapper<Picture> wrapper=new QueryWrapper<>();
        //2.1 封装条件构造器
        fillAnalyzeQueryWrapper(analyzeRequest,wrapper);
        wrapper.select("category AS category", "count(*) AS count", "sum(picSize) as totalSize").groupBy("category");
        //2.2 获取结果
        List<Map<String, Object>> maps = pictureService.getBaseMapper().selectMaps(wrapper);
        List<SpaceCategoryAnalyzeResponse> result = maps.stream().map(key -> {
            String category =  key.get("category")!=null ? key.get("category").toString():"未分类";
            Long totalCount = ((Number)key.get("count")).longValue();
            Long totalSize = ((Number)key.get("totalSize")).longValue();
            SpaceCategoryAnalyzeResponse response = new SpaceCategoryAnalyzeResponse(category,totalCount,totalSize);
            return response;
        }).collect(Collectors.toList());
        return result;
    }

    public List<SpaceTagsAnalyzeResponse> getSpaceTagsAnalyze(SpaceTagsAnalyzeRequest request,LoginUserVO loginUser){
        //1.check一下当前用户权限和request里面的参数是否相匹配
        checkSpaceAnalyzeAuth(request,loginUser);
        //2.创建条件构造器
        QueryWrapper<Picture> wrapper=new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request,wrapper);
        //3.查询结果
        wrapper.select("tags");
        List<Object> objects = pictureService.getBaseMapper().selectObjs(wrapper);
        List<String> tagsJsonList = objects.stream().filter(ObjectUtil::isNotNull).map(Object::toString).collect(Collectors.toList());
        //4.对结果进行处理 计算出每一个tag的count
        Map<String, Long> tagsCounts = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                 .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //5.封装成返回对象
        return tagsCounts.entrySet().stream().sorted((e1,e2)->Long.compare(e2.getValue(),e1.getValue()))
                .map(entry->new SpaceTagsAnalyzeResponse(entry.getKey(),entry.getValue())).collect(Collectors.toList());
    }

    /**
     * 分析图片大小情况
     * @param request
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest request, LoginUserVO loginUser) {
        checkSpaceAnalyzeAuth(request,loginUser);
        //构造查询条件
        QueryWrapper<Picture> wrapper=new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request,wrapper);
        //查询所有符合条件的图片大小
        wrapper.select("picSize");
        List<Long> picSize = pictureService.getBaseMapper().selectObjs(wrapper).stream().map(size -> ((Number) size).longValue()).collect(Collectors.toList());
        //定义分段范围，使用有序map
        Map<String,Long> sizeRanges=new LinkedHashMap<>();
        sizeRanges.put("<100kb",picSize.stream().filter(size->size<100*1024).count());
        sizeRanges.put("100kb-500kb",picSize.stream().filter(size->(size>=100*1024&&size<500*1024)).count());
        sizeRanges.put("500kb-1Mb",picSize.stream().filter(size->(size<1000*1024)&&size>=500*1024).count());
        sizeRanges.put(">1Mb",picSize.stream().filter(size->size>=1000*1024).count());
        //然后封装为List返回即可
        List<SpaceSizeAnalyzeResponse> responseList = sizeRanges.entrySet().stream().map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        return responseList;
    }

    /**
     * 统计用户各个时段上传情况
     * @param request
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest request, LoginUserVO loginUser) {
        checkSpaceAnalyzeAuth(request, loginUser);
        //1.创建构造器
        QueryWrapper<Picture> wrapper=new QueryWrapper<>();
        Long userId=request.getUserId();
        //用来统计该用户的
        wrapper.eq(userId!=null,"userId",userId);
        fillAnalyzeQueryWrapper(request,wrapper);
        //2.分析维度
        String timeDimension=request.getTimeDimension();
        switch(timeDimension){
            case "day":
                //如果是按天为单位计算该用户的上传图片情况
                wrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') AS period", "count(*) AS count");
                break;
            case "week":
                //如果是按周为单位计算该用户的上传图片情况
                wrapper.select("YEARWEEK(createTime) AS period", "count(*) AS count");
                break;
            case "month":
                //如果是按月为单位计算该用户的上传图片情况
                wrapper.select("DATE_FORMAT(createTime,'%Y-%m') AS period", "count(*) AS count");
                break;
            default:
                break;
        }
        //分组和排序
        wrapper.groupBy("period").orderByAsc("count");
        //查询结果并转换
        List<SpaceUserAnalyzeResponse> responseList = pictureService.getBaseMapper().selectMaps(wrapper).stream().map(result -> {
            String period = result.get("period").toString();
            Long count = ((Number) result.get("count")).longValue();
            return new SpaceUserAnalyzeResponse(period, count);
        }).collect(Collectors.toList());
        return responseList;
    }

    /**
     * 获取空间使用量前N的排行榜
     * @param request
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceTopAnalyze(SpaceTopAnalyzeRequest request, LoginUserVO loginUser) {
        //判断当前用户的role是否是管理员
        if(!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        QueryWrapper<Space> wrapper=new QueryWrapper<>();
        QueryWrapper<Space> resWrapper = wrapper.select("id", "spaceName", "userId", "totalSize").orderByAsc("totalSize")
                .last("LIMIT " + request.getTopN());
        return spaceService.list(resWrapper);
    }
}
