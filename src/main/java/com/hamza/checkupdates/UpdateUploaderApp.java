package com.hamza.checkupdates;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import okhttp3.*;

import java.io.File;
import java.io.IOException;

import static com.hamza.checkupdates.service.UpdateService.SERVER_URL;


public class UpdateUploaderApp extends Application {

//    private static final String SERVER_URL = "http://localhost:8080";

    private TextField versionField;
    private TextArea changelogArField;
    private TextArea changelogEnField;
    private TextField minVersionField;
    private CheckBox requiredCheckbox;
    private Label fileLabel;
    private ProgressBar progressBar;
    private Label statusLabel;
    private File selectedFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("أداة رفع التحديثات - Admin");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // العنوان
        Label title = new Label("🚀 رفع تحديث جديد");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // رقم الإصدار
        HBox versionBox = new HBox(10);
        versionBox.setAlignment(Pos.CENTER_LEFT);
        Label versionLabel = new Label("رقم الإصدار:");
        versionLabel.setPrefWidth(150);
        versionField = new TextField();
        versionField.setPromptText("مثال: 1.0.1");
        versionBox.getChildren().addAll(versionLabel, versionField);

        // اختيار الملف
        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        Button chooseFileBtn = new Button("اختر ملف JAR");
        fileLabel = new Label("لم يتم اختيار ملف");
        fileLabel.setStyle("-fx-text-fill: gray;");
        chooseFileBtn.setOnAction(e -> chooseFile(primaryStage));
        fileBox.getChildren().addAll(chooseFileBtn, fileLabel);

        // التغييرات عربي
        Label changelogArLabel = new Label("التغييرات (عربي):");
        changelogArField = new TextArea();
        changelogArField.setPromptText("اكتب التغييرات بالعربي...");
        changelogArField.setPrefRowCount(4);

        // التغييرات English
        Label changelogEnLabel = new Label("التغييرات (English):");
        changelogEnField = new TextArea();
        changelogEnField.setPromptText("Write changes in English...");
        changelogEnField.setPrefRowCount(4);

        // الحد الأدنى للإصدار
        HBox minVersionBox = new HBox(10);
        minVersionBox.setAlignment(Pos.CENTER_LEFT);
        Label minVersionLabel = new Label("الحد الأدنى للإصدار:");
        minVersionLabel.setPrefWidth(150);
        minVersionField = new TextField();
        minVersionField.setPromptText("مثال: 1.0.0");
        minVersionBox.getChildren().addAll(minVersionLabel, minVersionField);

        // تحديث إجباري
        requiredCheckbox = new CheckBox("تحديث إجباري");

        // شريط التقدم
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        progressBar.setVisible(false);

        // حالة الرفع
        statusLabel = new Label();
        statusLabel.setVisible(false);

        // زر الرفع
        Button uploadBtn = new Button("📤 رفع التحديث");
        uploadBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10px 40px;");
        uploadBtn.setOnAction(e -> uploadUpdate());

        root.getChildren().addAll(
                title,
                new Separator(),
                versionBox,
                fileBox,
                changelogArLabel,
                changelogArField,
                changelogEnLabel,
                changelogEnField,
                minVersionBox,
                requiredCheckbox,
                uploadBtn,
                progressBar,
                statusLabel
        );

        Scene scene = new Scene(root, 600, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void chooseFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختر ملف JAR");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JAR Files", "*.jar")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedFile = file;
            fileLabel.setText(file.getName());
            fileLabel.setStyle("-fx-text-fill: green;");
        }
    }

    private void uploadUpdate() {
        // التحقق من الحقول
        if (versionField.getText().isEmpty()) {
            showError("يجب إدخال رقم الإصدار");
            return;
        }

        if (selectedFile == null) {
            showError("يجب اختيار ملف JAR");
            return;
        }

        if (changelogArField.getText().isEmpty() || changelogEnField.getText().isEmpty()) {
            showError("يجب كتابة التغييرات بالعربي والإنجليزي");
            return;
        }

        // إظهار التقدم
        progressBar.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.setText("جاري الرفع...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        // بناء الطلب
        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(selectedFile, MediaType.parse("application/java-archive"));
//        RequestBody progressBody = new RequestBody() {
//            @Override
//            public MediaType contentType() {
//                return MediaType.parse("application/java-archive");
//            }
//
//            @Override
//            public void writeTo(BufferedSink sink) throws IOException {
//                try (Source source = Okio.source(selectedFile)) {
//                    long total = selectedFile.length();
//                    long uploaded = 0;
//                    Buffer buffer = new Buffer();
//                    long read;
//                    while ((read = source.read(buffer, 8192)) != -1) {
//                        uploaded += read;
//                        sink.write(buffer, read);
//                        final double progress = (double) uploaded / total;
//                        javafx.application.Platform.runLater(() ->
//                                progressBar.setProgress(progress)
//                        );
//                    }
//                }
//            }
//        };

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", selectedFile.getName(), fileBody)
                .addFormDataPart("version", versionField.getText())
                .addFormDataPart("changelogAr", changelogArField.getText())
                .addFormDataPart("changelogEn", changelogEnField.getText())
                .addFormDataPart("required", String.valueOf(requiredCheckbox.isSelected()))
                .addFormDataPart("minSupportedVersion", minVersionField.getText())
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/api/admin/upload")
                .post(requestBody)
                .build();

        // تنفيذ الطلب
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    showError("فشل الاتصال: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);

                    if (response.isSuccessful()) {
                        showSuccess("✅ تم رفع التحديث بنجاح!");
                        clearForm();
                    } else {
                        showError("❌ فشل الرفع: " + response.message());
                    }
                });
            }
        });
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setVisible(true);
    }

    private void clearForm() {
        versionField.clear();
        changelogArField.clear();
        changelogEnField.clear();
        minVersionField.clear();
        requiredCheckbox.setSelected(false);
        selectedFile = null;
        fileLabel.setText("لم يتم اختيار ملف");
        fileLabel.setStyle("-fx-text-fill: gray;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
