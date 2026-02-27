package com.las.backenduser.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import com.las.backenduser.service.LoginService;
import com.las.backenduser.service.db.redis.impl.RedisToolsImpl;
import com.las.backenduser.utils.jwt.JwtUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import com.las.backenduser.utils.salt.Salt;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    public final RedisToolsImpl redisTools;
    public final UserMapper userMapper;
    public final JwtUtils jwtUtils;

    public LoginServiceImpl(RedisToolsImpl redisTools, UserMapper userMapper, JwtUtils jwtUtils) {
        this.redisTools = redisTools;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }


    @Override
    public Result<String> login(String userName, String passwd , String clientId) {

        // 相当于 SELECT * FROM user WHERE user_name = '张三' LIMIT 1
        User getUserByName = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userName));

        //校验用户名
        if (getUserByName != null){
            //获取实际信息
            String userSalt = getUserByName.getSalt();
            String userCiphertext = getUserByName.getPassword();
            String userUUID = getUserByName.getUuid();
            //加盐
            String inputCiphertext = Salt.salt(passwd,userSalt);
            //校验密码
            if (Objects.equals(userCiphertext, inputCiphertext)){
                String rT = jwtUtils.createRefreshToken();
                String aT = jwtUtils.createAccessToken(userUUID,userName);

                //auth:rt:{UserUUID}:{clientId}   (Sections of code should not be commented out)
                redisTools.insert("auth:rt:" + userUUID + ":" + clientId,rT,14, TimeUnit.DAYS);

                //封装AT RT
                JSONObject result = new JSONObject();
                result.put("AT", aT);
                result.put("RT", rT);

                return ResultUtil.result(ResultEnum.SUCCESS.getCode(), result.toJSONString(),"登陆成功");

            }else{
                return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(),"" ,"用户名或密码错误");
            }
        }else {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(),"", "用户名或密码错误");
        }
    }
}
