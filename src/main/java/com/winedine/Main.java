package com.winedine;

import com.winedine.config.AppConfig;

public class Main {
    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        System.out.println("Wine&Dine AI is starting...");

        try {
            config.require("OPENAI_API_KEY");   // never print the key itself
            String model = config.require("OPENAI_MODEL");
            System.out.println("Settings loaded. Model: " + model);
        } catch (IllegalStateException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}