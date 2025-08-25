package com.get.zh_picture_backend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.get.zh_picture_backend.model.entity.Space;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SpaceVO implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间类型 0-私有空间 1-团队空间
     */
    private Integer spaceType;

    /**
     * 空间创始人
     */
    private UserVO createUser;

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
     * 当前用户拥有的权限
     */
    private List<String> permissionList=new ArrayList<>();

    public static SpaceVO objToVO(Space space) {
        if (ObjectUtil.isNull(space)) {
            return null;
        }
        SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
        return spaceVO;
    }

}
