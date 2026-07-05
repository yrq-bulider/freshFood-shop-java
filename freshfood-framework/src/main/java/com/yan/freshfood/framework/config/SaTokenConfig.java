package com.yan.freshfood.framework.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.yan.freshfood.common.constant.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SaTokenConfig {

    @Bean(name = "stpUserLogic")
    @Primary
    public StpLogic stpUserLogic() {
        return new StpLogicJwtForSimple(CommonConstants.TYPE_USER);
    }

    @Bean(name = "stpMerchantLogic")
    public StpLogic stpMerchantLogic() {
        return new StpLogicJwtForSimple(CommonConstants.TYPE_MERCHANT);
    }

    @Bean(name = "stpAdminLogic")
    public StpLogic stpAdminLogic() {
        return new StpLogicJwtForSimple(CommonConstants.TYPE_ADMIN);
    }
}