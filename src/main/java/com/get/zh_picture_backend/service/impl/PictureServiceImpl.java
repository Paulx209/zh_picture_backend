package com.get.zh_picture_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.google.gson.reflect.TypeToken;
import com.get.zh_picture_backend.api.aliyunai.expendImage.api.AliYunApi;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskRequest;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.generateImage.api.ALiYunApi2;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskRequest;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskResponse;
import com.get.zh_picture_backend.common.DeleteRequest;
import com.get.zh_picture_backend.constant.RedisPrefixConstant;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.model.enums.PictureReviewStatusEnum;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.manager.CosManager;
import com.get.zh_picture_backend.manager.upload.FilePictureUpload;
import com.get.zh_picture_backend.manager.upload.FileUrlUpload;
import com.get.zh_picture_backend.manager.upload.PictureUploadTemplate;
import com.get.zh_picture_backend.mapper.PictureMapper;
import com.get.zh_picture_backend.model.dto.file.UploadPictureResult;
import com.get.zh_picture_backend.model.dto.picture.*;
import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.enums.SpaceTypeEnum;
import com.get.zh_picture_backend.model.vo.LoginUserVO;
import com.get.zh_picture_backend.model.vo.PictureVO;
import com.get.zh_picture_backend.model.vo.UserVO;
import com.get.zh_picture_backend.service.PictureService;
import com.get.zh_picture_backend.service.SpaceService;
import com.get.zh_picture_backend.service.SpaceUserService;
import com.get.zh_picture_backend.service.UserService;
import com.get.zh_picture_backend.utils.ColorSimilarUtils;
import com.get.zh_picture_backend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Paul
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-18 20:50:53
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private FileUrlUpload fileUrlUpload;
    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunApi aliYunApi;

    @Resource
    private ALiYunApi2 aLiYunApi2;

    @Resource
    private SpaceUserService spaceUserService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, LoginUserVO loginUser) {
        ThrowUtils.throwIf(ObjectUtil.isNull(inputSource), ErrorCode.PARAMS_ERROR,"图片为空");
        Long pictureId = null;
        //如果pictureUploadRequest不为空
        if (pictureUploadRequest != null) {
            //这里的getId也不一定有值
            pictureId = pictureUploadRequest.getId();
        }
        //校验是否是上传到私有空间中
        Long spaceId = pictureUploadRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);
        if(!ObjectUtil.isNull(space)){
            //必须为当前空间的创建者或者管理员才能操作
//            if(!loginUser.getId().equals(space.getUserId())){
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该用户无权限操作该私有空间");
//            }
            //校验私有空间中account和size是否还有剩余
            ThrowUtils.throwIf(space.getTotalSize()>=space.getMaxSize(),ErrorCode.OTHER_ERROR,"可上传空间容量不足");
            ThrowUtils.throwIf(space.getTotalCount()>=space.getMaxCount(),ErrorCode.OTHER_ERROR,"可上传图片额度不足");
        }

        //如果是更新图片 需要校验图片是否存在 / 以及图片的spaceId和参数中的spaceId是否相同
        if (pictureId != null) {
            //1.判断该图片在数据库中是否存在
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR);
            //2.判断当前用户是否是管理员或者是该图片的创建者
