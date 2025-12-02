package com.hamza.checkupdates.service;

import com.hamza.checkupdates.model.UpdateInfo;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

public class AutoUpdateDownloader {

    private final OkHttpClient httpClient;
    private final DoubleProperty progress;
    private final StringProperty status;

    public AutoUpdateDownloader() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofMinutes(5))
                .readTimeout(java.time.Duration.ofMinutes(5))
                .build();
        this.progress = new SimpleDoubleProperty(0);
        this.status = new SimpleStringProperty("جاري التحضير...");
    }

    public CompletableFuture<File> downloadUpdate(UpdateInfo updateInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                updateStatus("جاري التحميل...");

                // إنشاء مجلد مؤقت للتحديثات
                Path updateDir = Paths.get(System.getProperty("user.home"),
                        ".myapp", "updates");
                Files.createDirectories(updateDir);

                // اسم الملف
                String fileName = "update-" + updateInfo.getVersion() + ".jar";
                File downloadFile = updateDir.resolve(fileName).toFile();

                // تحميل الملف
                Request request = new Request.Builder()
                        .url(updateInfo.getDownloadUrl())
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("فشل التحميل: " + response.code());
                    }

                    if (response.body() == null) {
                        throw new IOException("استجابة فارغة من السيرفر");
                    }

                    long fileSize = updateInfo.getFileSize() != null ?
                            updateInfo.getFileSize() :
                            response.body().contentLength();

                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(downloadFile)) {

                        byte[] buffer = new byte[8192];
                        long downloaded = 0;
                        int read;

                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                            downloaded += read;

                            if (fileSize > 0) {
                                double progressValue = (double) downloaded / fileSize;
                                updateProgress(progressValue);
                            }
                        }
                    }
                }

                updateStatus("جاري التحقق من الملف...");

                // التحقق من checksum إذا كان متوفراً
                if (updateInfo.getChecksum() != null && !updateInfo.getChecksum().isEmpty()) {
                    String fileChecksum = calculateChecksum(downloadFile);
                    if (!fileChecksum.equalsIgnoreCase(updateInfo.getChecksum())) {
                        downloadFile.delete();
                        throw new IOException("الملف المحمل تالف (checksum لا يتطابق)");
                    }
                }

                updateStatus("اكتمل التحميل بنجاح!");
                updateProgress(1.0);

                return downloadFile;

            } catch (Exception e) {
                updateStatus("فشل التحميل: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private String calculateChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void installUpdate(File updateFile) {
        try {
            // الحصول على مسار ملف JAR الحالي
            String currentJar = AutoUpdateDownloader.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File currentFile = new File(currentJar);
            File backupFile = new File(currentFile.getParent(),
                    currentFile.getName() + ".backup");

            // إنشاء نسخة احتياطية
            Files.copy(currentFile.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // كتابة سكريبت للتحديث
            File scriptFile = createUpdateScript(updateFile, currentFile, backupFile);

            // تشغيل السكريبت وإغلاق التطبيق
            executeUpdateScript(scriptFile);

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("فشل تثبيت التحديث: " + e.getMessage());
        }
    }

    private File createUpdateScript(File updateFile, File currentFile,
                                    File backupFile) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        File scriptFile;

        if (os.contains("win")) {
            // Windows batch script
            scriptFile = new File(System.getProperty("user.home"),
                    ".myapp/update.bat");
            try (PrintWriter writer = new PrintWriter(scriptFile, "UTF-8")) {
                writer.println("@echo off");
                writer.println("echo Waiting for application to close...");
                writer.println("timeout /t 3 /nobreak");
                writer.println("echo Installing update...");
                writer.println("copy /Y \"" + updateFile.getAbsolutePath() +
                        "\" \"" + currentFile.getAbsolutePath() + "\"");
                writer.println("if errorlevel 1 (");
                writer.println("  echo Update failed! Restoring backup...");
                writer.println("  copy /Y \"" + backupFile.getAbsolutePath() +
                        "\" \"" + currentFile.getAbsolutePath() + "\"");
                writer.println(") else (");
                writer.println("  echo Update successful!");
                writer.println("  del \"" + backupFile.getAbsolutePath() + "\"");
                writer.println(")");
                writer.println("echo Starting application...");
                writer.println("start \"\" \"" + currentFile.getAbsolutePath() + "\"");
                writer.println("del \"%~f0\"");
            }
        } else {
            // Unix/Linux/Mac bash script
            scriptFile = new File(System.getProperty("user.home"),
                    ".myapp/update.sh");
            try (PrintWriter writer = new PrintWriter(scriptFile, "UTF-8")) {
                writer.println("#!/bin/bash");
                writer.println("echo 'Waiting for application to close...'");
                writer.println("sleep 3");
                writer.println("echo 'Installing update...'");
                writer.println("if cp -f '" + updateFile.getAbsolutePath() +
                        "' '" + currentFile.getAbsolutePath() + "'; then");
                writer.println("  echo 'Update successful!'");
                writer.println("  rm -f '" + backupFile.getAbsolutePath() + "'");
                writer.println("else");
                writer.println("  echo 'Update failed! Restoring backup...'");
                writer.println("  cp -f '" + backupFile.getAbsolutePath() +
                        "' '" + currentFile.getAbsolutePath() + "'");
                writer.println("fi");
                writer.println("echo 'Starting application...'");
                writer.println("java -jar '" + currentFile.getAbsolutePath() + "' &");
                writer.println("rm -f \"$0\"");
            }
            scriptFile.setExecutable(true);
        }

        return scriptFile;
    }

    private void executeUpdateScript(File scriptFile) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", scriptFile.getAbsolutePath());
        } else {
            pb = new ProcessBuilder("sh", scriptFile.getAbsolutePath());
        }

        pb.start();

        // إغلاق التطبيق
        Platform.exit();
        System.exit(0);
    }

    private void updateProgress(double value) {
        Platform.runLater(() -> progress.set(value));
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> status.set(message));
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty statusProperty() {
        return status;
    }
}
