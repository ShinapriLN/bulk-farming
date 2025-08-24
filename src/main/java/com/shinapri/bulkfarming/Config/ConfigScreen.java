package com.shinapri.bulkfarming.Config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreen {
    private ConfigScreen() {}

    public static Screen create(Screen parent) {
        BulkFarmingConfig cfg = BulkFarmingConfigIO.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Bulk Farming"))
                .setSavingRunnable(BulkFarmingConfigIO::save);

        ConfigCategory cat = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        cat.addEntry(
                eb.startBooleanToggle(Component.literal("Collect to inventory"), cfg.collectInInventory)
                        .setDefaultValue(BulkFarmingConfig.DEFAULT_COLLECT_IN_INVENTORY)
                        .setSaveConsumer(v -> cfg.collectInInventory = v)
                        .build()
        );

        return builder.build();
    }
}
