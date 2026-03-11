package com.las.backenduser.model.dto.whitelist;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 白名单申请列表包装 VO（用于 Result 泛型 Serializable 约束）
 */
@Data
public class WhitelistApplicationListVO implements Serializable {

    private List<WhitelistApplicationVO> items;

    public static WhitelistApplicationListVO of(List<WhitelistApplicationVO> items) {
        WhitelistApplicationListVO vo = new WhitelistApplicationListVO();
        vo.setItems(items);
        return vo;
    }
}

