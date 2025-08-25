package com.get.zh_picture_backend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.model.entity.Picture;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {
    private final Long serialVersionUID = 1L;

    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 所属空间id
     */

    private Long spaceId;

    /**
     * 压缩图url
     */
    private String thumbnailUrl;

    /**
     * 主色调
     */
    private String picColor;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private UserVO createUser;

    /**
     * 当前用户拥有的权限
     */
    private List<String> permissionList=new ArrayList<>();

    public static Picture voToObj(PictureVO pictureVO) {
        if (ObjectUtil.isNull(pictureVO)) {
            return null;
        }
        Picture picture = BeanUtil.copyProperties(pictureVO, Picture.class);
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    public static PictureVO objToVO(Picture picture) {
        if (ObjectUtil.isNull(picture)) {
            return null;
        }
        PictureVO pictureVO = BeanUtil.copyProperties(picture, PictureVO.class);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(),String.class));
        //todo pictureVO中没有传递createUser
        return pictureVO;
    }

}
