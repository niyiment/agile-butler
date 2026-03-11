package com.niyiment.agilebutler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableAsync
@EnableCaching
@SpringBootApplication
public class AgileButlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgileButlerApplication.class, args);
    }

}
