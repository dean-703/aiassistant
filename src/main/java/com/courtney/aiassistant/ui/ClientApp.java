package com.courtney.aiassistant.ui;

import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;

public class ClientApp extends BorderPane {

    private final MenuItem miNewConversation = new MenuItem("New Conversation");
    private final MenuItem miOpenConversation = new MenuItem("Open Conversation");
    private final MenuItem miSaveConversation = new MenuItem("Save Conversation");
    private final MenuItem miSaveHtml = new MenuItem("Save View as HTML");
    private final MenuItem miExit = new MenuItem("Exit");

    private final MenuItem miConfig = new MenuItem("Configuration Settings");
    private final MenuItem miBrowseConversations = new MenuItem("Conversation Manager");

    private final MenuItem miAssistant = new MenuItem("Assistant");
    private final MenuItem miDictionary = new MenuItem("Deutsch");
    private final MenuItem miHealthcare = new MenuItem("Healthcare");
    private final MenuItem miProgrammer = new MenuItem("Programmer");

    private final MenuItem miAbout = new MenuItem("About");

    private final TextArea inputArea = new TextArea();
    private final WebView webView = new WebView();
    private final Label statusLeft = new Label("Ready");
    private final Label statusRight = new Label("");

    private ClientAppController controller; // Add reference to the controller

    public ClientApp(ClientAppController controller) {
        this.controller = controller; // Initialize the controller reference
        setTop(buildMenu());
        setCenter(buildCenter());
        setBottom(buildBottom());
        getStyleClass().add("app-root");
    }

    private MenuBar buildMenu() {
        Menu file = new Menu("File");
        file.getItems().addAll(miNewConversation, miOpenConversation, miSaveConversation, miSaveHtml, new SeparatorMenuItem(), miExit);

        Menu tools = new Menu("Tools");
        tools.getItems().addAll(miConfig, miBrowseConversations);

        Menu presets = new Menu("Presets");
        presets.getItems().addAll(miAssistant,miDictionary, miHealthcare,miProgrammer);
// Help menu
        Menu help = new Menu("Help");

        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAboutDialog());

        help.getItems().add(about);

        MenuBar bar = new MenuBar(file, tools, presets, help);
        bar.setStyle("-fx-font-size: 18px;");
        return bar;
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        // Load an image and set it as the graphic for the alert
        Image img = new Image(getClass().getResourceAsStream("/chatgpt.png"));
        //Image image = new Image("file:///home/dean/Pictures/icons/chatgpt-128.png");
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(32);  // Set the width of the image
        imageView.setFitHeight(32); // Set the height of the image
        alert.setGraphic(imageView);

        // Set the custom icon
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        try {
              alertStage.getIcons().add(img);
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
        }

        alert.setTitle("About AI Assistant");
        alert.setHeaderText("AI Assistant OpenAI Desktop Client");
        alert.setContentText("""
            A JavaFX client for OpenAI ChatGPT
            
            Version 1.0  "HyperLink"
            
            Features:
            • Web search
            • Links open in system browser
            • Open, save, search and delete conversations
            • Adjust parameters, set system prompt
            • Easy on the eyes dark theme
            • Export view as HTML
            • Useful presets
            
            by Dean Courtney  dean703@gmail.com
            """);
        alert.showAndWait();
    }

    private Node buildCenter() {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setJavaScriptEnabled(true);
        VBox.setVgrow(webView, Priority.ALWAYS);

        LinkHandler linkHandler;
        linkHandler = new LinkHandler();

        // Listen to loading state changes
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Attach a listener to handle link clicks
                JSObject window = (JSObject) webView.getEngine().executeScript("window");
                window.setMember("app", linkHandler);

                // Add event listener for link clicks using JavaScript
                webView.getEngine().executeScript(
                        "document.body.addEventListener('click', function(event) { " +
                                "  if (event.target.tagName === 'A') { " +
                                "    event.preventDefault(); " +
                                "    app.openLink(event.target.href); " +
                                "  } " +
                                "});"
                );
            }
        });

        return webView;
    }

    private Node buildBottom() {
        inputArea.setPromptText("How can I help you?");
        inputArea.setStyle("-fx-font-size: 18px;");
        inputArea.setWrapText(true);
        inputArea.setPrefRowCount(3);

        // Add key event handling for the input area
        inputArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isControlDown()) {
                    // Add new line if Ctrl is pressed
                    inputArea.appendText(System.lineSeparator());
                    // Consume event to prevent sending the message
                    event.consume();
                } else {
                    // Send message if Enter is pressed without Ctrl
                    controller.sendMessage(); // Call sendMessage on the controller
                    event.consume(); // Prevent the default action (new line)
                }
            }
        });

        HBox inputBar = new HBox(8, inputArea);
        HBox.setHgrow(inputArea, Priority.ALWAYS);
        inputBar.setPadding(new Insets(10));
        inputBar.setAlignment(Pos.CENTER_RIGHT);
        inputBar.getStyleClass().add("input-bar");

        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(6, 10, 6, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusLeft.getStyleClass().add("status-left");
        statusRight.getStyleClass().add("status-right");
        statusBar.getChildren().addAll(statusLeft, spacer, statusRight);
        statusBar.getStyleClass().add("status-bar");

        VBox bottom = new VBox(inputBar, statusBar);
        return bottom;
    }

    public Scene createScene() {
        Scene scene = new Scene(this, 720, 840);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    // Expose components

    public WebEngine getEngine() { return webView.getEngine(); }
    public TextArea getInputArea() { return inputArea; }
    public MenuItem getMiNewConversation() { return miNewConversation; }
    public MenuItem getMiOpenConversation() { return miOpenConversation; }
    public MenuItem getMiSaveConversation() { return miSaveConversation; }
    public MenuItem getMiSaveHtml() { return miSaveHtml; }
    public MenuItem getMiExit() { return miExit; }
    public MenuItem getMiConfig() { return miConfig; }
    public MenuItem getMiBrowseConversations() { return miBrowseConversations; }
    public MenuItem getMiAssistant() { return miAssistant; }
    public MenuItem getMiDictionary() { return miDictionary; }
    public MenuItem getMiHealthcare() { return miHealthcare; }
    public MenuItem getMiProgrammer() { return miProgrammer; }
    public Label getStatusLeft() { return statusLeft; }
    public Label getStatusRight() { return statusRight; }
    public void focusInputArea() { inputArea.requestFocus(); }

    public class LinkHandler {
        public void openLink(String adr) throws IOException {
            new ProcessBuilder("xdg-open", adr).start();
            //new ProcessBuilder(firefoxPath, adr).start();
        }
    }

}

