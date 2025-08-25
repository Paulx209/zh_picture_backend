package com.get.zh_picture_backend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserBreakTeamRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long spaceId;

}
