package com.las.backenduser.model;

import com.baomidou.mybatisplus.annotation.*;
import com.las.backenduser.handler.ListArrayTypeHandler;
import jakarta.persistence.Column;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "\"user\"", autoResultMap = true)
public class User {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_BANNED = 2;

    /**
     * id自增
     */
    @TableField(value = "id", insertStrategy = FieldStrategy.NEVER)
    private Long id;

    /**
     * 主键 UUID
     */
    @TableId(value = "uuid", type = IdType.INPUT)
    private String uuid;

    /**
     * minecraft uuid 列表
     */
    @TableField(value = "uuid_minecraft", typeHandler = ListArrayTypeHandler.class)
    private List<String> minecraftUuids;

    /**
     * minecraft id 列表
     */
    @TableField(value = "id_minecraft", typeHandler = ListArrayTypeHandler.class)
    private List<String> minecraftIds;

    /**用户名**/
    private String username;

    /**加盐密码**/
    private String password;

    /**
     * 注册日期 UTC+8 Shanghai
     */
    @TableField("registerdate")
    private Long registerDate;

    /**qq号**/
    private Long qq;

    /**细分权限列表**/
    @TableField(value = "permission", typeHandler = ListArrayTypeHandler.class)
    private List<String> permission;

    /**白名单**/
    @TableField(value = "whitelist", typeHandler = ListArrayTypeHandler.class)
    private List<String> whitelist;

    /**
     * 激活状态：0-待激活,1-已激活,2-封禁/禁用
     */
    private Integer status;

    /**主MC账号**/
    @Column(name = "main_minecraft_uuid")
    private String mainMinecraftUuid;

    /**salt**/
    private String salt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMinecraftUuids() {
        return minecraftUuids;
    }

    public void setMinecraftUuids(List<String> minecraftUuids) {
        this.minecraftUuids = minecraftUuids;
    }

    public List<String> getMinecraftIds() {
        return minecraftIds;
    }

    public void setMinecraftIds(List<String> minecraftIds) {
        this.minecraftIds = minecraftIds;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Long registerDate) {
        this.registerDate = registerDate;
    }

    public Long getQq() {
        return qq;
    }

    public void setQq(Long qq) {
        this.qq = qq;
    }

    public List<String> getPermission() {
        return permission;
    }

    public void setPermission(List<String> permission) {
        this.permission = permission;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMainMinecraftUuid() {
        return mainMinecraftUuid;
    }

    public void setMainMinecraftUuid(String mainMinecraftUuid) {
        this.mainMinecraftUuid = mainMinecraftUuid;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
