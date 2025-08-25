package com.get.zh_picture_backend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.get.zh_picture_backend.manager.websocket.model.PictureEditRequestMessage;
import com.get.zh_picture_backend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component
@Slf4j
public class PictureEditEventProducer {
    @Resource
    @Lazy
    Disruptor<PictureEditEvent> disruptor;

    public void publishPictureEditEvent(PictureEditRequestMessage requestMessage, WebSocketSession session, User user,Long pictureId){
        RingBuffer<PictureEditEvent> ringBuffer = disruptor.getRingBuffer();
        long next = ringBuffer.next();
        //获取到一个该next下标存储的东西
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        //这里直接修改的是next下标下存储的那个事件 相当于是一个环形缓冲区 然后每一个里面都放了一个事件 但是事件还没有发布 消费者将就不会收到并且执行
        pictureEditEvent.setPictureId(pictureId);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureEditRequestMessage(requestMessage);
        pictureEditEvent.setSession(session);
        //发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void close(){
        disruptor.shutdown();
    }
}
