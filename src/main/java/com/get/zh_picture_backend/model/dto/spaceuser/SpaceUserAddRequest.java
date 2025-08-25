package com.get.zh_picture_backend.model.dto.spaceuser;

import com.get.zh_picture_backend.model.enums.SpaceUserRoleEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户
     */
    private Long userId;

    /**
     * 用户权限
     */
    private SpaceUserRoleEnum userRole;
}
