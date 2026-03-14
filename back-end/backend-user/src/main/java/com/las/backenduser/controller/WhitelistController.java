package com.las.backenduser.controller;

import com.las.backenduser.model.dto.whitelist.WhitelistActionDTO;
import com.las.backenduser.model.dto.whitelist.WhitelistApplicationListVO;
import com.las.backenduser.model.dto.whitelist.WhitelistApplyDTO;
import com.las.backenduser.model.dto.whitelist.WhitelistStatusVO;
import com.las.backenduser.service.WhitelistService;
import com.las.backenduser.utils.cookie.CookieUtils;
import com.las.backenduser.utils.jwt.JwtUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

@RestController
@RequestMapping("/whitelist")
@RequiredArgsConstructor
@Slf4j
public class WhitelistController {

    private final WhitelistService whitelistService;
    private final JwtUtils jwtUtils;

    /**
     * 添加白名单
     * POST /whitelist/add
     * Body: { minecraftUuid, server }
     */
    @PostMapping("/add")
    public Result<Serializable> add(@RequestBody WhitelistActionDTO dto) {
        return whitelistService.addWhitelist(dto.getMinecraftUuid(), dto.getServer());
    }

    /**
     * 移除白名单
     * POST /whitelist/remove
     * Body: { minecraftUuid, server }
     */
    @PostMapping("/remove")
    public Result<Serializable> remove(@RequestBody WhitelistActionDTO dto) {
        return whitelistService.removeWhitelist(dto.getMinecraftUuid(), dto.getServer());
    }

    /**
     * 封禁（移除白名单并标记封禁）
     * POST /whitelist/ban
     * Body: { minecraftUuid, server, banDays }
     */
    @PostMapping("/ban")
    public Result<Serializable> ban(@RequestBody WhitelistActionDTO dto) {
        int days = dto.getBanDays() != null ? dto.getBanDays() : 0;
        return whitelistService.banWhitelist(dto.getMinecraftUuid(), dto.getServer(), days);
    }

    /**
     * 解封
     * POST /whitelist/unban
     * Body: { minecraftUuid, server }
     */
    @PostMapping("/unban")
    public Result<Serializable> unban(@RequestBody WhitelistActionDTO dto) {
        return whitelistService.unbanWhitelist(dto.getMinecraftUuid(), dto.getServer());
    }

    /**
     * 查询白名单状态
     * GET /whitelist/status?minecraftUuid=xxx&server=xxx
     * 返回: status: 1=有白名单, 0=无白名单, -1=封禁；封禁时附带 banExpireAt（毫秒，-1=永久）
     */
    @GetMapping("/status")
    public Result<WhitelistStatusVO> status(
            @RequestParam String minecraftUuid,
            @RequestParam String server) {
        return whitelistService.queryStatus(minecraftUuid, server);
    }

    /**
     * 玩家申请白名单（通过 HttpOnly Cookie 中的 Access Token 识别身份）
     * POST /whitelist/apply
     * Body: { server }
     */
    @PostMapping("/apply")
    public Result<Serializable> apply(
            HttpServletRequest request,
            @RequestBody WhitelistApplyDTO dto) {
        String token = CookieUtils.getCookieValue(request, CookieUtils.AT_COOKIE);
        if (token == null || token.trim().isEmpty()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "请先登录");
        }
        try {
            String userUuid = jwtUtils.getUserUUIDFromToken(token);
            return whitelistService.applyWhitelist(userUuid, dto.getServer());
        }
        catch (Exception e) {
            log.warn("白名单申请 Token 解析失败: {}", e.getMessage());
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "Token 无效或已过期");
        }
    }

    /**
     * 获取待处理的白名单申请列表（管理员用）
     * GET /whitelist/applications/pending
     */
    @GetMapping("/applications/pending")
    public Result<WhitelistApplicationListVO> pendingApplications() {
        return whitelistService.getPendingApplications();
    }

    /**
     * 审批白名单申请（管理员用）
     * POST /whitelist/applications/{id}/review?approve=true/false
     */
    @PostMapping("/applications/{id}/review")
    public Result<Serializable> reviewApplication(
            @PathVariable String id,
            @RequestParam boolean approve) {
        return whitelistService.reviewApplication(id, approve);
    }
}