//            if(!oldPicture.getUserId().equals(loginUser.getId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
//                throw new BusinessException(ErrorCode.OTHER_ERROR,"无权限更新图片");
//            }
            //3.判断该图片的spaceId和前端传来的spaceId是否一致
            if(spaceId==null){
                if(oldPicture.getSpaceId()!=null){
                    spaceId= oldPicture.getSpaceId();
                }
            }else{
                ThrowUtils.throwIf(!spaceId.equals(oldPicture.getSpaceId()),ErrorCode.OTHER_ERROR,"图片异常,无法修改");
            }
        }

        //上传图片 得到图片信息 然后封装给PictureVO
        String filePrefix;
        if(spaceId==null){
            filePrefix = String.format("picture/%s", loginUser.getId());
        }else{
            filePrefix=String.format("space/%s",spaceId);
        }
        //todo：根据上传图片的类型来区分
        PictureUploadTemplate pictureUploadTemplate=filePictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate=fileUrlUpload;
        }
        //得到cos返回的图片信息了
        UploadPictureResult pictureResult = pictureUploadTemplate.uploadPicture(inputSource, filePrefix);
        System.out.println("获取的颜色为:"+pictureResult.getPicColor());
        Picture picture = BeanUtil.copyProperties(pictureResult, Picture.class);
        picture.setThumbnailUrl(pictureResult.getThumbnailUrl());
        picture.setPicColor(pictureResult.getPicColor());
        System.out.println("获取到的缩略图地址为:"+picture.getThumbnailUrl());

        //给名字传值
        //a.首先从返回的结果信息中获取图片名称
        String picName=pictureResult.getPicName();
        //b.判断前端有没有传递名称数据
        if(pictureUploadRequest!=null&&StrUtil.isNotEmpty(pictureUploadRequest.getPicName())){
            picName=pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setSpaceId(spaceId);

        picture.setUserId(loginUser.getId());
        //补充审核参数
        fillReviewStatus(picture,loginUser);
        //如果pictureId不为空，表示是更新 否则是新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //开启事务 transactionTemplate
        Long finalSpaceId=spaceId;
        transactionTemplate.execute(status->{
            boolean res = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!res, ErrorCode.OTHER_ERROR, "图片上传失败");
            if(finalSpaceId!=null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize=totalSize+{0}", picture.getPicSize())
                        .setSql("totalCount=totalCount+1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OTHER_ERROR,"更新失败");
            }
            return picture;
        });
        return PictureVO.objToVO(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        //首先判断searchText
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or().like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus),"reviewStatus",reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId),"reviewerId",reviewerId);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage),"reviewMessage",reviewMessage);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceId),"spaceId",spaceId);
        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime),"editTime",startEditTime);
        queryWrapper.lt(ObjectUtil.isNotEmpty(endEditTime),"editTime",endEditTime);

        //通过nullSpaceId是否为true/false 来决定是否把spaceId==null 添加到构造器里面
        queryWrapper.isNull(nullSpaceId,"spaceId");
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO=PictureVO.objToVO(picture);
        Long userId = picture.getUserId();
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user==null,ErrorCode.PARAMS_ERROR,"用户不存在");
        UserVO userVO = userService.getUserVO(user);
        pictureVO.setCreateUser(userVO);
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVO).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setCreateUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 更新或者修改的时候对picture进行校验
     * @param picture
     */
    public void validPicture(Picture picture){
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),ErrorCode.PARAMS_ERROR,"参数为空");
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据时，id不能为空，
        ThrowUtils.throwIf(ObjectUtil.isNull(id),ErrorCode.PARAMS_ERROR,"id不能为空");
        if(StrUtil.isNotBlank(url)){
            ThrowUtils.throwIf(url.length()>1024,ErrorCode.PARAMS_ERROR,"url长度过长");
        }
        if(StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length()>800,ErrorCode.PARAMS_ERROR,"简介过长");
        }
    }

    /**
     * 获取分类、标签集合
     * @return
     */
    @Override
    public PictureTagCategory getTagCategory() {
        List<String> tagsList= Arrays.asList("唯美","古风","校园","萌宠","艺术","时尚","体育","AI");
        List<String> categoryList=Arrays.asList("二次元","风景","艺术","创意","生活","自然","山水");
        PictureTagCategory pictureTagCategory=new PictureTagCategory();
        pictureTagCategory.setCategoryList(categoryList);
        pictureTagCategory.setTagsList(tagsList);
        return pictureTagCategory;
    }

    /**
     * 审核上传图片
     * @param reviewRequest
     * @return
     */
    @Override
    public void reviewPictureRequest(PictureReviewRequest reviewRequest, LoginUserVO loginUser) {
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest),ErrorCode.PARAMS_ERROR);
        //1.先查找图片 是否存在
        long id=reviewRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(oldPicture),ErrorCode.NOT_LOGIN_ERROR);
        //2.校验当前图片的状态是否和即将要审核的一致
        if(reviewRequest.getReviewStatus().equals(oldPicture.getReviewStatus())){
            throw new BusinessException(ErrorCode.OTHER_ERROR,"请勿重复审核");
        }
        //3.更新图片
        Picture updatePicture=BeanUtil.copyProperties(reviewRequest,Picture.class);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean res = this.updateById(updatePicture);
        ThrowUtils.throwIf(!res,ErrorCode.OTHER_ERROR,"更新失败");
        //4.直接给缓存 清空 hhe
        String key= RedisPrefixConstant.PUBLIC_PICTURE_CACHE;
        if(redisTemplate.hasKey(key)){
            Boolean isDelete = redisTemplate.delete(key);
            ThrowUtils.throwIf(!isDelete,ErrorCode.OTHER_ERROR,"缓存删除失败");
        }
    }


    public void fillReviewStatus(Picture picture,LoginUserVO loginUser){
        //1.如果是管理员的话 自动过审
        if(loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        }else{
            //2.如果不是管理员的话 直接将图片状态改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getCode());
        }
    }

    /**
     * 批量抓取图片
     * @param batchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest batchRequest, LoginUserVO loginUser) {
        //searchText不需要校验 (为空也可以搜索 那就是随便搜)
        String searchText = batchRequest.getSearchText();
        Integer count = batchRequest.getCount();
        Long spaceId = batchRequest.getSpaceId();
        //1.1校验数量
        ThrowUtils.throwIf(count>30,ErrorCode.PARAMS_ERROR,"抓取图片数量不能超过30条");
        //1.2校验空间id的真实性 空间id可以不为空
//        ThrowUtils.throwIf(ObjectUtil.isNull(spaceId),ErrorCode.PARAMS_ERROR,"空间id为空");
//        Space space = spaceService.getById(spaceId);
//        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR);
        //2.构造要抓取的地址 使用%s占位符
        String fetchUrl=String.format("https://cn.bing.com/images/search?q=%s",searchText);
        //3.然后使用Jsoup访问该地址 获取document对象 并且判空
        Document document=null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Element div = document.getElementsByClass("dgControl").first();
        if(ObjectUtil.isNull(div)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标签元素为空");
        }
        //4.然后再获取到各个图片的元素 并且对其处理
        Elements elementsList = div.select(".iusc");
        Integer updateCount=0;
        for(Element imgElement:elementsList){
            //4.1 获取图片地址 并对其判空 这个照片是经过处理后的
//            String fileUrl = element.attr("src");
            String dataM=imgElement.attr("m");
            String fileUrl;
            String fileName;
            try{
                JSONObject jsonObject= JSONUtil.parseObj(dataM);
                fileUrl = jsonObject.getStr("murl");
                fileName=jsonObject.getStr("t");
                System.out.println("获取的文件名称为:"+fileName);
            }catch(Exception e){
                log.error("解析图片数据失败",e);
                continue;
            }
            if(StrUtil.isBlank(fileUrl)){
                log.info("当前链接为空,跳过{}",fileUrl);
                continue;
            }
            //4.2 对图片地址进行有效化处理
            int end = fileUrl.indexOf("?");
            String realUrl=fileUrl;
            if(end>-1){
                realUrl=fileUrl.substring(0,end);
            }
            //4.3 可以直接调用接口上传
            try {
                PictureUploadRequest uploadRequest=new PictureUploadRequest();
                uploadRequest.setPicName(fileName);
                uploadRequest.setSpaceId(batchRequest.getSpaceId());
                PictureVO pictureVO = this.uploadPicture(realUrl, uploadRequest, loginUser);
                log.info("照片上传成功:{}",pictureVO.getId());
                updateCount++;
            } catch (Exception e) {
                log.error("图片上传失败:{}",e);
                continue;
            }
            if(updateCount>=count){
                break;
            }
        }
        //5.返回上传成功的图片数量
        return updateCount;
    }

    @Override
    @Async
    public void removePictureById(Long id) {
        Picture oldPicture = getById(id);
        Space space;
        if(oldPicture.getSpaceId()!=null){
            space=spaceService.getById(oldPicture.getSpaceId());
        } else {
            space = null;
        }
        //在controller中已经校验过其他参数了
        Long count = this.lambdaQuery().eq(Picture::getUrl, oldPicture.getUrl()).count();
        if(count>1){
            ThrowUtils.throwIf(true,ErrorCode.OTHER_ERROR,"该图片已经被其他用户上传");
        }
        //开始清理cos存储桶中的数据
        String totalPath=oldPicture.getUrl();
        System.out.println("webpKey:"+getWebKey(totalPath));
        System.out.println("pngKey:"+getPngKey(totalPath));
        System.out.println("thumbnailKey:"+getThumbnailKey(totalPath));

        transactionTemplate.execute(status -> {
            //先删除数据库中的
            boolean res = this.removeById(id);
            if(!res){
                throw new BusinessException(ErrorCode.OTHER_ERROR,"删除失败");
            }
            //a.先删webp结尾的
            cosManager.deleteObject(getWebKey(totalPath));
            //b.再删除png结尾的(存webp的时候 原图和webp的都存储了)
            cosManager.deleteObject(getPngKey(totalPath));
            cosManager.deleteObject(getPngKey(getThumbnailKey(totalPath)));
            //删除完毕之后要恢复容量(如果是个人空间的话)
            if(space!=null){
                boolean update = spaceService.lambdaUpdate().
                        eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize=totalSize-{0}", oldPicture.getPicSize())
                        .setSql("totalCount=totalCount-1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OTHER_ERROR,"额度更新失败");
            }
            return true;
        });
    }

    @Override
    public void deletePictureTotal(DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long id=deleteRequest.getId();
        //1.判断图片是否存在
        Picture picture = this.getById(id);
        if(ObjectUtil.isNull(picture)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        //2.仅本人或者管理员可以删除
        LoginUserVO loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        checkPictureAuth(picture,loginUser);
        //3.操作数据库
        this.removePictureById(id);
    }

    public String getWebKey(String totalPath){
        System.out.println("totalPath:"+totalPath);
        int startIndex = totalPath.indexOf("picture");
        if(startIndex==-1){
            startIndex=totalPath.indexOf("space");
        }
        String suffix = totalPath.substring(startIndex);
        return suffix;
    }
    public String getPngKey(String totalPath){
        int startIndex = totalPath.indexOf("picture");
        if(startIndex==-1){
            startIndex=totalPath.indexOf("space");
        }
        String suffix = totalPath.substring(startIndex);
        int i = suffix.lastIndexOf(".");
        String pngKey = suffix.substring(0,i)+".png";
        return pngKey;
    }

    public String getThumbnailKey(String totalPath){
        int startIndex = totalPath.indexOf("picture");
        if(startIndex==-1){
            startIndex=totalPath.indexOf("space");
        }
        String suffix = totalPath.substring(startIndex);
        int i = suffix.lastIndexOf(".");
        String thumbnailKey = suffix.substring(0,i)+"_thumbnail.webp";
        return thumbnailKey;
    }

    @Override
    public void checkPictureAuth(Picture picture, LoginUserVO loginUser) {
        if(picture.getSpaceId()==null){
            //如果是共有空间的话 只有管理员或者是图片的创始人能操作
            if(!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)&&!picture.getUserId().equals(loginUser.getId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限修改图片");
            }
        }else{
            //如果是私有空间的话
            if(!loginUser.getId().equals(picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限修改个人空间图片");
            }
        }
    }

    @Override
    public Boolean editPicture(PictureEditRequest editRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(editRequest==null||editRequest.getId()<=0,ErrorCode.PARAMS_ERROR);
        //要更新的照片是否存在
        long id= editRequest.getId();
        Picture oldPicture = this.getById(id);
        if(ObjectUtil.isNull(oldPicture)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //转变成Picture类
        Picture newPicture=BeanUtil.copyProperties(editRequest,Picture.class);
        newPicture.setTags(JSONUtil.toJsonStr(editRequest.getTags()));
        newPicture.setEditTime(new Date());
        //校验图片
        this.validPicture(newPicture);
        LoginUserVO loginUser = userService.getLoginUser(request);

        checkPictureAuth(oldPicture,loginUser);
        //补充审核参数
        this.fillReviewStatus(newPicture,loginUser);
        //操作数据库
        LambdaUpdateWrapper<Picture> updateWrapper=new LambdaUpdateWrapper<>();
        updateWrapper.eq(Picture::getId,id);
        boolean res = this.update(newPicture,updateWrapper);
        return res;
    }

    /**
     * 根据color比较picture
     * @param spaceId 私有空间id
     * @param picColor 要进行搜索的颜色
     * @param loginUser 登录的用户
     * @return
     */
    @Override
    public List<PictureVO> getPictureVoByColor(Long spaceId, String picColor, LoginUserVO loginUser) {
        //1.校验参数 spaceId loginUser
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"该私有空间不存在");
//        if(!loginUser.getId().equals(space.getUserId())){
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该用户暂无权限");
//        }
        //2.查询该空间下所有的照片(集合)
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();
        if(CollUtil.isEmpty(pictureList)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"该空间下没有符合条件的图片");
        }
        //3.将我们目标颜色转换为color对象
        Color targetColor = Color.decode(picColor);
        //3.依次和前段传来的picColor进行比较  将比较结果排序返回
        List<Picture> pictures = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
            String hexColor = picture.getPicColor();
            Color color1 = Color.decode( hexColor);
            //similarity就是相似值  该值是越大越好 但是我们排序 要把相似的放到最前面 因此返回一个-
            double similarity = ColorSimilarUtils.calculateSimilarity(targetColor, color1);
            return -similarity;
        })).limit(12).collect(Collectors.toList());

        //4.将Picture转换为PictureVO
        List<PictureVO> res = pictures.stream().map(PictureVO::objToVO).collect(Collectors.toList());
        //5.返回
        return res;
    }

    /**
     * 批量修改图片
     * @param picturesList
     * @param loginUser 用来判断当前空间的创始人
     */
    @Override
    public void editPicturesByBatch(PictureEditByBatchRequest picturesList, LoginUserVO loginUser) {
        List<Long> pictureIdList = picturesList.getPictureIdList();
        Long spaceId = picturesList.getSpaceId();
        String category = picturesList.getCategory();
        String pictureNameRule = picturesList.getName();
        List<String> tags = picturesList.getTags();

        //1.校验参数
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList),ErrorCode.PARAMS_ERROR,"参数为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceId),ErrorCode.PARAMS_ERROR,"空间id为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser),ErrorCode.PARAMS_ERROR,"用户为空");
        //2.校验空间权限
        Space space = spaceService.getById(spaceId);
