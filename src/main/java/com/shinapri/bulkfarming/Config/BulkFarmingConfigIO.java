package com.shinapri.bulkfarming.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BulkFarmingConfigIO {
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "bulk-farming.json");
    private static BulkFarmingConfig INSTANCE = load();

    private BulkFarmingConfigIO() {}

    public static BulkFarmingConfig get() { return INSTANCE; }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, G.toJson(INSTANCE));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static BulkFarmingConfig load() {
        try {
            if (Files.exists(FILE)) {
                return G.fromJson(Files.readString(FILE), BulkFarmingConfig.class);
            }
        } catch (IOException ignored) {}
        return new BulkFarmingConfig();
    }
}


