package com.las.backenduser;

import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional //自动回滚
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void testInsertAndSelect() {
        // 1. 构造数据
        User user = new User();
        String randomUuid = UUID.randomUUID().toString();
        user.setUuid(randomUuid);
        user.setUsername("TestUser");
        user.setPassword("123456");
        user.setStatus(1);
        user.setRegisterdate(23434243L);
        user.setQq(2192519304L);


        // 测试 PostgreSQL 数组类型 (text[])
        List<String> permission = Arrays.asList("footjob", "hardjob");
        user.setPermission(permission);

        List<String> mcUuids = Arrays.asList("mc-uuid-1", "mc-uuid-2");
        user.setUuidMinecraft(mcUuids);

        List<String> whitelists = Arrays.asList("server-1", "server-2");
        user.setWhitelist(whitelists);

        // 2. 执行插入
        int rows = userMapper.insert(user);
        assertThat(rows).isEqualTo(1);

        // 3. 查询并验证
        User foundUser = userMapper.selectById(randomUuid);

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("TestUser");

        // 验证数组/JSON 处理器是否正常工作
        assertThat(foundUser.getUuidMinecraft()).containsExactly("mc-uuid-1", "mc-uuid-2");
        assertThat(foundUser.getWhitelist()).hasSize(2);
        assertThat(foundUser.getPermission()).hasSize(2);

        System.out.println("查询到的用户信息: " + foundUser);
    }
}