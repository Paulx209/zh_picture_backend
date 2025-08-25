package com.get.zh_picture_backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 *
 * @TableName space_invite
 */
@TableName(value ="space_invite")
@Data
public class SpaceInvite implements Serializable {
    /**
     * 邀请人关系表
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 邀请者的团队空间
     */
    private Long spaceId;

    /**
     * 1：同意  0：未处理 -1：不同意
     */
    private Integer status;

    /**
     *
     */
    private Date createTime;

    /**
     *
     */
    private Date updateTime;

    /**
     *
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
