package com.get.zh_picture_backend.api.aliyunai.generateImage.api;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskRequest;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.CreateImageSynthesisTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.generateImage.model.GetImageSynthesisTaskResponse;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ALiYunApi2 {
    @Value("${aliYunAi.apiKey}")
    private  String apiKey;

    /**
     * 创建任务地址 post请求
     * 请求头header:   X-DashScope-Async: enable;  Authorization:Bearer +key;Content-Type: application/json
     */
    public static final String CREATE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    //创建确认任务是否成功的地址 get请求
    public static final String GET_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务 参数是CreateImageSynthesisTaskRequest
     * @param taskRequest
     * @return
     */
    public CreateImageSynthesisTaskResponse createImageSynthesisTask(CreateImageSynthesisTaskRequest taskRequest){
        log.info("收到的请求参数为:{}",taskRequest);
        String myApiKey="Bearer "+apiKey;
        //构造一个post请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_TASK_URL)
                .header("Authorization", myApiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(taskRequest));
        try(HttpResponse httpResponse = httpRequest.execute()){
            //try的作用就是我们的函数执行结束之后 会自动销毁
            if(!httpResponse.isOk()){
                log.info("请求异常:{}",httpResponse.body());
                throw new BusinessException(ErrorCode.OTHER_ERROR,"创建生成图片任务请求异常");
            }
            //解析返回的httpResponse?
            CreateImageSynthesisTaskResponse taskResponse = JSONUtil.toBean(httpResponse.body(), CreateImageSynthesisTaskResponse.class);
            //只有报错 才会返回code
            String errorCode = taskResponse.getCode();
            if(StrUtil.isNotBlank(errorCode)){
                log.info("请求异常:{}",errorCode);
                throw new BusinessException(ErrorCode.OTHER_ERROR,"创建生成图片任务请求异常");
            }
            //执行到这里 说明没有异常了
            return taskResponse;
        }
    }

    /**
     * 请求我们  ‘文字生图’任务 的结果信息
     * @param taskId
     * @return
     */
    public GetImageSynthesisTaskResponse getImageSynthesisTask(String taskId){
        log.info("taskId为:{}",taskId);
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey);
        try(HttpResponse response = httpRequest.execute()){
            if(!response.isOk()){
                log.info("请求异常:{}",response.body());
                throw new BusinessException(ErrorCode.OTHER_ERROR,"获取任务失败");
            }
            GetImageSynthesisTaskResponse taskResponse = JSONUtil.toBean(response.body(), GetImageSynthesisTaskResponse.class);
            return taskResponse;
        }
    }

}
