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
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    // --- 提取重复使用的魔法值常量，解决 java:S1192 警告 ---
    private static final String REDIS_RT_USER_PREFIX = "auth:rt:user:";
    private static final String REDIS_RT_TOKEN_PREFIX = "auth:rt:token:";
    private static final String REDIS_KICKOUT_PREFIX = "login:kickout:";

    public final StringRedisTemplate stringRedisTemplate;
    public final RedisToolsImpl redisTools;
    public final UserMapper userMapper;
    public final JwtUtils jwtUtils;

    public LoginServiceImpl(StringRedisTemplate stringRedisTemplate, RedisToolsImpl redisTools, UserMapper userMapper, JwtUtils jwtUtils) {
        this.stringRedisTemplate = stringRedisTemplate;
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

                // 通过输入账密拿到新的refreshToken之前，如果有旧的就删除旧的
                String oldRtUserKey = REDIS_RT_USER_PREFIX + userUUID + ":" + clientId;
                String oldRt = stringRedisTemplate.opsForValue().get(oldRtUserKey);
                if (oldRt != null) {
                    oldRt = oldRt.replace("\"", "");
                    stringRedisTemplate.delete(REDIS_RT_TOKEN_PREFIX + oldRt + ":" + clientId);
                    stringRedisTemplate.delete(oldRtUserKey);
                }

                String rT = jwtUtils.createRefreshToken();
                String aT = jwtUtils.createAccessToken(userUUID,userName);

                // 写入新的映射记录 (已移除引起 java:S125 警告的多余注释)
                redisTools.insert(REDIS_RT_USER_PREFIX + userUUID + ":" + clientId, rT, 14, TimeUnit.DAYS);
                redisTools.insert(REDIS_RT_TOKEN_PREFIX + rT + ":" + clientId, userUUID, 14, TimeUnit.DAYS);

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

    public void kickOut(String uuid){
        // 1. 设置踢出黑名单标记
        stringRedisTemplate.opsForValue().set(
                REDIS_KICKOUT_PREFIX + uuid,
                String.valueOf(System.currentTimeMillis()),
                14,TimeUnit.DAYS
        );

        // 2. 踢出之后删除对应的refreshToken
        Set<String> userRtKeys = stringRedisTemplate.keys(REDIS_RT_USER_PREFIX + uuid + ":*");
        if (userRtKeys != null && !userRtKeys.isEmpty()) {
            for (String userRtKey : userRtKeys) {
                String oldRt = stringRedisTemplate.opsForValue().get(userRtKey);
                if (oldRt != null) {
                    oldRt = oldRt.replace("\"", "");
                    String clientId = userRtKey.substring(userRtKey.lastIndexOf(":") + 1);
                    stringRedisTemplate.delete(REDIS_RT_TOKEN_PREFIX + oldRt + ":" + clientId);
                }
                stringRedisTemplate.delete(userRtKey);
            }
        }
    }

    public boolean isKickedOut(String uuid, Date date){
        String kickTimeStr = stringRedisTemplate.opsForValue().get(REDIS_KICKOUT_PREFIX + uuid);
        if (kickTimeStr != null){
            long kickTime = Long.parseLong(kickTimeStr);
            return kickTime >= date.getTime();
        }
        return false;
    }

    @Override
    public Result<Serializable> loginByToken(String accessToken, String clientId) {

        try{
            Claims claims = jwtUtils.parseToken(accessToken);
            String userUuid = jwtUtils.getUserUUIDFromToken(accessToken);
            User getUserByUuid = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUuid, userUuid));

            if (isKickedOut(getUserByUuid.getUuid(), claims.getIssuedAt())){
                return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "KICKED");
            }
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "登录成功");

        }catch (Exception e){
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), e.getMessage(), null);
        }
    }

    public Result<Serializable> refreshToken(String refreshToken, String clientId) {
        refreshToken = redisTools.getByKey(REDIS_RT_TOKEN_PREFIX + refreshToken + ":" + clientId) != null ? refreshToken : null;
        if (refreshToken != null){
            @NonNull
            String userUuid = Objects.requireNonNull(stringRedisTemplate.opsForValue().get(REDIS_RT_TOKEN_PREFIX + refreshToken + ":" + clientId));
            userUuid = userUuid.replace("\"", "");
            String username = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUuid, userUuid)).getUsername();
            String accessToken = jwtUtils.createAccessToken(userUuid,username);
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), accessToken, null);
        }else{
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "refreshToken错误或过期");
        }
    }
}