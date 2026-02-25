package com.las.backenduser.model;

import com.baomidou.mybatisplus.annotation.*;
import com.las.backenduser.handler.ListArrayTypeHandler;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "\"user\"", autoResultMap = true)
public class User {

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
    private List<String> uuidMinecraft;

    /**用户名**/
    private String username;

    /**加盐密码**/
    private String password;

    /**
     * 注册日期 UTC+8 Shanghai
     */
    private Long registerdate;

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
}
