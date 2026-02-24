package com.las.backend.service.projectManager;

import java.util.concurrent.CompletableFuture;

public interface WsServerService {
    CompletableFuture<String> sendAndAwait(String action, String data) throws Exception;

    void onResultReceived(String id, String result);
}
