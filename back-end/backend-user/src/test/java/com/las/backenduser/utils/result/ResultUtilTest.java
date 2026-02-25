package com.las.backenduser.utils.result;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class ResultUtilTest {

    @Test
    void resultWithData(){
        Result<String> testResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "TEST RESULT","200 success" );
        assertThat(testResult.getData()).isEqualTo("TEST RESULT");
    }

    @Test
    void resultWithOutData(){
        Result testResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "200 success" );
        assertThat(testResult.getData()).isNull();
    }

}