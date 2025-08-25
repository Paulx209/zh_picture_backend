package com.get.zh_picture_backend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceUserVO  implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户信息
     */
    private UserVO userVO;

    /**
     * 空间信息
     */
    private SpaceVO spaceVO;

    public static SpaceUserVO objToVO(SpaceUser spaceUser){
        if(ObjectUtil.isNull(spaceUser)){
            return null;
        }
        SpaceUserVO spaceUserVO = BeanUtil.copyProperties(spaceUser, SpaceUserVO.class);

        return spaceUserVO;
    }

    public static SpaceUser voToObj(SpaceUserVO spaceUserVO){
        if(ObjectUtil.isNull(spaceUserVO)){
            return null;
        }
        SpaceUser spaceUser = BeanUtil.copyProperties(spaceUserVO, SpaceUser.class);

        return spaceUser;
    }
}
