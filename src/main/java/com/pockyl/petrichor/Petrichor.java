package com.pockyl.petrichor;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import com.pockyl.petrichor.network.ModNetwork;
import com.pockyl.petrichor.event.WeatherEvents;
import com.pockyl.petrichor.event.StormCommands;

public final class Petrichor implements ModInitializer {
    public static final String MOD_ID = "petrichor";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Config.SPEC.load(
                FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "-server.json")));
        net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TAGS_LOADED.register((registries,client) -> {
            com.pockyl.petrichor.world.SoundMaterial.clearCache();
            com.pockyl.petrichor.world.SurfaceKind.clearCache();
        });
        ModNetwork.register();
        WeatherEvents.register();
        StormCommands.register();
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
}
