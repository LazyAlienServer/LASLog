package com.las.backenduser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class LogTest {

    @Test
    void logTest(){
        log.info("-----TEST------INFO LOG");
        log.error("-----TEST------ERROR LOG");
        log.debug("-----TEST------DEBUG LOG");
    }

}
