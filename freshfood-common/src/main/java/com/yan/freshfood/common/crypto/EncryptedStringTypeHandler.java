package com.yan.freshfood.common.crypto;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库字符串字段加密 TypeHandler
 * <p>
 * 写库前自动 encrypt，读库后自动 decrypt。
 * 用法：在 DO 字段上加 @TableField(typeHandler = EncryptedStringTypeHandler.class)
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, FieldCryptoHolder.get().encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String raw = rs.getString(columnName);
        return raw == null ? null : FieldCryptoHolder.get().decrypt(raw);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String raw = rs.getString(columnIndex);
        return raw == null ? null : FieldCryptoHolder.get().decrypt(raw);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String raw = cs.getString(columnIndex);
        return raw == null ? null : FieldCryptoHolder.get().decrypt(raw);
    }
}