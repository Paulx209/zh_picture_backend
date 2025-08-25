package com.get.zh_picture_backend.manager.websocket.model;

import com.get.zh_picture_backend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage implements Serializable {
    /**
     * 消息类型 例如 INFO ERROR ENTER_EDIT EXIT_EDIT EDIT_ACTION
     * 消息 错误 进入编辑 退出编辑 编辑动作
     */
    private String type;

    /**
     * 消息
     */

    private String message;

    /**
     * 编辑动作
     */

    private String editAction;

    /**
     * 用户信息
     */
    private UserVO userVO;
}
