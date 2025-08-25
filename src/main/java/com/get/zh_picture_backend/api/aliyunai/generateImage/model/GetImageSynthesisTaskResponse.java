package com.get.zh_picture_backend.api.aliyunai.generateImage.model;


import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetImageSynthesisTaskResponse {

    private String requestId;

    private OutPut output;

    private Usage usage;

    @Data
    public static class Usage{
        @Alias("image_count")
        private Integer imageCount;
    }


    @Data
    public static class OutPut{
        /**
         * 任务ID
         */
        @Alias("task_id")
        private String taskId;

        /**
         * 任务状态
         * <ul>
         *     <li>PENDING：排队中</li>
         *     <li>RUNNING：处理中</li>
         *     <li>SUSPENDED：挂起</li>
         *     <li>SUCCEEDED：执行成功</li>
         *     <li>FAILED：执行失败</li>
         *     <li>UNKNOWN：任务不存在或状态未知</li>
         * </ul>
         */
        @Alias("task_status")
        private String taskStatus;

        /**
         * 提交时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String submitTime;

        /**
         * 调度时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String scheduledTime;

        /**
         * 结束时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String endTime;

        /**
         * 任务结果列表 数组
         */
        private List<Results> results;

        /**
         * 任务指标信息
         */
        private TaskMetrics taskMetrics;
    }
    /**
     * 表示任务的统计信息
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总任务数
         */
        private Integer total;

        /**
         * 成功任务数
         */
        private Integer succeeded;

        /**
         * 失败任务数
         */
        private Integer failed;
    }

    /**
     * 任务结果列表，包括图像URL、prompt、部分任务执行失败报错信息等
     */
    @Data
    public static class Results{
        /**
         * 原始提示词
         */
        @Alias("orig_prompt")
        private String origPrompt;
        /**
         * 反向提示词
         */
        @Alias("actual_prompt")
        private String actualPrompt;
        /**
         * 图片地址
         */
        private String url;
        /**
         * 响应码
         */
        private String code;
        /**
         * message
         */
        private String message;
    }
}
