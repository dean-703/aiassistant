package com.courtney.aiassistant.controller;

import com.courtney.aiassistant.model.AppSettings;
import com.courtney.aiassistant.service.ConfigService;

public class ConfigController {

    private final ConfigService service;

    public ConfigController(ConfigService service) {
        this.service = service;
    }

    public void save(AppSettings settings) {
        service.setSettings(settings);
        service.save();
    }
}
