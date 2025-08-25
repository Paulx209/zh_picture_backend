package com.get.zh_picture_backend.manager.auth;

import com.get.zh_picture_backend.model.entity.Picture;
import com.get.zh_picture_backend.model.entity.Space;
import com.get.zh_picture_backend.model.entity.SpaceUser;
import lombok.Data;

@Data
public class SpaceUserAuthContext {
    /**
     * 临时参数 不同的id 所对应的id是不同的
     */
    private Long id;

    /**
     * 可以根据spaceId获取到spaceId
     */
    private Long pictureId;

    /**
     * 告诉我们具体是哪一个空间
     */
    private Long spaceId;

    /**
     * 空间用户表id
     */
    private Long spaceUserId;

    /**
     * 图片信息
     */
    private Picture picture;

    /**
     * 空间信息
     */
    private Space space;

    /**
     * 空间用户信息
     */
    private SpaceUser spaceUser;

    @Override
    public String toString() {
        return "SpaceUserAuthContext{" +
                "id=" + id +
                ", pictureId=" + pictureId +
                ", spaceId=" + spaceId +
                ", spaceUserId=" + spaceUserId +
                ", picture=" + picture +
                ", space=" + space +
                ", spaceUser=" + spaceUser +
                '}';
    }
}
