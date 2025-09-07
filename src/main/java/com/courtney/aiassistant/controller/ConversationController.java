package com.courtney.aiassistant.controller;

import com.courtney.aiassistant.exception.ApiException;
import com.courtney.aiassistant.model.AppSettings;
import com.courtney.aiassistant.model.Conversation;
import com.courtney.aiassistant.model.Message;
import com.courtney.aiassistant.service.ApiService;
import com.courtney.aiassistant.service.ConfigService;
import com.courtney.aiassistant.service.ConversationRepository;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public class ConversationController {

    private final ApiService apiService;
    private final ConversationRepository repository;
    private final ConfigService configService;

    private Conversation current = new Conversation();

    public ConversationController(ApiService apiService,
                                  ConversationRepository repository,
                                  ConfigService configService) {
        this.apiService = apiService;
        this.repository = repository;
        this.configService = configService;
        newConversation();
    }

    public void newConversation() {
        current = new Conversation();
        current.setId(UUID.randomUUID().toString());
        current.setCreatedAt(LocalDateTime.now());
        current.setUpdatedAt(LocalDateTime.now());
        current.setTitle("New Conversation");
    }

    public void setCurrentConversation(Conversation conv) {
        this.current = conv;
    }

    public String currentConversationSuggestedFileName() {
        String base = (current.getTitle() == null || current.getTitle().isBlank())
                ? "conversation" : current.getTitle().trim().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        return base + ".json";
    }

    public void streamCompletion(String userText,
                                 Consumer<Void> onStart,
                                 Consumer<String> onDelta,
                                 Consumer<String> onComplete,
                                 Consumer<Throwable> onError) throws ApiException {

        AppSettings s = configService.getSettings();

        Message user = new Message("user", userText);
        current.getMessages().add(user);
        current.setUpdatedAt(LocalDateTime.now());

        StringBuilder assistantText = new StringBuilder();

        apiService.streamChatCompletion(current, s, () -> onStart.accept(null),
                delta -> {
                    assistantText.append(delta);
                    onDelta.accept(delta);
                },
                finishReason -> {
                    Message assistant = new Message("assistant", assistantText.toString());
                    current.getMessages().add(assistant);
                    if (current.getTitle() == null || current.getTitle().equals("New Conversation")) {
                        current.setTitle(generateTitleFromMessages());
                    }
                    current.setUpdatedAt(LocalDateTime.now());
                    onComplete.accept(assistantText.toString());
                },

                onError);
    }

    public void renderCurrentConversationTo(WebEngine engine) {
        Platform.runLater(() -> {
            engine.executeScript("clearMessages()");
            for (Message m : current.getMessages()) {
                String jsArg = toJsArg(m.getContent());
                if ("user".equals(m.getRole())) {
                    engine.executeScript("addUserMessage(" + jsArg + ")");
                } else {
                    engine.executeScript("addAssistantMessage(" + jsArg + ")");
                }
            }
        });
    }

    public void saveHtml(byte[] htmlBytes, Path path) throws RuntimeException {
        repository.saveHtml(htmlBytes, path);
    }

    public void saveConversation(Path path) {
        String fullFileName = path.getFileName().toString();
        String fileNameWithoutExtension;
        int dotIndex = fullFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileNameWithoutExtension = fullFileName.substring(0, dotIndex);
        } else {
            fileNameWithoutExtension = fullFileName; // No extension found
        }
        current.setTitle(fileNameWithoutExtension);
        repository.save(current, path);
    }

    private String generateTitleFromMessages() {
        StringBuilder sb = new StringBuilder();
        for (Message m : current.getMessages()) {
            if ("user".equals(m.getRole())) {
                sb.append(m.getContent());
                break;
            }
        }
        String t = sb.toString().trim();
        if (t.length() > 40) t = t.substring(0, 40) + "...";
        if (t.isBlank()) t = "Conversation";
        return t;
    }

    private static String toJsArg(String s) {
        if (s == null) return "''";
        String esc = s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "'" + esc + "'";
    }
}