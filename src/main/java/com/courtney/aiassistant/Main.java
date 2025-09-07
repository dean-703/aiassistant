package com.courtney.aiassistant;

import com.courtney.aiassistant.ui.ClientAppController;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        new ClientAppController().start(primaryStage);
    }
    public static void main(String[] args) {
        launch(args);
    }
}