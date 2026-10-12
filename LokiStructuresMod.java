package com.example.lokistructures;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Rarity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

/** Entrypoint; no client-only classes are loaded on dedicated servers. */
public final class LokiStructuresMod implements ModInitializer {
    public static final String MOD_ID = "loki_structures";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static final Item LOKI_SCEPTER = Registry.register(
            Registries.ITEM, id("loki_scepter"),
            new LokiScepterItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Feature<DefaultFeatureConfig> VILLAGER_TREASURY = Registry.register(
            Registries.FEATURE, id("villager_treasury"),
            new TreasureBuildingFeature(DefaultFeatureConfig.CODEC, false));

    public static final Feature<DefaultFeatureConfig> PILLAGER_STRONGHOLD = Registry.register(
            Registries.FEATURE, id("pillager_stronghold"),
            new TreasureBuildingFeature(DefaultFeatureConfig.CODEC, true));

    private static RegistryKey<PlacedFeature> placed(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, id(name));
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(LOKI_SCEPTER));
        // Both generate as natural overworld features in newly generated chunks.
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.SURFACE_STRUCTURES, placed("villager_treasury"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.SURFACE_STRUCTURES, placed("pillager_stronghold"));
    }
}
