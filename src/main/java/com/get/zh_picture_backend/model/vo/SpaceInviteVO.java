package com.get.zh_picture_backend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.get.zh_picture_backend.model.entity.SpaceInvite;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceInviteVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;

    /**
     * 邀请人id
     */
    private Long inviterId;

    /**
     * 受邀者id
     */
    private Long inviteeId;
    /**
     * 团队空间id
     */

    private Long spaceId;

    /**
     * 1：同意  0：未处理 -1：不同意
     */
    private Integer status;

    /**
     * 邀请人
     */
    private UserVO inviter;
    /**
     * 受邀者
     */
    private UserVO invitee;

    /**
     * 当前团队空间
     */
    private SpaceVO spaceVO;
    /**
     *
     */
    private String userRole;
    /**
     * 创建时间
     */
    private Date createTime;

    public static SpaceInviteVO objToVO(SpaceInvite spaceInvite){
        SpaceInviteVO spaceInviteVO = BeanUtil.copyProperties(spaceInvite, SpaceInviteVO.class);
        return spaceInviteVO;
    }

}
