package com.hamza.checkupdates.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import com.hamza.checkupdates.model.ClientInfo;
import com.hamza.checkupdates.model.UpdateInfo;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class UpdateService {
    public static final String SERVER_URL = "http://164.92.230.242:8080";
    private static final String CURRENT_VERSION = "3.1.2"; // يمكن قراءتها من ملف properties

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Preferences prefs;
    private String clientId;

    public UpdateService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(30))
                .build();

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                                LocalDateTime.parse(json.getAsString(),
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();

        this.prefs = Preferences.userNodeForPackage(UpdateService.class);
        this.clientId = getOrCreateClientId();
    }

    private String getOrCreateClientId() {
        String id = prefs.get("client_id", null);
        if (id == null) {
            // طلب ID جديد من السيرفر
            try {
                Request request = new Request.Builder()
                        .url(SERVER_URL + "/api/client-id")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        id = response.body().string();
                        prefs.put("client_id", id);
                    }
                }
            } catch (IOException e) {
                // في حالة الفشل، إنشاء ID محلي
                id = java.util.UUID.randomUUID().toString();
                prefs.put("client_id", id);
            }
        }
        return id;
    }

    public CompletableFuture<UpdateInfo> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ClientInfo clientInfo = collectClientInfo();

                HttpUrl url = HttpUrl.parse(SERVER_URL + "/api/version").newBuilder()
                        .addQueryParameter("clientId", clientInfo.getClientId())
                        .addQueryParameter("currentVersion", clientInfo.getCurrentVersion())
                        .addQueryParameter("osName", clientInfo.getOsName())
                        .addQueryParameter("osVersion", clientInfo.getOsVersion())
                        .addQueryParameter("javaVersion", clientInfo.getJavaVersion())
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        return gson.fromJson(json, UpdateInfo.class);
                    }
                }
            } catch (Exception e) {
                System.err.println("فشل التحقق من التحديثات: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    private ClientInfo collectClientInfo() {
        ClientInfo info = new ClientInfo();
        info.setClientId(clientId);
        info.setCurrentVersion(CURRENT_VERSION);
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setJavaVersion(System.getProperty("java.version"));
        return info;
    }

    public boolean isUpdateAvailable(UpdateInfo updateInfo) {
        if (updateInfo == null) return false;
        return compareVersions(CURRENT_VERSION, updateInfo.getVersion()) < 0;
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public String getClientId() {
        return clientId;
    }
}
