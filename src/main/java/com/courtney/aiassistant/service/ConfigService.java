package com.courtney.aiassistant.service;

import com.courtney.aiassistant.exception.ConfigurationException;
import com.courtney.aiassistant.model.AppSettings;
import com.courtney.aiassistant.util.FileManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigService {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private final Path configDir = FileManager.appDir();
    private final Path configFile = configDir.resolve("appsettings.json");

    private final ObjectProperty<AppSettings> settings = new SimpleObjectProperty<>(new AppSettings());

    public ConfigService() {
        load();
    }

    public void load() {
        try {
            if (!Files.exists(configFile)) {
                save(); // write defaults
            } else {
                AppSettings s = MAPPER.readValue(configFile.toFile(), AppSettings.class);
                if (s != null) settings.set(s);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configDir);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), settings.get());
        } catch (IOException e) {
            throw new ConfigurationException("Failed to save configuration: " + e.getMessage(), e);
        }
    }

    public AppSettings getSettings() {
        return settings.get();
    }

    public void setSettings(AppSettings s) {
        settings.set(s);
    }

    public ObjectProperty<AppSettings> settingsProperty() {
        return settings;
    }
}