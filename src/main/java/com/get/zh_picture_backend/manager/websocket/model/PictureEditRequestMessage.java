package com.get.zh_picture_backend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage implements Serializable {

    /**
     * 消息类型 例如"ENTER_EDIT EXIT_EDIT EDIT_ACTION
     * 进入编辑  退出编辑  正在编辑
     */
    private String type;

    /**
     * 执行的编辑动作 这个是对应EDIT_ACTION来说的 编辑动作可能包含放大 缩小 左旋 右旋
     */
    private String editAction;

//    private List<String> operationList;

    private static final Long serialVersionUID = 1L;
}
