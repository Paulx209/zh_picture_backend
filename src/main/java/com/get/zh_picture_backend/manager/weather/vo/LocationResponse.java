package com.get.zh_picture_backend.manager.weather.vo;

import lombok.Data;

@Data
public class LocationResponse {
    /**
     * 返回结果状态值 0:失败 1:成功
     */
    private String status;

    /**
     * 返回状态说明 成功:OK 失败:错误原因
     */
    private String info;

    /**
     * 状态码 100000正确
     */
    private String infocode;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 城市名称
     */
    private String adcode;

    /**
     * 城市矩形区域范围
     */
    private String rectangle;


}
