package com.las.backenduser.handler;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListArrayTypeHandlerTest {

    private ListArrayTypeHandler typeHandler;

    @Mock
    private PreparedStatement ps;
    @Mock
    private ResultSet rs;
    @Mock
    private Connection connection;
    @Mock
    private Array mockSqlArray;

    @BeforeEach
    void setUp() {
        typeHandler = new ListArrayTypeHandler();
    }

    @Test
    void should_set_non_null_parameter() throws SQLException {
        // 准备数据
        List<String> list = Arrays.asList("apple", "banana");

        // 简化：如果参数很明确，直接使用变量名或常量
        when(ps.getConnection()).thenReturn(connection);

        // 注意：这里因为用了 any()，所以 "text" 必须保留 eq()，否则会报错
        when(connection.createArrayOf(eq("text"), any(Object[].class))).thenReturn(mockSqlArray);

        // 执行测试
        typeHandler.setNonNullParameter(ps, 1, list, JdbcType.ARRAY);

        // 简化后的 verify：去掉 eq()
        verify(ps).setArray(1, mockSqlArray);
    }

    @Test
    void should_get_nullable_result_by_column_name() throws SQLException {
        // 准备 Mock 返回的数组
        String[] data = {"item1", "item2"};
        when(rs.getArray("my_column")).thenReturn(mockSqlArray);
        when(mockSqlArray.getArray()).thenReturn(data);

        // 执行
        List<?> result = typeHandler.getNullableResult(rs, "my_column");

        // 断言
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("item1", result.get(0));
    }

    @Test
    void should_return_empty_list_when_array_is_null() throws SQLException {
        // 模拟数据库返回 null
        when(rs.getArray(anyString())).thenReturn(null);

        // 执行
        List<?> result = typeHandler.getNullableResult(rs, "any_column");

        // 断言：应当返回空列表而非 null
        assertTrue(result.isEmpty());
    }

    @Test
    void should_get_nullable_result_by_column_index() throws SQLException {
        // 1. 准备数据
        String[] data = {"item1", "item2"};
        // 2. 模拟 rs.getArray(int) 的行为
        when(rs.getArray(1)).thenReturn(mockSqlArray);
        when(mockSqlArray.getArray()).thenReturn(data);

        // 3. 执行测试（调用 index 为 1 的方法）
        List<?> result = typeHandler.getNullableResult(rs, 1);

        // 4. 断言
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(rs).getArray(1); // 确保确实按索引调用了
    }

    @Test
    void should_get_nullable_result_from_callable_statement() throws SQLException {
        // 1. 准备 Mock 对象
        CallableStatement cs = mock(CallableStatement.class);
        String[] data = {"proc1", "proc2"};
        when(cs.getArray(1)).thenReturn(mockSqlArray);
        when(mockSqlArray.getArray()).thenReturn(data);

        // 2. 执行测试
        List<?> result = typeHandler.getNullableResult(cs, 1);

        // 3. 断言
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("proc1", result.get(0));
        verify(cs).getArray(1);
    }

    @Test
    void should_return_empty_list_when_inner_java_array_is_null() throws SQLException {
        // 模拟 rs.getArray() 虽返回了对象，但其内部的 getArray() 返回 null
        when(rs.getArray(anyString())).thenReturn(mockSqlArray);
        when(mockSqlArray.getArray()).thenReturn(null);

        List<?> result = typeHandler.getNullableResult(rs, "any_column");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        // 验证逻辑命中：javaArray == null
    }

    @Test
    void should_return_empty_list_when_inner_java_array_is_empty() throws SQLException {
        // 模拟内部数组长度为 0
        when(rs.getArray(anyInt())).thenReturn(mockSqlArray);
        when(mockSqlArray.getArray()).thenReturn(new Object[0]);

        List<?> result = typeHandler.getNullableResult(rs, 1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        // 验证逻辑命中：javaArray.length == 0
    }
}