package com.hamza.checkupdates.service;

import com.google.gson.Gson;
import com.hamza.checkupdates.model.UpdateInfo;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UpdateNotificationService {
    private static final String WS_URL = "ws://localhost:8080/ws/websocket";

    private WebSocketClient webSocketClient;
    private final Gson gson;
    private final List<Consumer<UpdateInfo>> listeners;

    public UpdateNotificationService() {
        this.gson = new Gson();
        this.listeners = new ArrayList<>();
    }

    public void connect() {
        try {
            webSocketClient = new WebSocketClient(new URI(WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("اتصال WebSocket مفتوح");
                    // الاشتراك في قناة التحديثات
                    send("SUBSCRIBE\nid:sub-0\ndestination:/topic/updates\n\n\0");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("رسالة مستلمة: " + message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("اتصال WebSocket مغلق: " + reason);
                    // إعادة الاتصال بعد 5 ثوانٍ
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("خطأ في WebSocket: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };

            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String message) {
        try {
            // تحليل رسالة STOMP
            if (message.contains("MESSAGE")) {
                String[] lines = message.split("\n");
                StringBuilder jsonBuilder = new StringBuilder();
                boolean readingBody = false;

                for (String line : lines) {
                    if (readingBody) {
                        jsonBuilder.append(line);
                    }
                    if (line.isEmpty()) {
                        readingBody = true;
                    }
                }

                String json = jsonBuilder.toString().replace("\0", "");
                if (!json.isEmpty()) {
                    UpdateInfo updateInfo = gson.fromJson(json, UpdateInfo.class);
                    notifyListeners(updateInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyListeners(UpdateInfo updateInfo) {
        Platform.runLater(() -> {
            for (Consumer<UpdateInfo> listener : listeners) {
                listener.accept(updateInfo);
            }
        });
    }

    public void addUpdateListener(Consumer<UpdateInfo> listener) {
        listeners.add(listener);
    }

    public void removeUpdateListener(Consumer<UpdateInfo> listener) {
        listeners.remove(listener);
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}
