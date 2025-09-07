package com.courtney.aiassistant.ui;

import com.courtney.aiassistant.model.AppSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class AppConfig {

    private final Stage stage = new Stage();
    private final TextField modelTextField = new TextField();
    private final Slider temperature = new Slider(0, 2, 0.7);
    private final TextField maxTokensTextField = new TextField();
    private final TextArea systemPrompt = new TextArea();

    private AppSettings workingCopy;
    private boolean okPressed = false;

    public AppConfig(Stage owner, AppSettings settings) {
        this.workingCopy = settings.copy();

        stage.setTitle("Configuration Settings");
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        // Configure temperature slider
        temperature.setShowTickLabels(true);
        temperature.setShowTickMarks(true);
        temperature.setMajorTickUnit(0.5);
        temperature.setMinorTickCount(4);
        temperature.setBlockIncrement(0.1);

        // Configure system prompt text area
        systemPrompt.setPromptText("System prompt (optional)...");
        systemPrompt.setWrapText(true);
        systemPrompt.setPrefRowCount(5);

        // Populate fields from settings
        populateFrom(workingCopy);

        // Top VBox for Temperature
        VBox topVBox = new VBox(10);
        topVBox.setPadding(new Insets(15));
        topVBox.getChildren().addAll(new Label("Temperature:"), temperature);

        // Left VBox for Model and Max Tokens
        VBox leftVBox = new VBox(10);
        leftVBox.setPadding(new Insets(15));
        leftVBox.getChildren().addAll(
                new Label("Model:"), modelTextField,
                new Label("Max Tokens:"), maxTokensTextField
        );

        // Center VBox for System Prompt
        VBox centerVBox = new VBox(10);
        centerVBox.setPadding(new Insets(15));
        centerVBox.getChildren().addAll(new Label("System Prompt:"), systemPrompt);

        // Bottom HBox for Buttons
        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        ToolBar tb = new ToolBar(btnOk,btnCancel);
        HBox hbox = new HBox(tb);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(0, 20, 10, 20));

        // BorderPane layout
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(topVBox);
        borderPane.setLeft(leftVBox);
        borderPane.setCenter(centerVBox);
        borderPane.setBottom(hbox);

        // Scene setup
        Scene scene = new Scene(borderPane, 660, 360);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);

        // Button actions
        btnOk.setOnAction(e -> {
            applyTo(workingCopy);
            okPressed = true;
            stage.close();
        });

        btnCancel.setOnAction(e -> {
            okPressed = false;
            stage.close();
        });
    }

    private void populateFrom(AppSettings s) {
        modelTextField.setText(s.getModel());
        temperature.setValue(s.getTemperature());
        maxTokensTextField.setText(String.valueOf(s.getMaxTokens()));
        systemPrompt.setText(s.getSystemPrompt() == null ? "" : s.getSystemPrompt());
    }

    private void applyTo(AppSettings s) {
        s.setModel(modelTextField.getText().trim());
        s.setTemperature(temperature.getValue());
        try {
            s.setMaxTokens(Integer.parseInt(maxTokensTextField.getText().trim()));
        } catch (NumberFormatException e) {
            // Handle invalid number input
            s.setMaxTokens(1024); // Default value or handle as needed
        }
        s.setSystemPrompt(systemPrompt.getText().trim());
    }

    public Optional<AppSettings> showAndWait() {
        stage.showAndWait();
        if (okPressed) {
            return Optional.of(workingCopy);
        }
        return Optional.empty();
    }
}