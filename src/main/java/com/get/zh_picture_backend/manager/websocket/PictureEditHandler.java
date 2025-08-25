package com.get.zh_picture_backend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.get.zh_picture_backend.manager.websocket.disruptor.PictureEditEventProducer;
import com.get.zh_picture_backend.manager.websocket.model.PictureEditRequestMessage;
import com.get.zh_picture_backend.manager.websocket.model.PictureEditResponseMessage;
import com.get.zh_picture_backend.model.entity.User;
import com.get.zh_picture_backend.model.vo.UserVO;
import com.get.zh_picture_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket处理器 刚才我们定义的只是一个拦截器 用来在握手之前校验一些参数的 但校验成功之后 我们就要对请求进行处理了
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;
    /**
     * a.首先我们先定义两个变量
     * a.1 pictureEditingUsers：是一个map 用来存储 某张图片正在被某个用户编辑
     * a.2 pictureSessions：也是一个map 用来存储 某张图片有哪些用户进入了编辑
     * ConcurrentHashMap用来保证线程安全 因为这是一个多线程的并发场景
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    //hashmap pictureId  WEBSO
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();


    /**
     * 握手成功之后执行的方法
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //1.首先我们要把这个session添加到pictureSessions中
        User user = (User)session.getAttributes().get("user");
        UserVO userVO = userService.getUserVO(user);
        Long pictureId = (Long)session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId,ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        //2.然后我们要将该信息广播出去 有人来了
        PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String  message =String.format("%s加入了编辑空间",userVO.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUserVO(userVO);
        broadcastToPicture(pictureId,pictureEditResponseMessage);
    }

    @Resource
    private PictureEditEventProducer  pictureEditEventProducer;
    /**
     * 接收客户端消息的方法
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //以前我们的写法就是 收到一个request之后 在方法里面判断是什么type 然后进行处理
        //现在我们只需要将该事件 放到我们的环形缓冲区内即可！
        String payload = message.getPayload();
        PictureEditRequestMessage requestMessage = JSONUtil.toBean(payload, PictureEditRequestMessage.class);
        //获取用户id pcitureid
        Map<String, Object> attributes = session.getAttributes();
        User user=(User) attributes.get("user");
        Long pictureId = (Long)attributes.get("pictureId");
        //交给生产者去生产即可
        pictureEditEventProducer.publishPictureEditEvent(requestMessage,session,user,pictureId);
    }


    /**
     * 处理  <<进入编辑空间>>   的消息
     * @param requestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage requestMessage,WebSocketSession session,User user,Long pictureId) throws IOException {
        //没有用户正在编辑该图片 才能进入编辑
        if(!pictureEditingUsers.containsKey(pictureId)||pictureEditingUsers.get(pictureId).equals(user.getId())){
            //设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId,user.getId());
            //然后广播一条消息
            PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
            String message=String.format("%s正在开始编辑图片",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
            broadcastToPicture(pictureId,pictureEditResponseMessage);
        }
    }

    public void handleEditActionMessage(PictureEditRequestMessage requestMessage,WebSocketSession session,User user,Long pictureId) throws IOException {
        //1.首先要判断操作的类型是否合规
        String editAction = requestMessage.getEditAction();
        PictureEditActionEnum enumByValue = PictureEditActionEnum.getEnumByValue(editAction);
        if(ObjectUtil.isNull(enumByValue)){
            log.info("当前操作类型不合规");
            return ;
        }
        //2.然后判断当前图片
        Long editUserId = pictureEditingUsers.get(pictureId);
        if(editUserId!=null&&editUserId.equals(user.getId())){
            //如果当前该照片的编辑者确定是editUser
            PictureEditResponseMessage responseMessage=new PictureEditResponseMessage();
            String message=String.format("%s执行了%s操作",user.getUserName(),enumByValue.getValue());
            //刚才这里没有添加种类 导致...
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            responseMessage.setEditAction(editAction);
            responseMessage.setMessage(message);
            responseMessage.setUserVO(userService.getUserVO(user));
            broadcastToPicture(pictureId,responseMessage,session);
        }
    }

    public void handleExitEditMessage(PictureEditRequestMessage requestMessage,WebSocketSession session,User user,Long pictureId) throws IOException {
        Long editUserId = pictureEditingUsers.get(pictureId);
        if(editUserId!=null&&editUserId.equals(user.getId())){
            //如果当前编辑的用户是当前登录的用户
            pictureEditingUsers.remove(editUserId);
            //构造退出编辑的消息
            PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
            String message=String.format("%s用户退出了编辑",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
            broadcastToPicture(pictureId,pictureEditResponseMessage);
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long)attributes.get("pictureId");
        User user=(User) attributes.get("user");
        handleExitEditMessage(null,session,user,pictureId);

        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if(CollUtil.isNotEmpty(webSocketSessions)){
            webSocketSessions.remove(session);
            //如果该图片对应的编辑空间里都没人了 那就直接remove掉吧
            if(webSocketSessions.isEmpty()){
                pictureSessions.remove(pictureId);
            }
        }

        //制造消息
        PictureEditResponseMessage responseMessage=new PictureEditResponseMessage();
        String message=String.format("%s用户离开编辑",user.getUserName());
        responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        responseMessage.setMessage(message);
        responseMessage.setUserVO(userService.getUserVO(user));
        broadcastToPicture(pictureId,responseMessage);
    }

    /**
     * 当某一个用户发送了一个编辑的请求的消息  就要将该消息 广播给其他用户
     *
     * @param pictureId 用来制定哪一张图片的
     * @param message   要广播的消息(也就是服务器端返回给客户端的消息 因此是response)
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage message, WebSocketSession mySession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            //因为前端的js长整数可能会损失精度 所以使用Jackson自定义序列化器
            // 在将对象转换为JSON字符的时候 将Long类型转换为String类型
            //1.创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            //2.配置序列化
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            //3.序列化字符串
            String messageStr = objectMapper.writeValueAsString(message);
            //4.定义好要发送的消息
            TextMessage textMessage = new TextMessage(messageStr);
            for (WebSocketSession session : sessionSet) {
                //挨个发消息
                if (mySession != null && session.equals(mySession)) {
                    continue;
                }
                //确保用户是open的
                if (session.isOpen()) {

                    session.sendMessage(textMessage);
                }
            }
        }
    }

    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage message) throws IOException {
        //向所有的用户广播消息
        broadcastToPicture(pictureId,message,null);
    }

}
