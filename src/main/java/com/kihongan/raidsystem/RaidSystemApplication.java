package com.kihongan.raidsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class RaidSystemApplication {
    
    @PostConstruct
    public void init() {
        // 設定應用程式時區為台北時間
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
    }
    
    public static void main(String[] args) {
        SpringApplication.run(RaidSystemApplication.class, args);
    }
}
