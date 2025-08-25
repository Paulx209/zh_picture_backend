package com.get.zh_picture_backend.manager.websocket.disruptor;

import com.get.zh_picture_backend.manager.websocket.model.PictureEditRequestMessage;
import com.get.zh_picture_backend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 生产者生产事件(比如说要执行什么样的action)
 * 然后消费者拿到事件 进行逻辑业务处理 然后返回一个Response
 * 因此事件就是桥梁
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
