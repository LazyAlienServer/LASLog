package com.las.backenduser.utils.result;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;


class ResultUtilTest {

    @Test
    void resultWithData(){
        Result<String> testResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "TEST RESULT","200 success" );
        assertThat(testResult.getData()).isEqualTo("TEST RESULT");
    }

    @Test
    void resultWithOutData(){
        Result<Serializable> testResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "200 success" );
        assertThat(testResult.getData()).isNull();
    }

}