//        if(!loginUser.getId().equals(space.getUserId())){
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该用户暂无权限");
//        }
        //3.查询
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        //4.批量进行更新分类和标签
        if(CollUtil.isEmpty(pictureList)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"没有符合条件的图片");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        int batchSize=100;
        List<CompletableFuture<Void>> future=new ArrayList<>();
        for(int i=0;i<pictureList.size();i+=100){
            List<Picture> batch=pictureList.subList(i,Math.min(i+batchSize,pictureList.size()));
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                //每一个异步任务 执行100条数据的更新
                batch.forEach((picture) -> {
                    if (StrUtil.isNotEmpty(category)) {
                        picture.setCategory(category);
                    }
                    if (CollUtil.isNotEmpty(tags)) {
                        picture.setTags(JSONUtil.toJsonStr(tags));
                    }
                });
                fillPictureWithNameRule(batch, pictureNameRule);
                boolean updateFlag = updateBatchById(batch);
                if (!updateFlag) {
                    throw new BusinessException(ErrorCode.OTHER_ERROR, "批量更新失败");
                }
            }, executorService);
            future.add(completableFuture);
        }
        CompletableFuture.allOf(future.toArray(new CompletableFuture[0])).join();
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count=1;
        try {
            for(Picture picture:pictureList){
                //遍历每一张图片
                String newName=nameRule.replaceAll("\\{序号}",String.valueOf(count++));
                picture.setName(newName);
            }
        } catch (Exception e) {
            log.error("名称解析错误");
            throw new BusinessException(ErrorCode.OTHER_ERROR,"名称解析错误");
        }
    }

    /**
     * 创建Ai扩展任务请求
     * @param taskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest taskRequest, LoginUserVO loginUser) {
        //0.在controller层中已经对参数进行校验了
        //1.首先校验要扩展图片信息和用户权限
        Long pictureId = taskRequest.getPictureId();
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),ErrorCode.NOT_FOUND_ERROR);
        //2.校验用户权限 如果不是该用户的创建者或者不是管理员身份
        if(!picture.getUserId().equals(loginUser.getId())&&!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该用户没有权限");
        }
        //3.封装请求参数
        CreateOutPaintingTaskRequest request=new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        request.setInput(input);
        BeanUtil.copyProperties(taskRequest,request);
        //这里的参数为null
        System.out.println("创建ai扩展任务的请求:"+request);
        return aliYunApi.createOutPaintingTask(request);
    }

    /**
     * 创建 '文生图' 任务
     * @param taskRequest
     * @param loginUserVO
     * @return
     */
    @Override
    public CreateImageSynthesisTaskResponse createImageSynthesisTask(CreateImageSynthesisTaskRequestDto taskRequest, LoginUserVO loginUserVO) {
        log.info("创建文生图任务:{}",taskRequest);
        //1.校验参数合法性
        String prompt = taskRequest.getPrompt();
        String negativePrompt = taskRequest.getNegativePrompt();
        if(StrUtil.isBlank(prompt)){
            log.info("提示词为空:{}",prompt);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"提示词为空");
        }
        //2.构造参数
        CreateImageSynthesisTaskRequest request=new CreateImageSynthesisTaskRequest();
        CreateImageSynthesisTaskRequest.Input input=new CreateImageSynthesisTaskRequest.Input(prompt,negativePrompt);
        request.setInput(input);
        request.setParameters(taskRequest.getParameters());
        //2.直接调用api生成
        CreateImageSynthesisTaskResponse taskResponse = aLiYunApi2.createImageSynthesisTask(request);
        ThrowUtils.throwIf(ObjectUtil.isNull(taskResponse),ErrorCode.OTHER_ERROR,"任务为空");
        String taskId = taskResponse.getOutput().getTaskId();
        ThrowUtils.throwIf(StrUtil.isBlank(taskId),ErrorCode.NOT_FOUND_ERROR);
        return taskResponse;
    }

    /**
     * 将picture对象转换为vo对象
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public List<PictureVO> transferPictureVO(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        Long spaceId = pictureQueryRequest.getSpaceId();
        pictureQueryRequest.setNullSpaceId(true);
        //1.鉴权
        if(ObjectUtil.isNotNull(spaceId)){
            LoginUserVO loginUser = userService.getLoginUser(request);
            //如果spaceId不为空的话 查询的时候就要将该属性设为false
            pictureQueryRequest.setNullSpaceId(false);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR);
            //判断是私有空间还是公共空间
            if(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE_SPACE.getValue())){
                //1.1如果是私有空间
                ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()),ErrorCode.NO_AUTH_ERROR,"无权限查看该空间信息");
            }else{
                //如果是团队空间
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .one();
                ThrowUtils.throwIf(ObjectUtil.isNull(spaceUser),ErrorCode.NO_AUTH_ERROR,"该用户不属于该团队空间");
            }
        }else {
            //如果是公共空间 必须查找审核成功的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
            if (isPublicSpace(pictureQueryRequest)) {
                //todo 2.1 查询缓存 只查询公共空间的
                String value = redisTemplate.opsForValue().get(RedisPrefixConstant.PUBLIC_PICTURE_CACHE);
                if (StrUtil.isNotBlank(value)) {
                    //2.2 如果缓存查到
                    Type type = new TypeToken<List<PictureVO>>() {
                    }.getType();
                    List<PictureVO> list = JSONUtil.toBean(value, type, true);
                    return list;
                }
            }
        }
        //2.构造查询条件
        QueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        List<Picture> pictureList = list(queryWrapper);
        //3.得到pictureVOList 但是没有封装
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVO).collect(Collectors.toList());
        //4.进行封装
        Set<Long> idSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        ThrowUtils.throwIf(CollUtil.isEmpty(idSet),ErrorCode.NOT_FOUND_ERROR,"图库图片为空");
        Map<Long, List<User>> userListMap = userService.listByIds(idSet).stream().collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVO->{
            Long userId=pictureVO.getUserId();
            User user=null;
            if(userListMap.containsKey(userId)){
                user=userListMap.get(userId).get(0);
            }
            pictureVO.setCreateUser(userService.getUserVO(user));
        });
        //向缓存中写值
        if(isPublicSpace(pictureQueryRequest)){
            //可以只存取三十个？
            List<PictureVO> collect = pictureVOList.
                    stream().
                    sorted(Comparator.comparing(PictureVO::getCreateTime).reversed()).collect(Collectors.toList());
            redisTemplate.opsForValue().set(RedisPrefixConstant.PUBLIC_PICTURE_CACHE,JSONUtil.toJsonStr(collect),1, TimeUnit.DAYS);
        }
        return pictureVOList;
    }

    public boolean isPublicSpace(PictureQueryRequest pictureQueryRequest){
        return pictureQueryRequest.getSpaceId()==null
                &&CollUtil.isEmpty(pictureQueryRequest.getTags())
                &&StrUtil.isEmpty(pictureQueryRequest.getCategory())
                &&StrUtil.isEmpty(pictureQueryRequest.getSearchText());
    }
}




