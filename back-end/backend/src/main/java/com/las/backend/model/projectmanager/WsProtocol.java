package com.las.backend.model.projectmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsProtocol {
    private String id;      // 消息唯一ID (UUID)
    private String action;  // 操作类型，例如 "CALCULATE"
    private String data;    // 传递的数据内容
}
