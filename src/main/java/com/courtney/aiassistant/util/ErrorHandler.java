package com.courtney.aiassistant.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class ErrorHandler {

    public static void alert(String title, String message, AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    public static void toast(String title, String message) {
        alert(title, message, AlertType.INFORMATION);
    }
}
