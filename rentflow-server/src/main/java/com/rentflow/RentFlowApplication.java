package com.rentflow;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.rentflow", annotationClass = Mapper.class)
@SpringBootApplication
public class RentFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentFlowApplication.class, args);
    }
}
