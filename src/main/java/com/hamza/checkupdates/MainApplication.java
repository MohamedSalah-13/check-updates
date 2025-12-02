package com.hamza.checkupdates;


import com.hamza.checkupdates.service.UpdateNotificationService;
import com.hamza.checkupdates.service.UpdateService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainApplication extends Application {

    private UpdateService updateService;
    private UpdateNotificationService notificationService;
    private ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // تهيئة الخدمات
        updateService = new UpdateService();
        notificationService = new UpdateNotificationService();

        // إعداد التطبيق الرئيسي
        // ... كود التطبيق العادي

        // التحقق من التحديثات عند بدء التطبيق
        checkForUpdatesOnStartup();

        // بدء خدمة الإشعارات الفورية
        startRealtimeNotifications();

        // جدولة فحص دوري (كل 6 ساعات كنسخة احتياطية)
        schedulePeriodicUpdateCheck();

        primaryStage.setOnCloseRequest(event -> {
            cleanup();
        });
    }

    private void checkForUpdatesOnStartup() {
        updateService.checkForUpdates().thenAccept(updateInfo -> {
            if (updateInfo != null && updateService.isUpdateAvailable(updateInfo)) {
                Platform.runLater(() -> {
                    UpdateDialog dialog = new UpdateDialog(
                            updateInfo,
                            updateService.getCurrentVersion()
                    );
                    dialog.show();
                });
            }
        });
    }

    private void startRealtimeNotifications() {
        // الاتصال بـ WebSocket
        notificationService.connect();

        // إضافة مستمع للتحديثات الفورية
        notificationService.addUpdateListener(updateInfo -> {
            if (updateService.isUpdateAvailable(updateInfo)) {
                Platform.runLater(() -> {
                    UpdateDialog dialog = new UpdateDialog(
                            updateInfo,
                            updateService.getCurrentVersion()
                    );
                    dialog.show();
                });
            }
        });
    }

    private void schedulePeriodicUpdateCheck() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            updateService.checkForUpdates().thenAccept(updateInfo -> {
                if (updateInfo != null && updateService.isUpdateAvailable(updateInfo)) {
                    Platform.runLater(() -> {
                        UpdateDialog dialog = new UpdateDialog(
                                updateInfo,
                                updateService.getCurrentVersion()
                        );
                        dialog.show();
                    });
                }
            });
        }, 6, 6, TimeUnit.HOURS);
    }

    private void cleanup() {
        if (notificationService != null) {
            notificationService.disconnect();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}