package com.get.zh_picture_backend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.get.zh_picture_backend.manager.websocket.PictureEditHandler;
import com.get.zh_picture_backend.manager.websocket.PictureEditMessageTypeEnum;
import com.get.zh_picture_backend.manager.websocket.model.PictureEditRequestMessage;
import com.get.zh_picture_backend.manager.websocket.model.PictureEditResponseMessage;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 消费者 用来处理接收到的参数
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {
    @Resource
    private PictureEditHandler pictureEditHandler;
    @Resource
    private UserService userService;
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage requestMessage = pictureEditEvent.getPictureEditRequestMessage();
        //获取到消息类别
        String type = requestMessage.getType();
        User user = pictureEditEvent.getUser();
        WebSocketSession session = pictureEditEvent.getSession();
        Long pictureId = pictureEditEvent.getPictureId();

        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.valueOf(type);
        switch(messageTypeEnum){
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(requestMessage,session,user,pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(requestMessage,session,user,pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(requestMessage,session,user,pictureId);
                break;
            default:
                PictureEditResponseMessage responseMessage=new PictureEditResponseMessage();
                responseMessage.setMessage("消息类型错误");
                responseMessage.setUserVO(userService.getUserVO(user));
                responseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(responseMessage)));
                break;
        }
    }
}
