package com.yan.freshfood;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.yan.freshfood",
        "com.yan.freshfood.user",
        "com.yan.freshfood.merchant",
        "com.yan.freshfood.admin",
        "com.yan.freshfood.framework",
        "com.yan.freshfood.common"
})
@MapperScan(basePackages = {
        "com.yan.freshfood.user.mapper",
        "com.yan.freshfood.merchant.mapper",
        "com.yan.freshfood.admin.mapper"
})
public class FreshfoodShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshfoodShopApplication.class, args);
    }
}