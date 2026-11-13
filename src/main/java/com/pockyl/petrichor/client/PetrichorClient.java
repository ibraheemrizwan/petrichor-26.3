package com.pockyl.petrichor.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import com.pockyl.petrichor.ClientConfig;
import com.pockyl.petrichor.Petrichor;
import com.pockyl.petrichor.network.ModNetwork;
import com.pockyl.petrichor.client.render.*;
public final class PetrichorClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientConfig.SPEC.load(FabricLoader.getInstance().getConfigDir().resolve("petrichor-client.json"));
        PetrichorShaders.register();
        ModNetwork.registerClient(p -> ClientWeather.onServerSync(p.rainType(),p.rainLevel(),p.sunShower(),p.wetness()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> WeatherClient.onClientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> WeatherClient.onLoggingOut());
        ClientEntityEvents.ENTITY_LOAD.register(WeatherClient::onEntityJoin);
        EntityRendererRegistry.register(net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT,PetrichorBoltRenderer::new);
        LevelExtractionEvents.END_EXTRACTION.register(WeatherClient::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(WeatherClient::collect);
        LevelRenderEvents.END_MAIN.register(WeatherClient::endMain);
        net.minecraft.client.gui.components.debug.DebugScreenEntries.register(Petrichor.id("weather"),(displayer,level,clientChunk,serverChunk) -> {
            var lines = new java.util.ArrayList<String>();
            WeatherClient.onDebugText(lines);
            displayer.addToGroup(Petrichor.id("weather"),lines);
        });
    }
}
