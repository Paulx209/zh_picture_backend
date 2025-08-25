package com.get.zh_picture_backend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.get.zh_picture_backend.annotation.AuthCheck;
import com.get.zh_picture_backend.api.aliyunai.expendImage.api.AliYunApi;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.GetOutPaintingTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.generateImage.api.ALiYunApi2;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.GetImageSynthesisTaskResponse;
import com.get.zh_picture_backend.api.imagesearch.ImageSearchApiFacade;
import com.get.zh_picture_backend.api.imagesearch.model.ImageSearchResult;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.manager.auth.SpaceUserManager;
import com.get.zh_picture_backend.manager.auth.StpKit;
import com.get.zh_picture_backend.manager.auth.annotation.SaSpaceCheckPermission;
import com.get.zh_picture_backend.manager.auth.constant.SpaceUserPermissionConstant;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.enums.PictureReviewStatusEnum;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.manager.getPicture.GetPictureRedis;
import com.get.zh_picture_backend.manager.getPicture.GetPictureTemplate;
import com.get.zh_picture_backend.model.dto.picture.*;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.enums.SpaceLevelEnum;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.PictureVO;
import com.get.zh_picture_backend.model.vo.SpaceLevel;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ResultUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Api(tags = "pictureController")
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private GetPictureRedis  getPictureRedis;

    private final String REDIS_PREFIX="yu-picture";

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunApi aliYunApi;

    @Resource
    private ALiYunApi2 aliYunApi2;

    @Resource
    private SpaceUserManager spaceUserManager;

    @Resource
    private SpaceUserService spaceUserService;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    @PostMapping("/upload")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @ApiOperation("图片上传")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request){
        log.info("图片上传:{},{}",multipartFile,pictureUploadRequest);
        LoginUserVO loginUser = userService.getLoginUser(request);

        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @ApiOperation("url上传")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,HttpServletRequest request){
        log.info("通过url进行上传图片:{}",pictureUploadRequest);
        LoginUserVO loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除picture
     * @param deleteRequest
     * @param request
     * @return
     */
    @DeleteMapping("/delete")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_DELETE)
    @ApiOperation("删除图片")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        log.info("删除图片:{}",deleteRequest.getId());
        pictureService.deletePictureTotal(deleteRequest,request);
        return ResultUtils.success(true);
    }

    /**
     * 更新picture 管理员干的事情
     * @param updateRequest
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("更新图片")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest updateRequest,HttpServletRequest request){
        log.info("更新图片:{}",updateRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(updateRequest)||updateRequest.getId()<=0,ErrorCode.PARAMS_ERROR,"参数为空");
        //判断该id的picture是否存在
        Long id=updateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(oldPicture),ErrorCode.PARAMS_ERROR,"picture不存在");
        //将dto转换成picture
        Picture newPicture= BeanUtil.copyProperties(updateRequest,Picture.class);
        newPicture.setTags(JSONUtil.toJsonStr(updateRequest.getTags()));
        //对数据进行校验
        pictureService.validPicture(newPicture);
        //将picture的审核信息重置
        LoginUserVO loginUser = userService.getLoginUser(request);
        pictureService.fillReviewStatus(newPicture,loginUser);
        //操作数据库
        boolean res = pictureService.updateById(newPicture);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(res);
    }

    /**
     * 根据id查询(仅限管理员查询) 返回原类
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("根据id查询")
    public BaseResponse<Picture> getPictureById(Long id,HttpServletRequest request){
        log.info("根据id查询:{}",id);
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),ErrorCode.NOT_FOUND_ERROR,"该图片不存在");
        if(picture.getSpaceId()!=null){
            LoginUserVO loginUser = userService.getLoginUser(request);
            if(!loginUser.getId().equals(picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限获取私有空间图片");
            }
        }
        return ResultUtils.success(picture);
    }

    /**
     * 根据id查询，返回封装类
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation("根据id查询vo")
    public BaseResponse<PictureVO> getPictureVoById(Long id,HttpServletRequest request){
        log.info("根据id查询:{}",id);
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),ErrorCode.NOT_FOUND_ERROR,"该图片不存在");
        LoginUserVO loginUser = userService.getLoginUser(request);
        Space space=null;
        if(picture.getSpaceId()!=null){
            //说明是私有空间
            space=spaceService.getById(picture.getSpaceId());
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission,ErrorCode.NO_AUTH_ERROR,"没有查看图片的权限");
            //需要判断是不是团队成员
            if(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE_SPACE.getValue())){
                //如果是私有空间
                if(!loginUser.getId().equals(picture.getUserId())){
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限获取私有空间图片");
                }
            }else{
                //需要判断该用户是否是当前空间的成员
                SpaceUser one = spaceUserService.lambdaQuery().eq(SpaceUser::getUserId, loginUser.getId())
                        .eq(SpaceUser::getSpaceId, space.getId()).one();
                ThrowUtils.throwIf(ObjectUtil.isNull(one),ErrorCode.OTHER_ERROR,"当前用户不是该空间的成员");
            }
        }
        List<String> permissionList = spaceUserManager.getPermissionList(space, loginUser);
        //获取封装类
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表(仅管理员可用)
     */
    @PostMapping("/list/page")
    @ApiOperation("分页获取图片列表")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest){
        log.info("分页获取图片列表:{}",pictureQueryRequest);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if(spaceId==null){
            pictureQueryRequest.setNullSpaceId(true);
        }
        Page<Picture> picturePage=new Page<>(pictureQueryRequest.getCurrent(),pictureQueryRequest.getPageSize());
        Page<Picture> pageRes = pictureService.page(picturePage, pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pageRes);
    }



    /**
     * 分页获取图片列表(封装类)
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("分页获取图片列表---vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request){
        log.info("分页获取图片列表---vo:{}",pictureQueryRequest);
        long current=pictureQueryRequest.getCurrent();
        long size=pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if(spaceId==null){
            //如果是公共图库的话 只能看已经pass过的  设置该属性为true就行
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
            pictureQueryRequest.setNullSpaceId(true);
        }else{
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.OTHER_ERROR,"私有空间不存在");
            boolean hasPermission=StpKit.SPACE.hasPermissionOr(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission,ErrorCode.NO_AUTH_ERROR,"没有查看图片的权限");
            //私有空间 需要判断当前用户是否是该空间的用户
            pictureQueryRequest.setNullSpaceId(false);
            LoginUserVO loginUser = userService.getLoginUser(request);
            //判断当前用户 要判断是什么类型的
            if(space.getSpaceType().equals(SpaceTypeEnum.TEAM_SPACE.getValue())){
                //如果是私有空间
                SpaceUser one = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId()).one();
                ThrowUtils.throwIf(ObjectUtil.isNull(one),ErrorCode.OTHER_ERROR,"当前用户不是该空间的成员");
                //如果当前用户是该空间的成员
                pictureQueryRequest.setUserId(null);
            }
        }
        //查询数据库
        Page<Picture> pageRes = pictureService.page(new Page<Picture>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(pageRes, request);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 在用户未登录的时候 也能让他看到这些图片
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/all")
    @ApiOperation("查询所有照片")
    public BaseResponse<List<PictureVO>> queryAll(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request){
        log.info("查询所有图片:{}",pictureQueryRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),ErrorCode.NOT_FOUND_ERROR);
        List<PictureVO> res=pictureService.transferPictureVO(pictureQueryRequest,request);
        return ResultUtils.success(res);
    }


    @PostMapping("/list/page/test/vo/cache")
    @ApiOperation("测试版")
    public BaseResponse<Page<PictureVO>> test(@RequestBody PictureQueryRequest queryRequest,HttpServletRequest request){
        log.info("测试版：分页获取图片列表--redis缓存,{}",queryRequest);
        //todo 这里应该还需要判断
        GetPictureTemplate pictureTemplate=getPictureRedis;
        Page<PictureVO> pictureVOPage = pictureTemplate.listPictureVOByPageCache(redisTemplate, queryRequest, request);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页查询图片+redis缓存
     * @param queryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    @ApiOperation("分页获取图片列表---vo---缓存")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCache(@RequestBody PictureQueryRequest queryRequest,
                                                                  HttpServletRequest request){
        log.info("分页获取图片列表--缓存,{}",queryRequest);
        queryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
        long current=queryRequest.getCurrent();
        long size=queryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        //1.首先从redis缓存中查找数据
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        String jsonStr = JSONUtil.toJsonStr(queryRequest);
        String condition = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String key=String.format(REDIS_PREFIX+"/listPictureVOByPageCache/%s",condition);
        String cachedValue = opsForValue.get(key);
        if(cachedValue!=null){
            //从redis缓存中查询到了数据
            Page<PictureVO> res = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(res);
        }
        Long spaceId = queryRequest.getSpaceId();
        if(spaceId==null){
            queryRequest.setNullSpaceId(true);
        }
        //2.如果没有从redis缓存中查询到 再查询数据库
        Page<Picture> pageRes = pictureService.page(new Page<Picture>(current, size), pictureService.getQueryWrapper(queryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(pageRes, request);
        //存入的数据是序列化之后的
        String value = JSONUtil.toJsonStr(pictureVOPage);
        long expireTime=300+ RandomUtil.randomLong(0,300);
        opsForValue.set(key,value,expireTime, TimeUnit.SECONDS);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页查询图片+本地缓存
     * @param queryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/localCache")
    @ApiOperation("分页获取图片列表---vo---本地缓存")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageLocalCache(@RequestBody PictureQueryRequest queryRequest,
                                                                  HttpServletRequest request){
        log.info("分页获取图片列表--本地缓存,{}",queryRequest);
        queryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
        long current=queryRequest.getCurrent();
        long size=queryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        //1.首先从本地缓存中查找数据
        String jsonStr = JSONUtil.toJsonStr(queryRequest);
        String condition = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String key=String.format("/listPictureVOByPageLocalCache/%s",condition);
        String cachedValue = LOCAL_CACHE.getIfPresent(key);
        if(cachedValue!=null){
            //从redis缓存中查询到了数据
            Page<PictureVO> cachedData = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedData);
        }
        Long spaceId = queryRequest.getSpaceId();
        if(spaceId==null){
            queryRequest.setNullSpaceId(true);
        }
        //2.如果没有从redis缓存中查询到 再查询数据库
        Page<Picture> pageRes = pictureService.page(new Page<Picture>(current, size), pictureService.getQueryWrapper(queryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(pageRes, request);
        //存入的数据是序列化之后的
        String value = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(key,value);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页查询图片+本地缓存
     * @param queryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/cacheAndLocalCache")
    @ApiOperation("分页获取图片列表---vo---本地缓存")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCacheAndLocalCache(@RequestBody PictureQueryRequest queryRequest,
                                                                       HttpServletRequest request){
        log.info("分页获取图片列表--本地缓存,{}",queryRequest);
        queryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
        long current=queryRequest.getCurrent();
        long size=queryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        //1.首先从本地缓存中查找数据
        String jsonStr = JSONUtil.toJsonStr(queryRequest);
        String condition = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String key=String.format("/listPictureVOByPageCacheAndLocalCache/%s",condition);

        String cachedValueByLocal = LOCAL_CACHE.getIfPresent(key);
        if(cachedValueByLocal!=null){
            //从redis缓存中查询到了数据
            Page<PictureVO> cachedData = JSONUtil.toBean(cachedValueByLocal, Page.class);
            return ResultUtils.success(cachedData);
        }
        //2.如果本地缓存中没有 再查redis缓存
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        String cachedValueByRedis = opsForValue.get(key);
        if(StrUtil.isNotEmpty(cachedValueByRedis)){
            //如果查到了 还要更新本地数据
            LOCAL_CACHE.put(key,cachedValueByRedis);
            Page<PictureVO> cachedData = JSONUtil.toBean(cachedValueByRedis, Page.class);
            return ResultUtils.success(cachedData);
        }
        Long spaceId = queryRequest.getSpaceId();
        if(spaceId==null){
            queryRequest.setNullSpaceId(true);
        }
        //3.如果没有从redis缓存中查询到 再查询数据库
        Page<Picture> pageRes = pictureService.page(new Page<Picture>(current, size), pictureService.getQueryWrapper(queryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(pageRes, request);
        //存入的数据是序列化之后的
        String value = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(key,value);
        opsForValue.set(key,value);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片是用户干的事情
     * @param editRequest
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    @ApiOperation("编辑图片")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest editRequest,HttpServletRequest request){
        log.info("编辑图片:{}",editRequest);
        Boolean res=pictureService.editPicture(editRequest,request);
        ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"更新失败");
        return ResultUtils.success(true);
    }

    @GetMapping("/get/tags_category")
    @ApiOperation("获取分类和组件")
    public BaseResponse<PictureTagCategory> getTagCategory(){
        log.info("获取分类和组件选项");
        PictureTagCategory pictureTagCategory=pictureService.getTagCategory();
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @ApiOperation("审核上传图片")
    //只有管理员具有审核的功能
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest reviewRequest,HttpServletRequest request){
        log.info("审核上传图片:{}", reviewRequest);
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest),ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        pictureService.reviewPictureRequest(reviewRequest,loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取图片
     * @param batchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @ApiOperation("批量抓取图片")
    public BaseResponse<Integer> uploadPictureBatch(@RequestBody PictureUploadByBatchRequest batchRequest,HttpServletRequest request){
        log.info("批量抓取图片:{}",batchRequest);
        LoginUserVO loginUser = userService.getLoginUser(request);
        Integer uploadPictureCount = pictureService.uploadPictureByBatch(batchRequest, loginUser);
        return  ResultUtils.success(uploadPictureCount);
    }


    @GetMapping("/list/level")
    @ApiOperation("获取私有空间等级")
    public BaseResponse<List<SpaceLevel>>  listSpaceLevel(){
        log.info("获取私有空间等级:{}");
        List<SpaceLevel> res = Arrays.stream(SpaceLevelEnum.values()).map(levelEnum -> {
             return new SpaceLevel(levelEnum.getValue(),
                    levelEnum.getText(),
                    levelEnum.getMaxCount(),
                    levelEnum.getMaxSize()
            );
        }).collect(Collectors.toList());
        return ResultUtils.success(res);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    @ApiOperation("以图搜图")
    public BaseResponse<List<ImageSearchResult>> searchPicturesByPicture(@RequestBody SearchPictureByPictureRequest searchPicture){
        log.info("以图搜图:{}",searchPicture);
        ThrowUtils.throwIf(ObjectUtil.isNull(searchPicture),ErrorCode.PARAMS_ERROR,"图片为空");
        Long pictureId = searchPicture.getPictureId();
        Picture picture = pictureService.getById(pictureId);
        System.out.println("获取到的图片信息为:"+picture);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),ErrorCode.NOT_FOUND_ERROR);
        //注意这里的url 选择压缩后的？
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(picture.getThumbnailUrl());
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 颜色搜索
     */
    @PostMapping("/search/color")
    @ApiOperation("颜色搜索")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPicturesByColor(@RequestBody SearchPictureByColorRequest searchPicture,HttpServletRequest request){
        log.info("颜色搜索:{}",searchPicture);
        ThrowUtils.throwIf(ObjectUtil.isNull(searchPicture),ErrorCode.PARAMS_ERROR,"前端传来的参数为空");
        LoginUserVO loginUserVO=userService.getLoginUser(request);
        String picColor = searchPicture.getPicColor();
        Long spaceId = searchPicture.getSpaceId();
        List<PictureVO> pictureVOList = pictureService.getPictureVoByColor(spaceId,picColor,loginUserVO);
        return ResultUtils.success(pictureVOList);
    }

    @PostMapping("/edit/batch")

    @ApiOperation("批量编辑图片")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureBatch(@RequestBody PictureEditByBatchRequest editBatchRequest,HttpServletRequest request){
        log.info("批量编辑图片:{}",editBatchRequest);
        LoginUserVO loginUser = userService.getLoginUser(request);
        //不抛出异常 就说明没问题！
        pictureService.editPicturesByBatch(editBatchRequest,loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @ApiOperation("创建 AI 扩图任务")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        log.info("创建 AI 扩图任务:{}",createPictureOutPaintingTaskRequest);
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    @ApiOperation("查询 AI 扩图任务")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        log.info("查询AI扩图任务:{}",taskId);
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunApi.getOutPaintingTask(taskId);
        System.out.println(task);
        return ResultUtils.success(task);
    }

    /**
     * 创建 '文生图' 任务
     * @param taskRequest 请求参数 taskRequest
     * @param request 请求体request
     * @return
     */
    @PostMapping("/image_synthesis/create_task")
    @ApiOperation("创建文生图任务")
    public BaseResponse<CreateImageSynthesisTaskResponse> createImageSynthesisTask
    (@RequestBody CreateImageSynthesisTaskRequestDto taskRequest, HttpServletRequest request){
        log.info("创建文生图任务:{}",taskRequest);
        LoginUserVO loginUser = userService.getLoginUser(request);
        CreateImageSynthesisTaskResponse response = pictureService.createImageSynthesisTask(taskRequest, loginUser);
        return ResultUtils.success(response);
    }

    @GetMapping("/image_synthesis/get_task")
    @ApiOperation("获取文生图任务")
    public BaseResponse<GetImageSynthesisTaskResponse> getImageSynthesisTask(String taskId){
        log.info("获取文生图任务:{}",taskId);
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetImageSynthesisTaskResponse task = aliYunApi2.getImageSynthesisTask(taskId);
        System.out.println("获取到的参数为:"+task);
        return ResultUtils.success(task);
    }
}
