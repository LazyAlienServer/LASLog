package com.las.backenduser.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@MappedJdbcTypes(JdbcType.ARRAY)
@MappedTypes(List.class)
public class ListArrayTypeHandler extends BaseTypeHandler<List<?>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<?> parameter, JdbcType jdbcType) throws SQLException {
        // 将 List 转为 postgresql 识别的数组对象
        Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<?> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extractList(rs.getArray(columnName));
    }

    @Override
    public List<?> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extractList(rs.getArray(columnIndex));
    }

    @Override
    public List<?> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extractList(cs.getArray(columnIndex));
    }

    private List<?> extractList(Array array) throws SQLException {
        if (array == null) {
            return Collections.emptyList();
        }
        Object[] javaArray = (Object[]) array.getArray();
        if (javaArray == null || javaArray.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(javaArray);
    }
}