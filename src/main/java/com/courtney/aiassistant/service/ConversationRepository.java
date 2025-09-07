package com.courtney.aiassistant.service;

import com.courtney.aiassistant.model.Conversation;
import com.courtney.aiassistant.util.FileManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConversationRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private final Path conversationsDir = FileManager.appDir().resolve("conversations");

    public ConversationRepository() {
        try {
            Files.createDirectories(conversationsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create conversations directory", e);
        }
    }

    public void save(Conversation conv, Path target) {
        try {
            if (target == null) {
                String name = FileManager.safeFileName(conv.getTitle()) + "-" + conv.getCreatedAt().toLocalDate() + ".json";
                target = conversationsDir.resolve(name);
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), conv);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save conversation: " + e.getMessage(), e);
        }
    }

    public List<Conversation> listAll() {
        List<Conversation> res = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(conversationsDir, "*.json")) {
            for (Path p : stream) {
                try {
                    Conversation c = MAPPER.readValue(p.toFile(), Conversation.class);
                    res.add(c);
                } catch (IOException ignore) {}
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list conversations: " + e.getMessage(), e);
        }
        res.sort(Comparator.comparing(Conversation::getCreatedAt).reversed());
        return res;
    }

    public void delete(Conversation conv) {
        // Attempt to find conversation by title/date pattern
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(conversationsDir, "*.json")) {
            for (Path p : stream) {
                try {
                    Conversation c = MAPPER.readValue(p.toFile(), Conversation.class);
                    if (c.getId() != null && c.getId().equals(conv.getId())) {
                        Files.deleteIfExists(p);
                        return;
                    }
                } catch (IOException ignore) {}
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete conversation: " + e.getMessage(), e);
        }
    }

    public void saveHtml(byte[] htmlBytes, Path path) {
        try {
            Files.write(path, htmlBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save HTML: " + e.getMessage(), e);
        }
    }
}
