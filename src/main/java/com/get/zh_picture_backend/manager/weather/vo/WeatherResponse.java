package com.get.zh_picture_backend.manager.weather.vo;

import lombok.Data;

@Data
public class WeatherResponse {
    private String status;
    private String count;
    private String info;
    private String infocode;

    private Lives[] lives;

    @Data
    public  class Lives{
        private String province;
        private String city;
        private String adcode;
        private String weather;
        private String temperature;
    }

}
