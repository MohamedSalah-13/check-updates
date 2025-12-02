package com.hamza.checkupdates;


import com.hamza.checkupdates.model.UpdateInfo;
import com.hamza.checkupdates.service.UpdateService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UpdateCheckerApp extends Application {

    private UpdateService updateService;

    // UI Components
    private Label statusLabel;
    private Label currentVersionLabel;
    private Label latestVersionLabel;
    private TextArea changelogArea;
    private ProgressBar downloadProgressBar;
    private Label progressLabel;
    private Button checkButton;
    private Button downloadButton;
    private Button installButton;
    private VBox updateInfoBox;

    private UpdateInfo latestUpdate;
    private File downloadedFile;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        updateService = new UpdateService();

        primaryStage.setTitle("🔄 مدير التحديثات");
        primaryStage.setResizable(false);

        // Root Layout
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        // Header
        VBox header = createHeader();

        // Version Info Card
        VBox versionCard = createVersionCard();

        // Update Info Card
        updateInfoBox = createUpdateInfoCard();
        updateInfoBox.setVisible(false);
        updateInfoBox.setManaged(false);

        // Download Progress Card
        VBox progressCard = createProgressCard();
        progressCard.setVisible(false);
        progressCard.setManaged(false);

        // Action Buttons
        HBox actionButtons = createActionButtons(progressCard);

        root.getChildren().addAll(header, versionCard, updateInfoBox, progressCard, actionButtons);

        Scene scene = new Scene(root, 700, 800);
//        Style_Sheet.changeStyle(scene);
//        ChangeOrientation.sceneOrientation(scene);
//        primaryStage.getIcons().add(new javafx.scene.image.Image(new Image_Setting().update));
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
//        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.show();

        // Check for updates on startup (optional)
        // checkForUpdates(progressCard);
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label title = new Label("🔄 مدير التحديثات");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("تحقق من التحديثات وقم بتنزيلها بسهولة");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #e0e0e0;");

        statusLabel = new Label("جاهز للتحقق من التحديثات");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b0b0b0;");

        header.getChildren().addAll(title, subtitle, statusLabel);
        return header;
    }

    private VBox createVersionCard() {
        VBox card = createCard("📊 معلومات الإصدار");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 0, 0));

        // Current Version
        Label currentLabel = new Label("الإصدار الحالي:");
        currentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        currentVersionLabel = new Label(updateService.getCurrentVersion());
        currentVersionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #667eea;");

        // Latest Version
        Label latestLabel = new Label("أحدث إصدار:");
        latestLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        latestVersionLabel = new Label("---");
        latestVersionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #667eea;");

        grid.add(currentLabel, 0, 0);
        grid.add(currentVersionLabel, 1, 0);
        grid.add(latestLabel, 0, 1);
        grid.add(latestVersionLabel, 1, 1);

        card.getChildren().add(grid);
        return card;
    }

    private VBox createUpdateInfoCard() {
        VBox card = createCard("📝 تفاصيل التحديث");

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 0, 0, 0));

        // Release Date
        Label releaseDateLabel = new Label();
        releaseDateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // File Size
        Label fileSizeLabel = new Label();
        fileSizeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Required Badge
        Label requiredBadge = new Label();
        requiredBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        // Changelog
        Label changelogLabel = new Label("التغييرات:");
        changelogLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        changelogArea = new TextArea();
        changelogArea.setEditable(false);
        changelogArea.setWrapText(true);
        changelogArea.setPrefRowCount(6);
        changelogArea.setStyle("-fx-font-size: 12px;");

        content.getChildren().addAll(
                releaseDateLabel,
                fileSizeLabel,
                requiredBadge,
                changelogLabel,
                changelogArea
        );

        card.getChildren().add(content);
        card.setUserData(new Object[]{releaseDateLabel, fileSizeLabel, requiredBadge});

        return card;
    }

    private VBox createProgressCard() {
        VBox card = createCard("⬇️ التنزيل");

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 0, 0, 0));

        progressLabel = new Label("في انتظار بدء التنزيل...");
        progressLabel.setStyle("-fx-font-size: 12px;");

        downloadProgressBar = new ProgressBar(0);
        downloadProgressBar.setPrefWidth(600);
        downloadProgressBar.setPrefHeight(25);

        Label percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Bind percent label to progress
        downloadProgressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            percentLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
        });

        content.getChildren().addAll(progressLabel, downloadProgressBar, percentLabel);
        card.getChildren().add(content);

        return card;
    }

    private HBox createActionButtons(VBox progressCard) {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // Check Button
        checkButton = new Button("🔍 التحقق من التحديثات");
        checkButton.setStyle(getButtonStyle("#667eea"));
        checkButton.setPrefWidth(200);
        checkButton.setPrefHeight(45);
        checkButton.setOnAction(e -> checkForUpdates(progressCard));

        // Download Button
        downloadButton = new Button("⬇️ تنزيل التحديث");
        downloadButton.setStyle(getButtonStyle("#28a745"));
        downloadButton.setPrefWidth(200);
        downloadButton.setPrefHeight(45);
        downloadButton.setDisable(true);
        downloadButton.setOnAction(e -> downloadUpdate(progressCard));

        // Install Button
        installButton = new Button("🚀 تثبيت التحديث");
        installButton.setStyle(getButtonStyle("#ffc107"));
        installButton.setPrefWidth(200);
        installButton.setPrefHeight(45);
        installButton.setDisable(true);
        installButton.setOnAction(e -> installUpdate());

        buttonBox.getChildren().addAll(checkButton, downloadButton, installButton);
        return buttonBox;
    }

    private VBox createCard(String title) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-padding: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        card.getChildren().add(titleLabel);
        return card;
    }

    private String getButtonStyle(String color) {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand;",
                color
        );
    }

    private void checkForUpdates(VBox progressCard) {
        setStatus("🔍 جاري التحقق من التحديثات...", Color.BLUE);
        checkButton.setDisable(true);
        downloadButton.setDisable(true);
        installButton.setDisable(true);

        updateService.checkForUpdates().thenAccept(updateInfo -> {
            Platform.runLater(() -> {
                if (updateInfo == null) {
                    setStatus("❌ فشل الاتصال بالسيرفر", Color.RED);
                    checkButton.setDisable(false);
                    return;
                }

                latestUpdate = updateInfo;
                latestVersionLabel.setText(updateInfo.getVersion());

                if (updateService.isUpdateAvailable(updateInfo)) {
                    setStatus("✅ يوجد تحديث جديد متوفر!", Color.GREEN);
                    displayUpdateInfo(updateInfo);
                    downloadButton.setDisable(false);
                } else {
                    setStatus("✅ أنت تستخدم أحدث إصدار", Color.GREEN);
                    updateInfoBox.setVisible(false);
                    updateInfoBox.setManaged(false);
                }

                checkButton.setDisable(false);
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                setStatus("❌ خطأ: " + throwable.getMessage(), Color.RED);
                checkButton.setDisable(false);
            });
            return null;
        });
    }

    private void displayUpdateInfo(UpdateInfo updateInfo) {
        updateInfoBox.setVisible(true);
        updateInfoBox.setManaged(true);

        Object[] labels = (Object[]) updateInfoBox.getUserData();
        Label releaseDateLabel = (Label) labels[0];
        Label fileSizeLabel = (Label) labels[1];
        Label requiredBadge = (Label) labels[2];

        // Release Date
        String dateStr = updateInfo.getReleaseDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        releaseDateLabel.setText("📅 تاريخ الإصدار: " + dateStr);

        // File Size
        if (updateInfo.getFileSize() != null) {
            double sizeMB = updateInfo.getFileSize() / (1024.0 * 1024.0);
            fileSizeLabel.setText(String.format("💾 حجم الملف: %.2f ميجابايت", sizeMB));
        }

        // Required Badge
        if (updateInfo.isRequired()) {
            requiredBadge.setText("⚠️ هذا التحديث إجباري!");
            requiredBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: red;");
        } else {
            requiredBadge.setText("ℹ️ تحديث اختياري");
            requiredBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        }

        // Changelog
        String changelog = updateInfo.getChangelog().getOrDefault("ar", "لا توجد معلومات");
        changelogArea.setText(changelog);
    }

    private void downloadUpdate(VBox progressCard) {
        if (latestUpdate == null) return;

        progressCard.setVisible(true);
        progressCard.setManaged(true);

        setStatus("⬇️ جاري التنزيل...", Color.BLUE);
        downloadButton.setDisable(true);
        checkButton.setDisable(true);

        progressLabel.setText("جاري الاتصال بالسيرفر...");
        downloadProgressBar.setProgress(0);

        // Create download directory
        String downloadDir = System.getProperty("user.home") + "/.myapp/updates";
        new File(downloadDir).mkdirs();

        String fileName = "update-" + latestUpdate.getVersion() + ".jar";
        String savePath = downloadDir + "/" + fileName;

        // Download in background thread
        new Thread(() -> {
            try {
                boolean success = downloadWithProgress(
                        latestUpdate.getDownloadUrl(),
                        savePath,
                        latestUpdate.getChecksum()
                );

                Platform.runLater(() -> {
                    if (success) {
                        downloadedFile = new File(savePath);
                        setStatus("✅ تم التنزيل بنجاح!", Color.GREEN);
                        progressLabel.setText("✅ التنزيل مكتمل!");
                        installButton.setDisable(false);

                        showSuccessAlert();
                    } else {
                        setStatus("❌ فشل التنزيل", Color.RED);
                        progressLabel.setText("❌ حدث خطأ أثناء التنزيل");
                        checkButton.setDisable(false);
                        downloadButton.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("❌ خطأ: " + e.getMessage(), Color.RED);
                    progressLabel.setText("❌ " + e.getMessage());
                    checkButton.setDisable(false);
                    downloadButton.setDisable(false);
                });
            }
        }).start();
    }

    private boolean downloadWithProgress(String downloadUrl, String savePath, String checksum) {
        try {
            java.net.URL url = new java.net.URL(downloadUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            long fileSize = conn.getContentLengthLong();

            try (java.io.InputStream in = conn.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(savePath)) {

                byte[] buffer = new byte[8192];
                long totalBytesRead = 0;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    long finalTotalBytesRead = totalBytesRead;
                    Platform.runLater(() -> {
                        if (fileSize > 0) {
                            double progress = (double) finalTotalBytesRead / fileSize;
                            downloadProgressBar.setProgress(progress);

                            double downloadedMB = finalTotalBytesRead / (1024.0 * 1024.0);
                            double totalMB = fileSize / (1024.0 * 1024.0);
                            progressLabel.setText(String.format(
                                    "جاري التنزيل: %.2f / %.2f ميجابايت",
                                    downloadedMB, totalMB
                            ));
                        }
                    });
                }
            }

            // Verify checksum
            if (checksum != null && !checksum.isEmpty()) {
                Platform.runLater(() -> progressLabel.setText("جاري التحقق من الملف..."));

                String actualChecksum = calculateChecksum(savePath);
                if (!actualChecksum.equalsIgnoreCase(checksum)) {
                    new File(savePath).delete();
                    throw new Exception("الملف المحمل تالف (checksum لا يتطابق)");
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String calculateChecksum(String filePath) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
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

    private void installUpdate() {
        if (downloadedFile == null || !downloadedFile.exists()) {
            showErrorAlert("ملف التحديث غير موجود!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("تأكيد التثبيت");
        confirmAlert.setHeaderText("هل تريد تثبيت التحديث الآن؟");
        confirmAlert.setContentText(
                "سيتم إغلاق التطبيق وتثبيت التحديث.\n" +
                        "سيتم إعادة تشغيل التطبيق تلقائياً بعد التثبيت."
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performInstallation();
        }
    }

    private void performInstallation() {
        try {
            // Get current JAR path
            String currentJar = UpdateCheckerApp.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File currentFile = new File(currentJar);
            File backupFile = new File(currentFile.getParent(),
                    currentFile.getName() + ".backup");

            // Create update script
            File scriptFile = createUpdateScript(downloadedFile, currentFile, backupFile);

            // Execute script and exit
            executeUpdateScript(scriptFile);

        } catch (Exception e) {
            showErrorAlert("فشل تثبيت التحديث: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File createUpdateScript(File updateFile, File currentFile,
                                    File backupFile) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        File scriptFile;

        if (os.contains("win")) {
            scriptFile = new File(System.getProperty("user.home"),
                    ".myapp/update.bat");
            try (java.io.PrintWriter writer = new java.io.PrintWriter(scriptFile, "UTF-8")) {
                writer.println("@echo off");
                writer.println("timeout /t 2 /nobreak > nul");
                writer.println("copy /Y \"" + currentFile.getAbsolutePath() +
                        "\" \"" + backupFile.getAbsolutePath() + "\"");
                writer.println("copy /Y \"" + updateFile.getAbsolutePath() +
                        "\" \"" + currentFile.getAbsolutePath() + "\"");
                writer.println("start \"\" \"" + currentFile.getAbsolutePath() + "\"");
                writer.println("del \"%~f0\"");
            }
        } else {
            scriptFile = new File(System.getProperty("user.home"),
                    ".myapp/update.sh");
            try (java.io.PrintWriter writer = new java.io.PrintWriter(scriptFile, "UTF-8")) {
                writer.println("#!/bin/bash");
                writer.println("sleep 2");
                writer.println("cp -f '" + currentFile.getAbsolutePath() +
                        "' '" + backupFile.getAbsolutePath() + "'");
                writer.println("cp -f '" + updateFile.getAbsolutePath() +
                        "' '" + currentFile.getAbsolutePath() + "'");
                writer.println("java -jar '" + currentFile.getAbsolutePath() + "' &");
                writer.println("rm -f \"$0\"");
            }
            scriptFile.setExecutable(true);
        }

        return scriptFile;
    }

    private void executeUpdateScript(File scriptFile) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", scriptFile.getAbsolutePath());
        } else {
            pb = new ProcessBuilder("sh", scriptFile.getAbsolutePath());
        }

        pb.start();
        Platform.exit();
        System.exit(0);
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("نجح التنزيل");
        alert.setHeaderText("تم تنزيل التحديث بنجاح! ✅");
        alert.setContentText("يمكنك الآن تثبيت التحديث.");
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("خطأ");
        alert.setHeaderText("حدث خطأ");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
