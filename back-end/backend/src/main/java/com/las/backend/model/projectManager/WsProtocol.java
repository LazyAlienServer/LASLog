package com.las.backend.model.projectManager;

public class WsProtocol {
    public String id;      // 消息唯一ID (UUID)
    public String action;  // 操作类型，例如 "CALCULATE"
    public String data;    // 传递的数据内容

    public WsProtocol() {}
    public WsProtocol(String id, String action, String data) {
        this.id = id;
        this.action = action;
        this.data = data;
    }
}
