package com.las.backenduser.utils;

import com.las.backenduser.model.Password;
import com.las.backenduser.utils.salt.Salt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaltTest {

    @Test
    void testSalt() {
        String rawPassword = "mySecretPassword123";
        Password password = Salt.salt(rawPassword);

        assertNotNull(password);
        assertNotNull(password.getSalt());
        assertNotNull(password.getCipherText());

        // 验证确实进行了加密 (不同于原密码)
        assertNotEquals(rawPassword, password.getCipherText());
        // UUID 盐的长度通常为 36
        assertEquals(36, password.getSalt().length());
    }
}