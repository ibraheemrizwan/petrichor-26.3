package com.pockyl.petrichor.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Optional channel: vanilla clients and servers remain supported. Client handler installs only on a client. */
public final class ModNetwork {
    private ModNetwork() {}
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(WeatherSyncPayload.TYPE, WeatherSyncPayload.STREAM_CODEC);
    }
    public static void registerClient(java.util.function.Consumer<WeatherSyncPayload> receiver) {
        ClientPlayNetworking.registerGlobalReceiver(WeatherSyncPayload.TYPE, (payload, context) -> receiver.accept(payload));
    }
    public static void sendToPlayer(ServerPlayer player, WeatherSyncPayload payload) {
        if (ServerPlayNetworking.canSend(player, WeatherSyncPayload.TYPE)) ServerPlayNetworking.send(player, payload);
    }
    public static void sendToPlayersInDimension(ServerLevel level, WeatherSyncPayload payload) {
        for (ServerPlayer player : level.players()) sendToPlayer(player, payload);
    }
}
