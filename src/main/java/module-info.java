module com.hamza.checkupdates {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires okhttp3;
    requires com.google.gson;
    requires java.prefs;
    requires static lombok;
    requires org.java_websocket;

    opens com.hamza.checkupdates to javafx.fxml;
    exports com.hamza.checkupdates;
    exports com.hamza.checkupdates.service;
    opens com.hamza.checkupdates.service to javafx.fxml;
}