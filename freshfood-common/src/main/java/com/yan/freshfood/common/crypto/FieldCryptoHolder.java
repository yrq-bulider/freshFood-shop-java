package com.yan.freshfood.common.crypto;

/**
 * 静态持有 FieldCrypto 实例，供 MyBatis TypeHandler 使用
 * <p>
 * 由于 MyBatis TypeHandler 由框架反射实例化，无法直接注入 Spring Bean，
 * 通过 CryptoConfig 在 Spring 启动时把 FieldCrypto 注入到这里。
 */
public class FieldCryptoHolder {

    private static volatile FieldCrypto INSTANCE;

    public static void set(FieldCrypto crypto) {
        INSTANCE = crypto;
    }

    public static FieldCrypto get() {
        FieldCrypto c = INSTANCE;
        if (c == null) {
            throw new IllegalStateException(
                    "FieldCrypto 未初始化，请确认 CryptoConfig 已加载且 freshfood.crypto.aes-key 已配置");
        }
        return c;
    }
}