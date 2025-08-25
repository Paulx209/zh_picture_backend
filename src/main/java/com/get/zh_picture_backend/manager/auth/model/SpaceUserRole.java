package com.get.zh_picture_backend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserRole implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 角色键
     */
    private String key;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 角色拥有的权限
     */
    private List<String> permissions;
    /**
     * 角色描述
     */
    private String description;
}
