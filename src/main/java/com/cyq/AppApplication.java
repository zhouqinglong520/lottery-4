package com.cyq;

import com.cyq.app.TIM.IMProperties;
import com.cyq.app.configuration.TTMProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@MapperScan({"com.cyq.*.mapper", "com.cyq.uid.worker.mapper"})
@EnableConfigurationProperties({IMProperties.class, TTMProperties.class})
@EnableTransactionManagement
@SpringBootApplication
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
        log.info("The application ★app★ has started successfully !");
    }

}

