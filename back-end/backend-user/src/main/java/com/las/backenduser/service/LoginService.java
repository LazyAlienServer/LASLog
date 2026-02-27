package com.las.backenduser.service;

import com.las.backenduser.utils.result.Result;

import java.io.Serializable;

public interface LoginService {

    /**
     * 用户登陆
     *
     * @param userName 用户名
     * @param passwd   密码
     * @param clientId 设备ID
     * @return AT+RT
     */
    Result<String> login(String userName, String passwd,String clientId);

    Result<Serializable> loginByToken(String token, String clientId);

}
