package com.las.backenduser.service;

import com.las.backenduser.model.dto.whitelist.WhitelistApplicationListVO;
import com.las.backenduser.model.dto.whitelist.WhitelistStatusVO;
import com.las.backenduser.utils.result.Result;

import java.io.Serializable;

/**
 * 白名单管理业务接口
 */
public interface WhitelistService {

    /**
     * 给某玩家添加某服务器白名单
     *
     * @param minecraftUuid 玩家 Minecraft UUID
     * @param server        服务器名称
     * @return 操作结果
     */
    Result<Serializable> addWhitelist(String minecraftUuid, String server);

    /**
     * 移除某玩家的某服务器白名单
     *
     * @param minecraftUuid 玩家 Minecraft UUID
     * @param server        服务器名称
     * @return 操作结果
     */
    Result<Serializable> removeWhitelist(String minecraftUuid, String server);

    /**
     * 封禁某玩家在某服务器的白名单（标记为封禁状态）
     *
     * @param minecraftUuid 玩家 Minecraft UUID
     * @param server        服务器名称
     * @param banDays       封禁天数（0 = 永久）
     * @return 操作结果
     */
    Result<Serializable> banWhitelist(String minecraftUuid, String server, int banDays);

    /**
     * 解封某玩家在某服务器的封禁
     *
     * @param minecraftUuid 玩家 Minecraft UUID
     * @param server        服务器名称
     * @return 操作结果
     */
    Result<Serializable> unbanWhitelist(String minecraftUuid, String server);

    /**
     * 查询某玩家在某服务器的白名单状态
     *
     * @param minecraftUuid 玩家 Minecraft UUID
     * @param server        服务器名称
     * @return status: 1=有白名单, 0=无白名单, -1=封禁；封禁时附带 banExpireAt（毫秒时间戳，-1=永久）
     */
    Result<WhitelistStatusVO> queryStatus(String minecraftUuid, String server);

    /**
     * 玩家申请白名单（通过 Access Token 识别身份）
     *
     * @param userUuid 用户系统 UUID（从 AT 解析）
     * @param server   申请的服务器
     * @return 操作结果
     */
    Result<Serializable> applyWhitelist(String userUuid, String server);

    /**
     * 获取待处理的白名单申请列表
     *
     * @return 申请列表
     */
    Result<WhitelistApplicationListVO> getPendingApplications();

    /**
     * 审批白名单申请（同意或拒绝）
     *
     * @param applicationId 申请记录 ID
     * @param approve       true=同意, false=拒绝
     * @return 操作结果
     */
    Result<Serializable> reviewApplication(String applicationId, boolean approve);
}



