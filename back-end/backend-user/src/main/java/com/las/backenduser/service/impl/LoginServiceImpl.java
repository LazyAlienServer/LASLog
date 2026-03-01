package com.las.backenduser.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import com.las.backenduser.service.LoginService;
import com.las.backenduser.utils.jwt.JwtUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import com.las.backenduser.utils.salt.Salt;
import io.jsonwebtoken.Claims;
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
    public final UserMapper userMapper;
    public final JwtUtils jwtUtils;

    public LoginServiceImpl(StringRedisTemplate stringRedisTemplate, UserMapper userMapper, JwtUtils jwtUtils) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Result<String> login(String userName, String passwd , String clientId) {

        User getUserByName = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userName));
        if (getUserByName != null && Objects.equals(getUserByName.getPassword(), Salt.salt(passwd, getUserByName.getSalt()))) {

            String userUUID = getUserByName.getUuid();

            // 通过输入账密拿到新的refreshToken之前，如果有旧的就删除旧的
            String oldRtUserKey = REDIS_RT_USER_PREFIX + userUUID + ":" + clientId;
            String oldRt = stringRedisTemplate.opsForValue().get(oldRtUserKey);
            if (oldRt != null) {
                stringRedisTemplate.delete(REDIS_RT_TOKEN_PREFIX + oldRt + ":" + clientId);
                stringRedisTemplate.delete(oldRtUserKey);
            }

            String rT = jwtUtils.createRefreshToken();
            String aT = jwtUtils.createAccessToken(userUUID, userName);

            // 写入新的映射记录 (统一使用 stringRedisTemplate)
            stringRedisTemplate.opsForValue().set(REDIS_RT_USER_PREFIX + userUUID + ":" + clientId, rT, 14, TimeUnit.DAYS);
            stringRedisTemplate.opsForValue().set(REDIS_RT_TOKEN_PREFIX + rT + ":" + clientId, userUUID, 14, TimeUnit.DAYS);

            // 封装AT RT
            JSONObject result = new JSONObject();
            result.put("AT", aT);
            result.put("RT", rT);

            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), result.toJSONString(), "登陆成功");
        }

        return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "", "用户名或密码错误");
    }

    /**
     * 根据 UUID 踢出用户
     */
    public Result<Serializable> kickOutByUuid(String uuid) {
        // 1. 先校验数据库里到底有没有这个用户（防止乱传 UUID）
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUuid, uuid));
        if (count == null || count == 0) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "未找到用户");
        }

        // 2. 无论当前是否在线，都打上踢出标记（废掉他手里可能还没过期的 15 分钟 AccessToken）
        stringRedisTemplate.opsForValue().set(
                REDIS_KICKOUT_PREFIX + uuid,
                String.valueOf(System.currentTimeMillis()),
                16L, TimeUnit.MINUTES
        );

        // 3. 查找该用户在所有设备上的长效登录凭证 (RefreshToken)
        Set<String> userRtKeys = stringRedisTemplate.keys(REDIS_RT_USER_PREFIX + uuid + ":*");

        // 4. 如果没有查到 RT 记录，说明本来就不在线
        if (userRtKeys != null && userRtKeys.isEmpty()) {
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "未找到用户的登录状态或已经踢出");
        }

        // 5. 遍历并执行双向删除
        for (String userRtKey : userRtKeys) {
            String oldRt = stringRedisTemplate.opsForValue().get(userRtKey);
            if (oldRt != null) {
                String clientId = userRtKey.substring(userRtKey.lastIndexOf(":") + 1);
                stringRedisTemplate.delete(REDIS_RT_TOKEN_PREFIX + oldRt + ":" + clientId);
            }
            stringRedisTemplate.delete(userRtKey);
        }

        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "踢出成功");
    }

    /**
     * 根据 Username 踢出用户
     */
    public Result<Serializable> kickOutByUsername(String username) {
        // 先通过用户名查出对应的 UUID
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "未找到用户");
        }

        // 复用 UUID 踢出逻辑
        return kickOutByUuid(user.getUuid());
    }

    /**
     * 登出账户
     */
    public Result<Serializable> logout(String uuid, String clientId) {
        String userUuidKey = REDIS_RT_USER_PREFIX + uuid + ":" + clientId;
        String oldRt = stringRedisTemplate.opsForValue().get(userUuidKey);
        if (oldRt != null) {
            stringRedisTemplate.delete(REDIS_RT_TOKEN_PREFIX + oldRt + ":" + clientId);
            stringRedisTemplate.delete(userUuidKey);
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "登出成功");
        } else {
            return ResultUtil.result(ResultEnum.FORBIDDEN.getCode(), "未找到登录状态或已下线");
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
        try {
            Claims claims = jwtUtils.parseToken(accessToken);
            String uuid = claims.getSubject();

            if (isKickedOut(uuid, claims.getIssuedAt())) {
                return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "凭证已失效，请重新登录");
            }

            String userUuidKey = REDIS_RT_USER_PREFIX + uuid + ":" + clientId;

            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(userUuidKey))) {
                return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "当前设备已登出或会话已结束，请重新登录");
            }

            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "登录有效");

        } catch (Exception e) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "AccessToken无效或已过期");
        }
    }

    public Result<Serializable> refreshToken(String refreshToken, String clientId) {
        String redisKey = REDIS_RT_TOKEN_PREFIX + refreshToken + ":" + clientId;
        String userUuid = stringRedisTemplate.opsForValue().get(redisKey);

        if (userUuid != null) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUuid, userUuid));
            String accessToken = jwtUtils.createAccessToken(userUuid, user.getUsername());
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), accessToken, null);
        }
        return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "refreshToken错误或过期");
    }
}