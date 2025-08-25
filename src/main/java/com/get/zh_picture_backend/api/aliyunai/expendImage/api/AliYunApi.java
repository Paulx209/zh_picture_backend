package com.get.zh_picture_backend.api.aliyunai.expendImage.api;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONUtil;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskRequest;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.CreateOutPaintingTaskResponse;
import com.get.zh_picture_backend.api.aliyunai.expendImage.model.GetOutPaintingTaskResponse;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class AliYunApi {

    @Value("${aliYunAi.apiKey}")
    private  String apiKey;

    //创建任务地址
    public static final String CREATE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    //创建确认任务是否成功的地址
    public static final String GET_TASK_URL = " https://dashscope.aliyuncs.com/api/v1/tasks/%s";


    /**
     * 发送请求 用来创建一个任务 并返回一个任务id
     * @param myRequest
     * @return
     */
    public  CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest myRequest) {
        //1.校验参数---另外一种写法
        Optional.ofNullable(myRequest).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "扩图参数为空"));
        //2.定义请求
        String myApiKey="Bearer "+apiKey;
        System.out.println("apiKey:"+apiKey);
        System.out.println("myApiKey:"+myApiKey);
        HttpRequest httpRequest = HttpRequest.post(CREATE_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(myRequest));

        //3.发送请求
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.info("请求异常:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败");
            }
            //4.解析结果
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.info("AI扩图失败，errorCode:{},errorMessage:{}", errorCode,errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图接口响应异常");
            }
            return response;
        }
    }
    public  GetOutPaintingTaskResponse getOutPaintingTask(String taskId){
        //1.校验参数
        if(StrUtil.isEmpty(taskId)){
            throw new BusinessException(ErrorCode.OTHER_ERROR,"AI扩图任务id为空");
        }
        //Optional.ofNullable(taskId).orElseThrow(()->new BusinessException(ErrorCode.PARAMS_ERROR,"任务id为空"));
        //2.发送请求
        try(HttpResponse httpResponse=HttpRequest.
                get(String.format(GET_TASK_URL,taskId)).
                header(Header.AUTHORIZATION,"Bearer "+apiKey).
                execute()){
            if(!httpResponse.isOk()){
                log.info("请求异常:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查找任务id失败");
            }
            //3.解析结果
            GetOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
            return response;
        }
    }
}
