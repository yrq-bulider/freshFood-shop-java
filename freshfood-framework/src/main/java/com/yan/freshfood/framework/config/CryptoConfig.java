package com.yan.freshfood.framework.config;

import com.yan.freshfood.common.crypto.FieldCrypto;
import com.yan.freshfood.common.crypto.FieldCryptoHolder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 字段加解密配置
 * <p>
 * 读取 freshfood.crypto.aes-key，在 Spring 启动时构造 FieldCrypto
 * 并注入到 FieldCryptoHolder，供 MyBatis TypeHandler 使用。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "freshfood.crypto")
public class CryptoConfig {

    /** Base64 编码的 AES key（16/24/32 字节） */
    private String aesKey;

    /**
     * 把 FieldCrypto 暴露给 MyBatis TypeHandler（由框架反射实例化，无法注入 Spring Bean）。
     */
    @PostConstruct
    public void init() {
        FieldCryptoHolder.set(new FieldCrypto(aesKey));
    }
}
