package com.yan.freshfood.framework.config;

import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SaTokenConfig {

    /**
     * 单 Sa-Token 体系：只用默认 StpLogic（type="login"），所有账号共用同一份 token 空间；
     * 商家端接口靠 @SaCheckRole("MERCHANT") 注解拦截。
     */
    @Bean(name = "stpLogic")
    @Primary
    public StpLogic stpLogic() {
        return new StpLogic("login");
    }
}