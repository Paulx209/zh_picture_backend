package com.get.zh_picture_backend.model.dto.spaceInvite;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceInviteUpdateRequest implements Serializable {
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
     * 1：同意  0：未处理 -1：不同意
     */
    private Integer status;


}
