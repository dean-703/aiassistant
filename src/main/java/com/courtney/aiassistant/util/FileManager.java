package com.courtney.aiassistant.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {
    public static Path appDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".aiassistant");
    }

    public static String safeFileName(String s) {
        if (s == null || s.isBlank()) return "conversation";
        return s.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}