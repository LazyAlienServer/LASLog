package com.las.backend.service.projectmanager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface WsServerService {

    CompletableFuture<String> sendAndAwait(String action, String data) throws IOException;

    void onResultReceived(String id, String result);
}
