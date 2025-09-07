package com.courtney.aiassistant.ui;

import com.courtney.aiassistant.controller.ConfigController;
import com.courtney.aiassistant.controller.ConversationController;
import com.courtney.aiassistant.exception.ApiException;
import com.courtney.aiassistant.model.AppSettings;
import com.courtney.aiassistant.model.Conversation;
import com.courtney.aiassistant.service.ApiService;
import com.courtney.aiassistant.service.ConfigService;
import com.courtney.aiassistant.service.ConversationRepository;
import com.courtney.aiassistant.template.HtmlTemplate;
import com.courtney.aiassistant.util.ErrorHandler;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ClientAppController {

    private Stage stage;
    private ClientApp view;

    private ConfigService configService;
    private ConversationRepository conversationRepository;
    private ApiService apiService;
    private AppSettings settings;

    private ConversationController conversationController;
    private ConfigController configController;

    public void start(Stage stage) {
        this.stage = stage;

        // Services
        configService = new ConfigService();
        conversationRepository = new ConversationRepository();
        apiService = new ApiService();

        // Controllers
        configController = new ConfigController(configService);
        conversationController = new ConversationController(apiService, conversationRepository, configService);

        // UI
        view = new ClientApp(this); // Pass the controller instance to the view
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/chatgpt.png")));
        stage.setScene(view.createScene());

        initWebView();
        bindActions();
        applyStatus();

        settings = configService.getSettings();
        String modeSetting = settings.getMode();

        switch (modeSetting) {
            case "Assistant":
                stage.setTitle("AI Assistant");
                break;
            case "Deutsch":
                stage.setTitle("Deutsches Wörterbuch");
                break;
            case "Healthcare":
                stage.setTitle("Healthcare Assistant");
                break;
            case "Programmer":
                stage.setTitle("Programming Assistant");
                break;
            default:
                stage.setTitle("AI Assistant");
                break;
        }
        view.focusInputArea();
        stage.show();
    }
    private void initWebView() {
        String baseHtml = HtmlTemplate.baseHtml(configService.getSettings());
        view.getEngine().loadContent(baseHtml, "text/html");
        view.focusInputArea();
    }

    private void bindActions() {
        view.getMiNewConversation().setOnAction(e -> newConversation());
        view.getMiOpenConversation().setOnAction(e -> openConversationBrowser());
        view.getMiSaveConversation().setOnAction(e -> saveConversationAsJson());
        view.getMiSaveHtml().setOnAction(e -> saveViewAsHtml());
        view.getMiExit().setOnAction(e -> stage.close());
        view.getMiConfig().setOnAction(e -> openConfigDialog());
        view.getMiBrowseConversations().setOnAction(e -> openConversationBrowser());
        view.getMiAssistant().setOnAction(e -> setAssistant());
        view.getMiDictionary().setOnAction(e -> setDictionary());
        view.getMiHealthcare().setOnAction(e -> setMedical());
        view.getMiProgrammer().setOnAction(e -> setProgrammer());

        // Ctrl+Enter to send
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                this::sendMessage
        );

        // Update right status when settings change
        configService.settingsProperty().addListener((obs, oldV, newV) -> applyStatus());
    }

    private void setProgrammer() {
        AppSettings s = new AppSettings();
        s.setMode("Programmer");
        s.setModel("gpt-4o");
        s.setTemperature(0.1);
        s.setMaxTokens(4096);
        s.setSystemPrompt("You are an expert Java and JavaFX engineer. Provide working code with comments, error handling and good programming practice.");
        configService.setSettings(s);
        configService.save();
        configService.load();
        stage.setTitle("Programming Assistant");
    }

    private void setMedical() {
        AppSettings s = new AppSettings();
        s.setMode("Healthcare");
        s.setModel("gpt-4o");
        s.setTemperature(0.1);
        s.setMaxTokens(1024);
        s.setSystemPrompt("You are an expert healthcare professional. Explain the key features and symptoms, possible treatments, therapies, medications and contraindications and other relevant information for the health matter described by the user.");
        configService.setSettings(s);
        configService.save();
        configService.load();
        stage.setTitle("Healthcare Assistant");
    }

    private void setDictionary() {
        AppSettings s = new AppSettings();
        s.setMode("Deutsch");
        s.setModel("gpt-4o-mini");
        s.setTemperature(0.4);
        s.setMaxTokens(1024);
        s.setSystemPrompt("Sie sind ein deutschsprachiger Assistent. Übersetzen Sie englische Wörter, Ausdrücke und Texte ins Deutsche und erklären Sie deren Verwendung. Erklären Sie deutsche Texte auf Deutsch.");
        configService.setSettings(s);
        configService.save();
        configService.load();
        stage.setTitle("Deutsches Wörterbuch");
    }

    private void setAssistant() {
        AppSettings s = new AppSettings();
        s.setMode("Assistant");
        s.setModel("gpt-4o-mini");
        s.setTemperature(0.7);
        s.setMaxTokens(1024);
        s.setSystemPrompt("You are a helpful assistant.");
        configService.setSettings(s);
        configService.save();
        configService.load();
        stage.setTitle("AI Assistant");
    }

    private void applyStatus() {
        AppSettings s = configService.getSettings();
        view.getStatusLeft().setText("Ready");
        view.getStatusRight().setText("Mode: " + s.getMode() + "   Model: " + s.getModel() + "   Temp: " + String.format("%.2f", s.getTemperature()));
    }

    private void newConversation() {
        conversationController.newConversation();
        initWebView();
    }

    public void sendMessage() {
        final String prompt = view.getInputArea().getText().trim();

        if (prompt.isEmpty()) return;

        view.getInputArea().clear();

        // Add user message to HTML
        Platform.runLater(() ->
                view.getEngine().executeScript("addUserMessage(" + toJsArg(prompt) + ");"));
        Platform.runLater(() -> view.getStatusLeft().setText("Thinking..."));
        // Start streaming
        try {
            conversationController.streamCompletion(
                    prompt,
                    onStart -> {
                        Platform.runLater(() ->
                                view.getEngine().executeScript("beginAssistantMessage();"));
                        Platform.runLater(() -> view.getStatusLeft().setText("Responding..."));
                    },
                    delta -> {
                        // Append streamed tokens without flicker: DOM append of text nodes
                        Platform.runLater(() ->
                                view.getEngine().executeScript("appendAssistant(" + toJsArg(delta) + ");"));
                    },
                    finishText -> {
                        Platform.runLater(() -> {
                            view.getEngine().executeScript("endAssistantMessage();");
                            view.getStatusLeft().setText("Ready");
                        });
                    },
                    err -> {
                        Platform.runLater(() -> {
                            view.getEngine().executeScript("endAssistantMessage();");
                            view.getStatusLeft().setText("Ready");
                            ErrorHandler.alert("API Error", err.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
            );
        } catch (ApiException ex) {
            ErrorHandler.alert("Configuration Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void saveViewAsHtml() {
        try {
            Object htmlObj = view.getEngine().executeScript("document.documentElement.outerHTML");
            String html = String.valueOf(htmlObj);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save HTML");
            String userHome = System.getProperty("user.home");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));
            chooser.setInitialDirectory(new File(userHome));
            chooser.setInitialFileName("conversation.html");
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                conversationController.saveHtml(html.getBytes(StandardCharsets.UTF_8), file.toPath());
                ErrorHandler.toast("Saved", "HTML saved successfully.");
            }
        } catch (Exception ex) {
            ErrorHandler.alert("Save Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void saveConversationAsJson() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Conversation (JSON)");
            // Get the user's home directory
            String userHome = System.getProperty("user.home");

            // Define the subdirectory within the user's home directory
            String subdirectory = ".aiassistant/conversations";

            // Create a File object for the initial directory
            File initialDirectory = new File(userHome, subdirectory);

            // Check if the directory exists, if not, handle it gracefully
            if (initialDirectory.exists() && initialDirectory.isDirectory()) {
                chooser.setInitialDirectory(initialDirectory);
            } else {
                // Fallback to user's home directory if the subdirectory doesn't exist
                chooser.setInitialDirectory(new File(userHome));
            }

            //chooser.setInitialDirectory(new File("/home/dean/.aiassistant/conversations"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName(conversationController.currentConversationSuggestedFileName());
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                conversationController.saveConversation(file.toPath());
                ErrorHandler.toast("Conversation Saved", "Conversation saved successfully.");
            }
        } catch (Exception ex) {
            ErrorHandler.alert("Save Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openConfigDialog() {
        AppConfig dialog = new AppConfig(stage, configService.getSettings());
        Optional<AppSettings> updated = dialog.showAndWait();
        updated.ifPresent(settings -> {
            configController.save(settings);
            applyStatus();
            // Re-render base HTML to apply possible style changes dependent on settings or reset streaming state
            initWebView();
            // Re-display the current conversation after HTML reload
            conversationController.renderCurrentConversationTo(view.getEngine());
        });
    }

    private void openConversationBrowser() {
        ConversationManager cm = new ConversationManager(stage, conversationController, conversationRepository);
        Optional<Conversation> chosen = cm.showAndWait();
        chosen.ifPresent(conv -> {
            conversationController.setCurrentConversation(conv);
            initWebView();
            conversationController.renderCurrentConversationTo(view.getEngine());
        });
    }

    private static String toJsArg(String s) {
        if (s == null) return "''";
        String esc = s
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "'" + esc + "'";
    }
}