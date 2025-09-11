package com.courtney.aiassistant.controller;

import com.courtney.aiassistant.model.AppSettings;
import com.courtney.aiassistant.service.ConfigService;
import javafx.stage.Stage;

public class Presets {

    ConfigService configService = new ConfigService();
    Stage stage = new Stage();

    public Presets(){}

    public Presets(ConfigService c, Stage s)
    {   configService = c; stage = s; }

    public void setProgrammerMode() {
        AppSettings s = new AppSettings();
        s.setMode("Programmer");
        s.setModel("gpt-4o");
        s.setTemperature(0.1);
        s.setMaxTokens(4096);
        s.setSystemPrompt("You are an expert Java and JavaFX engineer. Assist the user with the issue presented. Provide working code when requested with comments, error handling and good programming practice.");
        configService.setSettings(s);
        configService.save();
        configService.load();
        stage.setTitle("Programming Assistant");
    }

    public void setMedicalMode() {
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

    public void setDictionaryMode() {
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

    public void setAssistantMode() {
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

}
