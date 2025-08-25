package com.get.zh_picture_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@MapperScan("com.get.zh_picture_backend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
//开启定时任务
@EnableScheduling
//开启异步线程? 默认线程池是?
@EnableAsync
public class ZhPictureBackendApplication {


    public static void main(String[] args) {
        List<Integer> list=new ArrayList<>();
        SpringApplication.run(ZhPictureBackendApplication.class, args);
    }

}
