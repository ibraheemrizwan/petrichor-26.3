package com.pockyl.petrichor.gametest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import com.pockyl.petrichor.Petrichor;
import com.pockyl.petrichor.client.*;
import com.pockyl.petrichor.weather.*;
public final class ClientPortTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        try (var world=context.worldBuilder().create()) {
            save=world.getWorldSave();
            world.getServer().runCommand("fillbiome -32 -64 -32 32 -58 32 minecraft:plains");
            world.getServer().runCommand("tp @a 0 -59 0 0 10");
            world.getServer().runCommand("fill -16 -61 -16 16 -61 16 minecraft:stone_bricks");
            world.getServer().runCommand("fill -5 -60 7 -3 -57 9 minecraft:oak_leaves[persistent=true]");
            world.getServer().runCommand("petrichor weather thunderstorm 600");
            world.getServer().runCommand("petrichor wetness 1");
            world.getServer().runOnServer(server -> {
                var data=StormData.get(server.overworld());
                for(int i=0;i<6000;i++) data.tick(server.overworld());
                com.pockyl.petrichor.event.WeatherEvents.syncLevel(server.overworld());
            });
            context.waitTicks(5);
        }
        try (var world=save.open()) {
            world.getServer().runCommand("tp @a 0 -59 0 0 10");
            world.getConnection().waitForChunksRender();
            context.waitFor(client -> ClientWeather.syncedWithServer() && ClientWeather.rain()>0.2F && ClientWeather.level()>2.5F,1200);
            context.waitTicks(100);
            List<String> debug=context.computeOnClient(client -> {var lines=new ArrayList<String>(); WeatherClient.onDebugText(lines);return lines;});
            var matcher=Pattern.compile("Drops (\\d+)").matcher(String.join("\n",debug));
            if (!matcher.find() || Integer.parseInt(matcher.group(1))==0) throw new AssertionError("No rendered rain: "+debug);
            Petrichor.LOGGER.info("PETRICHOR_CLIENT: {}",debug);
            context.takeScreenshot("petrichor-thunderstorm");
            world.getServer().runCommand("petrichor strike");
            context.waitTicks(10);
            context.takeScreenshot("petrichor-lightning");
            var reload=context.computeOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> reload.isDone(),1200);reload.join();
            context.waitTicks(30);
            context.takeScreenshot("petrichor-after-resource-reload");
            world.getServer().runCommand("petrichor weather sun_shower 600");
            world.getServer().runCommand("time set 1000");
            context.waitFor(client -> com.pockyl.petrichor.client.render.Rainbow.strength()>0.2F,1200);
            context.takeScreenshot("petrichor-sun-shower");
            world.getServer().runCommand("petrichor weather thunderstorm 600");
        }
        try (var world=save.open()) {
            boolean persisted=world.getServer().computeOnServer(server -> StormData.get(server.overworld()).save(new net.minecraft.nbt.CompoundTag(),server.registryAccess()).getIntOr("override",-1)==RainType.THUNDERSTORM.ordinal());
            if (!persisted) throw new AssertionError("Forced rain did not survive world reload");
            context.waitFor(client -> ClientWeather.syncedWithServer(),1200);
            context.waitTicks(20);
            context.takeScreenshot("petrichor-after-world-reload");
        }
        Petrichor.LOGGER.info("PETRICHOR_CLIENT: weather drawing, lightning, resource reload, saved-world reload passed");
    }
}


