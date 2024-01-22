package com.atguigu.gmall.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients("com.atguigu.gmall")
@ComponentScan("com.atguigu.gmall")
public class ActivityApp {

    public static void main(String[] args) {
        SpringApplication.run(ActivityApp.class, args);
    }

}
