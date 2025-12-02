package com.hamza.checkupdates;


import com.hamza.checkupdates.model.UpdateInfo;
import com.hamza.checkupdates.service.AutoUpdateDownloader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UpdateDialog {

    private final UpdateInfo updateInfo;
    private final String currentVersion;

    public UpdateDialog(UpdateInfo updateInfo, String currentVersion) {
        this.updateInfo = updateInfo;
        this.currentVersion = currentVersion;
    }

    public void show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("تحديث متوفر");
        dialog.setHeaderText("إصدار جديد: " + updateInfo.getVersion());

        // المحتوى
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label currentLabel = new Label("الإصدار الحالي: " + currentVersion);
        Label newLabel = new Label("الإصدار الجديد: " + updateInfo.getVersion());
        newLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label changelogLabel = new Label("التغييرات:");
        changelogLabel.setStyle("-fx-font-weight: bold;");

        TextArea changelogArea = new TextArea(updateInfo.getChangelog().get("ar"));
        changelogArea.setEditable(false);
        changelogArea.setPrefRowCount(5);
        changelogArea.setWrapText(true);

        if (updateInfo.getFileSize() != null) {
            double sizeMB = updateInfo.getFileSize() / (1024.0 * 1024.0);
            Label sizeLabel = new Label(String.format("حجم التحديث: %.2f ميجابايت", sizeMB));
            content.getChildren().add(sizeLabel);
        }

        if (updateInfo.isRequired()) {
            Label requiredLabel = new Label("⚠️ هذا التحديث إجباري!");
            requiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            content.getChildren().add(requiredLabel);
        }

        content.getChildren().addAll(currentLabel, newLabel, changelogLabel, changelogArea);

        dialog.getDialogPane().setContent(content);

        // الأزرار
        ButtonType downloadButton = new ButtonType("تحميل وتثبيت",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType laterButton = new ButtonType("لاحقاً",
                ButtonBar.ButtonData.CANCEL_CLOSE);

        if (updateInfo.isRequired()) {
            dialog.getDialogPane().getButtonTypes().setAll(downloadButton);
        } else {
            dialog.getDialogPane().getButtonTypes().setAll(downloadButton, laterButton);
        }

        dialog.showAndWait().ifPresent(response -> {
            if (response == downloadButton) {
                showDownloadProgress();
            }
        });
    }

    private void showDownloadProgress() {
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("جاري التحميل...");
        progressStage.setResizable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label statusLabel = new Label("جاري التحضير...");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        Label percentLabel = new Label("0%");

        layout.getChildren().addAll(statusLabel, progressBar, percentLabel);

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 350, 150);
        progressStage.setScene(scene);

        // بدء التحميل
        AutoUpdateDownloader downloader = new AutoUpdateDownloader();

        downloader.progressProperty().addListener((obs, oldVal, newVal) -> {
            progressBar.setProgress(newVal.doubleValue());
            percentLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
        });

        downloader.statusProperty().addListener((obs, oldVal, newVal) -> {
            statusLabel.setText(newVal);
        });

        downloader.downloadUpdate(updateInfo).thenAccept(file -> {
            javafx.application.Platform.runLater(() -> {
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDialog.setTitle("التحديث جاهز");
                confirmDialog.setHeaderText("تم تحميل التحديث بنجاح!");
                confirmDialog.setContentText("هل تريد تثبيت التحديث الآن؟\n" +
                        "سيتم إعادة تشغيل التطبيق.");

                confirmDialog.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        downloader.installUpdate(file);
                    }
                });

                progressStage.close();
            });
        }).exceptionally(throwable -> {
            javafx.application.Platform.runLater(() -> {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("خطأ");
                errorAlert.setHeaderText("فشل تحميل التحديث");
                errorAlert.setContentText(throwable.getMessage());
                errorAlert.showAndWait();
                progressStage.close();
            });
            return null;
        });

        progressStage.show();
    }
}
