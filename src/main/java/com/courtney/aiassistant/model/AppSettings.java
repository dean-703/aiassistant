package com.courtney.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import javafx.beans.property.SimpleObjectProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppSettings {
    private String mode = "Assistant";
    private String model = "gpt-4o-mini";
    private double temperature = 0.7;
    private int maxTokens = 1024;
    private String systemPrompt = "You are a helpful assistant.";

    // Provide a shallow copy helper for the dialog
    public AppSettings copy() {
        AppSettings c = new AppSettings();
        c.mode = this.mode;
        c.model = this.model;
        c.temperature = this.temperature;
        c.maxTokens = this.maxTokens;
        c.systemPrompt = this.systemPrompt;
        return c;
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    // For two-way binding in UI (optional convenience)
    public static class Holder extends SimpleObjectProperty<AppSettings> {
        public Holder(AppSettings value) { super(value); }
    }
}
