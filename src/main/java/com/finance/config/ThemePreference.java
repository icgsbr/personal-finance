package com.finance.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ThemePreference {

    private static final Path FILE = Paths.get("data", "preferences.properties");

    public static void save(boolean darkMode) {
        try {
            Files.createDirectories(FILE.getParent());
            Properties props = new Properties();
            props.setProperty("theme", darkMode ? "dark" : "light");
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, null);
            }
        } catch (IOException ignored) {}
    }

    public static boolean loadDarkMode() {
        if (!Files.exists(FILE)) return true;
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties props = new Properties();
            props.load(in);
            return !"light".equals(props.getProperty("theme", "dark"));
        } catch (IOException e) {
            return true;
        }
    }
}
