package com.las.backenduser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.dto.whitelist.WhitelistStatusVO;
import com.las.backenduser.repository.WhitelistApplicationRepository;
import com.las.backenduser.model.User;
import com.las.backenduser.model.WhitelistApplication;
import com.las.backenduser.model.dto.whitelist.WhitelistApplicationListVO;
import com.las.backenduser.model.dto.whitelist.WhitelistApplicationVO;
import com.las.backenduser.service.WhitelistService;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhitelistServiceImpl implements WhitelistService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final WhitelistApplicationRepository whitelistApplicationRepository;

    /** Redis key 前缀: ban:{minecraftUuid}:{server} */
    private static final String BAN_PREFIX = "whitelist:ban:";

    /** 申请状态常量 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    /** 错误消息常量 */
    private static final String MSG_USER_NOT_FOUND = "未找到该 Minecraft UUID 对应的用户";

    // ==================== 根据 Minecraft UUID 查找用户 ====================

    private User findUserByMinecraftUuid(String minecraftUuid) {
        // minecraftUuids 是 PostgreSQL 数组，使用 apply 查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("uuid_minecraft @> ARRAY[{0}]::text[]", minecraftUuid);
        return userMapper.selectOne(wrapper);
    }

    // ==================== 核心白名单操作 ====================

    @Override
    public Result<Serializable> addWhitelist(String minecraftUuid, String server) {
        User user = findUserByMinecraftUuid(minecraftUuid);
        if (user == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), MSG_USER_NOT_FOUND);
        }

        // 检查是否已被封禁
        String banKey = BAN_PREFIX + minecraftUuid + ":" + server;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(banKey))) {
            return ResultUtil.result(ResultEnum.FORBIDDEN.getCode(), "该玩家在此服务器已被封禁，无法添加白名单");
        }

        List<String> whitelist = new ArrayList<>(user.getWhitelist() == null ? List.of() : user.getWhitelist());
        if (whitelist.contains(server)) {
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "该玩家已在此服务器白名单中");
        }

        whitelist.add(server);
        user.setWhitelist(whitelist);
        userMapper.updateById(user);

        log.info("添加白名单成功: minecraftUuid={}, server={}", minecraftUuid, server);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "添加白名单成功");
    }

    @Override
    public Result<Serializable> removeWhitelist(String minecraftUuid, String server) {
        User user = findUserByMinecraftUuid(minecraftUuid);
        if (user == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), MSG_USER_NOT_FOUND);
        }

        List<String> whitelist = new ArrayList<>(user.getWhitelist() == null ? List.of() : user.getWhitelist());
        if (!whitelist.contains(server)) {
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "该玩家不在此服务器白名单中");
        }

        whitelist.remove(server);
        user.setWhitelist(whitelist);
        userMapper.updateById(user);

        log.info("移除白名单成功: minecraftUuid={}, server={}", minecraftUuid, server);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "移除白名单成功");
    }

    @Override
    public Result<Serializable> banWhitelist(String minecraftUuid, String server, int banDays) {
        User user = findUserByMinecraftUuid(minecraftUuid);
        if (user == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), MSG_USER_NOT_FOUND);
        }

        // 如果有白名单，先移除
        List<String> whitelist = new ArrayList<>(user.getWhitelist() == null ? List.of() : user.getWhitelist());
        if (whitelist.remove(server)) {
            user.setWhitelist(whitelist);
            userMapper.updateById(user);
        }

        // 写入 Redis 封禁记录
        String banKey = BAN_PREFIX + minecraftUuid + ":" + server;
        if (banDays > 0) {
            stringRedisTemplate.opsForValue().set(banKey, "1", banDays, TimeUnit.DAYS);
        } else {
            // 永久封禁：设置一个极大的过期时间（100年）
            stringRedisTemplate.opsForValue().set(banKey, "1", 36500, TimeUnit.DAYS);
        }

        log.info("封禁成功: minecraftUuid={}, server={}, days={}", minecraftUuid, server, banDays);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), banDays > 0
                ? "封禁成功，时长 " + banDays + " 天"
                : "永久封禁成功");
    }

    @Override
    public Result<Serializable> unbanWhitelist(String minecraftUuid, String server) {
        String banKey = BAN_PREFIX + minecraftUuid + ":" + server;
        Boolean deleted = stringRedisTemplate.delete(banKey);
        if (!Boolean.TRUE.equals(deleted)) {
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "该玩家在此服务器未被封禁");
        }

        log.info("解封成功: minecraftUuid={}, server={}", minecraftUuid, server);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "解封成功");
    }

    @Override
    public Result<WhitelistStatusVO> queryStatus(String minecraftUuid, String server) {
        String banKey = BAN_PREFIX + minecraftUuid + ":" + server;
        Long ttlSeconds = stringRedisTemplate.getExpire(banKey, TimeUnit.SECONDS);
        if (ttlSeconds == null) {//sonar必须要加这句
            log.error("查询封禁状态失败: minecraftUuid={}, server={}", minecraftUuid, server);
            return ResultUtil.result(ResultEnum.FAIL.getCode(), null, "查询封禁状态失败");
        }
        // ttlSeconds > 0 → 有限期封禁；-1 → 永久（无过期）；-2 → key 不存在（未封禁）
        if (ttlSeconds >= -1) {
            WhitelistStatusVO vo = new WhitelistStatusVO();
            vo.setStatus(-1);
            if (ttlSeconds == -1L) {
                // 永久封禁，用 -1 标识
                vo.setBanExpireAt(-1L);
            }
            else {
                // 计算到期毫秒时间戳
                vo.setBanExpireAt(System.currentTimeMillis() + ttlSeconds * 1000L);
            }
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), vo, "封禁");
        }

        User user = findUserByMinecraftUuid(minecraftUuid);
        if (user == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), null, "未找到该玩家");
        }

        List<String> whitelist = user.getWhitelist();
        boolean hasWhitelist = whitelist != null && whitelist.contains(server);
        WhitelistStatusVO vo = new WhitelistStatusVO();
        vo.setStatus(hasWhitelist ? 1 : 0);
        vo.setBanExpireAt(null);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), vo, hasWhitelist ? "有白名单" : "无白名单");
    }

    // ==================== 白名单申请 ====================

    @Override
    public Result<Serializable> applyWhitelist(String userUuid, String server) {
        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUuid, userUuid);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "用户不存在");
        }

        // 判断是否已有白名单
        List<String> whitelist = user.getWhitelist();
        if (whitelist != null && whitelist.contains(server)) {
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "您已在此服务器白名单中");
        }

        // 判断是否已有待处理申请
        if (whitelistApplicationRepository.existsByUserUuidAndServerAndStatus(userUuid, server, STATUS_PENDING)) {
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "您已提交过此服务器的白名单申请，请等待审核");
        }

        WhitelistApplication application = new WhitelistApplication();
        application.setUserUuid(userUuid);
        application.setUsername(user.getUsername());
        application.setServer(server);
        application.setStatus(STATUS_PENDING);
        application.setCreateTime(System.currentTimeMillis());
        whitelistApplicationRepository.save(application);

        log.info("白名单申请提交成功: userUuid={}, server={}", userUuid, server);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "申请已提交，等待管理员审核");
    }

    @Override
    public Result<WhitelistApplicationListVO> getPendingApplications() {
        List<WhitelistApplication> list = whitelistApplicationRepository
                .findByStatusOrderByCreateTimeDesc(STATUS_PENDING);

        List<WhitelistApplicationVO> voList = list.stream().map(app -> {
            WhitelistApplicationVO vo = new WhitelistApplicationVO();
            vo.setId(app.getId());
            vo.setUserUuid(app.getUserUuid());
            vo.setUsername(app.getUsername());
            vo.setServer(app.getServer());
            vo.setStatus(app.getStatus());
            vo.setCreateTime(app.getCreateTime());
            return vo;
        }).toList();

        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), WhitelistApplicationListVO.of(voList), "查询成功");
    }

    @Override
    public Result<Serializable> reviewApplication(String applicationId, boolean approve) {
        WhitelistApplication application = whitelistApplicationRepository.findById(applicationId)
                .orElse(null);
        if (application == null) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "申请记录不存在");
        }
        if (!STATUS_PENDING.equals(application.getStatus())) {
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "该申请已被处理");
        }

        if (approve) {
            // 同意：添加白名单
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUuid, application.getUserUuid());
            User user = userMapper.selectOne(wrapper);
            if (user != null) {
                List<String> whitelist = new ArrayList<>(
                        user.getWhitelist() == null ? List.of() : user.getWhitelist());
                if (!whitelist.contains(application.getServer())) {
                    whitelist.add(application.getServer());
                    user.setWhitelist(whitelist);
                    userMapper.updateById(user);
                }
            }
            application.setStatus(STATUS_APPROVED);
        } else {
            application.setStatus(STATUS_REJECTED);
        }

        whitelistApplicationRepository.save(application);

        log.info("审批白名单申请: id={}, approve={}", applicationId, approve);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), approve ? "已同意申请并添加白名单" : "已拒绝申请");
    }
}




