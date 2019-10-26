package com.cumt.gmall.manage;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

//@EnableDubbo(scanBasePackages = "com.cumt.gmall")
@SpringBootApplication
@MapperScan(basePackages = "com.cumt.gmall.manage.mapper")
@EnableTransactionManagement
@ComponentScan(basePackages = "com.cumt.gmall")
public class GmallManageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallManageServiceApplication.class, args);
    }

}
