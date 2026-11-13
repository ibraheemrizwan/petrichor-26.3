package com.pockyl.petrichor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.pockyl.petrichor.client.render.WeatherBuffers;
import com.pockyl.petrichor.client.render.WeatherShader;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.Entity;

import com.pockyl.petrichor.ClientConfig;
import com.pockyl.petrichor.Petrichor;
import com.pockyl.petrichor.client.compat.ShaderPacks;
import com.pockyl.petrichor.client.fx.FxAtlas;
import com.pockyl.petrichor.client.fx.FxSpawner;
import com.pockyl.petrichor.client.fx.Precipitation;
import com.pockyl.petrichor.client.fx.RainFx;
import com.pockyl.petrichor.client.fx.RainVeils;
import com.pockyl.petrichor.client.lightning.Lightning;
import com.pockyl.petrichor.client.render.Atmosphere;
import com.pockyl.petrichor.client.render.Cinematics;
import com.pockyl.petrichor.client.render.PetrichorEffects;
import com.pockyl.petrichor.client.render.PetrichorShaders;
import com.pockyl.petrichor.client.render.Puddles;
import com.pockyl.petrichor.client.render.Rainbow;
import com.pockyl.petrichor.client.render.SceneCopy;
import com.pockyl.petrichor.client.sound.RainSounds;
import com.pockyl.petrichor.compat.ParticleRain;
import com.pockyl.petrichor.weather.MistModel;

/**
 * The client's weather systems and the game events that drive them.
 */
public final class WeatherClient {
    private static final Columns COLUMNS = new Columns();
    private static final RainFx FX = new RainFx();
    private static final Precipitation PRECIPITATION = new Precipitation();
    private static final FxSpawner SPAWNER = new FxSpawner();
    private static final Lightning LIGHTNING = new Lightning();
    private static Puddles puddles;
    private static ByteBufferBuilder rainBytes;
    private static int ticks;
    /** How much of the sky the camera sees, 0 (deep indoors, underground) .. 1 (outdoors), eased. */
    private static float skyView = 1.0F;
    private static float previousSkyView = 1.0F;
    /** Whether the weather was drawn in the vanilla weather pass this frame. */
    private static boolean weatherDrawn;

    private WeatherClient() {
    }

    public static boolean ourSky(ClientLevel level) {
        return level != null && level.canHaveWeather();
    }

    private static ByteBufferBuilder rainBuffer() {
        if (rainBytes == null) {
            rainBytes = new ByteBufferBuilder(1 << 20);
        }
        return rainBytes;
    }

    private static Puddles puddles() {
        if (puddles == null) {
            puddles = new Puddles();
        }
        return puddles;
    }

    /** Whether the mod draws the rain (and vanilla only the snow). */
    public static boolean ownsRain() {
        return ClientConfig.RAIN.get() && PetrichorShaders.rain() != null && ourSky(Minecraft.getInstance().level);
    }

    public static float flash(float partialTick) {
        return LIGHTNING.flash(partialTick);
    }

    /** How much the world darkens in the moment before a strike. */
    public static float hush(float partialTick) {
        return LIGHTNING.hush(partialTick);
    }

    /** Light of close strikes, which overexposes the view. */
    public static float glare(float partialTick) {
        return LIGHTNING.glare(partialTick);
    }

    /**
     * Vanilla leaves in the rain drip water particles; with drips enabled the mod's own drop falls from there instead.
     *
     * @return whether the drop was handled (false: let vanilla spawn its particle)
     */
    public static boolean leafDrip(Level level, BlockPos pos, RandomSource random) {
        if (!(level instanceof ClientLevel clientLevel) || !ourSky(clientLevel) || !ClientConfig.DRIPS.get()) {
            return false;
        }
        SPAWNER.dropFrom(clientLevel, puddles(), FX, pos.getX() + random.nextDouble(), pos.getY(), pos.getZ() + random.nextDouble());
        return true;
    }

    /**
     * How much of the sky the camera sees, from the sky light where it stands: 0 deep indoors or underground, 1
     * outdoors. What lights or hazes the whole view (lightning flashes, the rain's haze and fog) fades with it, so a
     * storm overhead does not flicker in a cave.
     */
    public static float skyView(float partialTick) {
        return Mth.lerp(partialTick, previousSkyView, skyView);
    }

    private static void tickSkyView(ClientLevel level, Camera camera) {
        previousSkyView = skyView;
        int light = level.getBrightness(LightLayer.SKY, camera.blockPosition());
        // Sky light drops by one per block into a cave: the sky's light fades in over the last 14 blocks to the exit.
        float target = Mth.clamp((light - 1) / 14.0F, 0.0F, 1.0F);
        target *= target;
        // Leaves dim the sky light too, but under the trees one is still out in the rain.
        if (Mist.outdoors()) {
            target = 1.0F;
        }
        skyView += (target - skyView) * 0.15F;
    }

    /** How overcast the light is: follows the rain, heavier rain is gloomier; a sun shower is not overcast. */
    public static float gloom() {
        return ClientWeather.rain() * Math.min(1.0F, 0.45F + ClientWeather.density * 0.25F) * (float) Math.min(1.0, ClientConfig.FOG.get())
                * ClientWeather.shade();
    }

    /**
     * Vanilla's rain level where it darkens the sky, clouds, fog and daylight and hides the sun: the sun shines
     * through a sun shower.
     */
    public static float rainShade(float rain) {
        return ourSky(Minecraft.getInstance().level) ? rain * ClientWeather.shade() : rain;
    }

    /** Debug description of the puddle data at a column. */
    public static String describePuddle(int x, int z) {
        return puddles == null ? "no puddles" : puddles.describe(x, z);
    }

    /** Throws away the puddle meshes so they are built again from the current terrain. */
    public static void rebuildSurfaces() {
        if (puddles != null) {
            puddles.clear();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------------------------

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (!ourSky(level)) {
            RainSounds.stopAll();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        ticks++;
        ClientWeather.tick(level);
        ParticleRain.refresh();
        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        Mist.tick(level, cam, ticks);
        tickSkyView(level, camera);
        COLUMNS.begin(level, ticks, cam.x, cam.z);
        FX.setCapacity(ClientConfig.quality().maxEffects);
        FX.recenter(cam.x, cam.z);
        FX.tick(ClientWeather.windX(), ClientWeather.windZ(), cam.x, cam.y, cam.z);
        Puddles puddles = puddles();
        puddles.tick(level, cam.x, cam.z);
        boolean rain = ClientConfig.RAIN.get();
        if (rain) {
            SPAWNER.tick(level, COLUMNS, puddles, FX, cam);
            RainSounds.tick(level, COLUMNS, puddles, cam);
        } else {
            RainSounds.stopAll();
        }
        LIGHTNING.tick(level, cam, FX, ClientWeather.thunder(), skyView);
        Rainbow.tick(level);
        Cinematics.tick();
        if (ClientConfig.BOLTS.get()) {
            // Vanilla bolts set a full-white sky flash; the graded flash of the lightning system replaces it.
            level.setSkyFlashTime(0);
        }
    }

    public static void onEntityJoin(Entity entity, ClientLevel level) {
        if (entity instanceof LightningBolt bolt && ourSky(level) && ClientConfig.BOLTS.get())
            LIGHTNING.onBolt(level, bolt, Minecraft.getInstance().gameRenderer.mainCamera().position());
    }

    public static SoundInstance filterSound(SoundInstance sound) {
        if (sound instanceof Lightning.ThunderSound || !ClientConfig.DELAYED_THUNDER.get()
                || !ourSky(Minecraft.getInstance().level)) return sound;
        String name = sound.getIdentifier().getPath();
        return name.equals("entity.lightning_bolt.thunder") || name.equals("entity.lightning_bolt.impact") ? null : sound;
    }

    public static void onLoggingOut() {
        ClientWeather.reset();
        Mist.reset();
        FX.clear();
        LIGHTNING.clear();
        Cinematics.clear();
        Rainbow.clear();
        RainSounds.stopAll();
        if (puddles != null) {
            puddles.clear();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------------------------------

    /** Draws the rain and its effects in place of the vanilla rain. */
    public static boolean renderWeather(ClientLevel level, float partialTick, double camX, double camY,
            double camZ) {
        weatherDrawn = true;
        WeatherShader shader = PetrichorShaders.rain();
        if (shader == null) return false;
        // The air first: haze over the distance, the overcast sky. The rain falls in front of it.
        Atmosphere.render(level, partialTick, camX, camY, camZ);
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        Vector3f left = new Vector3f(camera.leftVector());
        Vector3f up = new Vector3f(camera.upVector());
        COLUMNS.begin(level, ticks, camX, camZ);
        WeatherShader.use(shader);
        WeatherShader.texture(0, FxAtlas.TEXTURE);
        float[] fog = WeatherShader.fogColor();
        double time = level.getGameTime() + (double) partialTick;
        // Two passes: drops add light (rain glints, it never darkens), effects blend normally.
        ByteBufferBuilder rainBytes = rainBuffer();
        BufferBuilder drops = new BufferBuilder(rainBytes, PrimitiveTopology.QUADS, DefaultVertexFormat.PARTICLE);
        BufferBuilder builder = WeatherBuffers.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.PARTICLE);
        PRECIPITATION.render(drops, COLUMNS, camX, camY, camZ, time, fog, LIGHTNING.flash(partialTick));
        FX.render(builder, drops, camX, camY, camZ, partialTick, left, up);
        MeshData mesh = builder.build();
        if (mesh != null) {
            WeatherShader.drawCurrent(mesh);
        }
        MeshData dropMesh = drops.build();
        if (dropMesh != null) {
            shader.additiveBlend();
            WeatherShader.drawCurrent(dropMesh);

        }

        WeatherShader veil = PetrichorShaders.veil();
        if (veil != null) {
            WeatherShader.use(veil);
            WeatherShader.texture(0, RainVeils.TEXTURE);
            builder = WeatherBuffers.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.PARTICLE);
            RainVeils.render(builder, COLUMNS, camX, camY, camZ, time, fog, WeatherShader.fogEnd(),
                    Atmosphere.active() ? Atmosphere.haze() : 0.0F);
            mesh = builder.build();
            if (mesh != null) {
                WeatherShader.drawCurrent(mesh);
            }
        }

        return true;
    }

    public static void extract(LevelExtractionContext context) {
        if (!ourSky(Minecraft.getInstance().level)) return;
        var state = context.levelState();
        if (ownsRain()) state.weatherRenderState.rainColumns.clear();
        applyFog(state.cameraRenderState.fogData, state.cameraRenderState.fogType, state.worldPartialTicks);
        // Sun showers leave the celestial discs visible while precipitation keeps its actual intensity.
        state.skyRenderState.rainBrightness = 1.0F - rainShade(1.0F - state.skyRenderState.rainBrightness);
    }

    public static void afterSky() {
        if (!ourSky(Minecraft.getInstance().level)) return;
        var state = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState;
        var camera = state.cameraRenderState;
        WeatherShader.frame(camera.projectionMatrix, camera.fogData);
        SceneCopy.newFrame();
        com.pockyl.petrichor.client.compat.PackWeather.onFrameStart(state.worldPartialTicks);
        Rainbow.render(Minecraft.getInstance().level, camera.viewRotationMatrix, state.worldPartialTicks);
        LIGHTNING.renderSky(camera.viewRotationMatrix, Minecraft.getInstance().gameRenderer.mainCamera(), state.worldPartialTicks);
    }

    public static void afterSolid(LevelRenderContext context) {
        if (!ourSky(Minecraft.getInstance().level) || puddles == null) return;
        var camera = context.levelState().cameraRenderState;
        puddles.render(camera.viewRotationMatrix, camera.projectionMatrix, camera.pos, camera.cullFrustum, context.levelState().worldPartialTicks);
    }

    public static void collect(LevelRenderContext context) {
        if (!ourSky(Minecraft.getInstance().level) || puddles == null) return;
        var camera = context.levelState().cameraRenderState;
        puddles.collectWater(context.submitNodeCollector(), camera.pos, camera.cullFrustum);
    }

    public static void endMain(LevelRenderContext context) {
        var level = Minecraft.getInstance().level;
        if (!ourSky(level)) return;
        var camera = context.levelState().cameraRenderState;
        float partial = context.levelState().worldPartialTicks;
        WeatherShader.frame(camera.projectionMatrix, camera.fogData);
        if (puddles != null) puddles.render(camera.viewRotationMatrix, camera.projectionMatrix, camera.pos, camera.cullFrustum, partial);
        if (ownsRain()) renderWeather(level, partial, camera.pos.x, camera.pos.y, camera.pos.z);
        LIGHTNING.renderBolts(camera.viewRotationMatrix, Minecraft.getInstance().gameRenderer.mainCamera(), partial);
    }

    /** Called after world postprocessing, before HUD drawing. */
    public static void renderScreen() {
        var minecraft = Minecraft.getInstance();
        if (ourSky(minecraft.level)) Cinematics.renderScreen(minecraft.gameRenderer.gameRenderState().levelRenderState.worldPartialTicks);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fog
    // ------------------------------------------------------------------------------------------------------------

    private static void applyFogDistance(FogData fog, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!ourSky(level)) {
            return;
        }
        float view = skyView(partialTick);
        float rain = ClientWeather.rain() * view;
        double strength = ClientConfig.FOG.get();
        float mist = Mist.density() * view;
        // The atmosphere draws the haze itself; the vanilla fog only hides the edge of the world as usual.
        if ((rain <= 0.0F || strength <= 0.0) && mist <= 1.0E-4F || Atmosphere.active()) {
            return;
        }
        float far = Math.min(fog.environmentalEnd, fog.renderDistanceEnd);
        // Without the atmosphere a plain fog stands in for the haze, about half as far as one can see; the mist
        // closes in further, to where it hides most things.
        float target = ClientWeather.visibility * 0.5F;
        float k = strength <= 0.0 ? 0.0F
                : (float) Math.min(1.0, rain * Math.sqrt(Math.max(ClientWeather.intensity(), 0.0F)) * strength) * ClientWeather.shade();
        if (mist > 1.0E-4F) {
            // A soft veil far out, not a wall of fog: the vanilla fog cannot fade the mist out with distance.
            float mistTarget = 6.0F / mist;
            float mistK = 0.7F * Math.min(1.0F, mist / MistModel.DENSITY);
            if (k <= 0.0F || mistTarget < target) {
                target = mistTarget;
                k = Math.max(k, mistK);
            }
        }
        if (target >= far || k <= 0.0F) {
            return;
        }
        float newFar = far + (target - far) * k;
        float near = fog.environmentalStart;
        // The haze starts a little in front of the camera instead of a clear zone with a wall of fog.
        float newNear = Math.min(near, newFar * (0.4F - 0.25F * k));
        fog.environmentalEnd = newFar;
        fog.environmentalStart = Math.max(0.0F, newNear);
    }

    public static void applyFog(FogData fog, FogType type, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!ourSky(level) || type != FogType.NONE && type != FogType.ATMOSPHERIC) {
            return;
        }
        float r = fog.color.x;
        float g = fog.color.y;
        float b = fog.color.z;
        float rain = ClientWeather.rain() * skyView(partialTick) * (float) Math.min(1.0, ClientConfig.FOG.get());
        if (rain > 0.0F) {
            // Rain washes the colour out towards a deep, neutral grey.
            float luma = r * 0.3F + g * 0.59F + b * 0.11F;
            float k = rain * 0.55F * ClientWeather.shade();
            r += (luma * 0.84F - r) * k;
            g += (luma * 0.9F - g) * k;
            b += (luma * 0.97F - b) * k;
        }
        float flash = LIGHTNING.flash(partialTick);
        if (flash > 0.0F) {
            r += flash * 0.35F;
            g += flash * 0.37F;
            b += flash * 0.45F;
        }
        fog.color.x = Math.min(1.0F, r);
        fog.color.y = Math.min(1.0F, g);
        fog.color.z = Math.min(1.0F, b);
        applyFogDistance(fog, partialTick);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Debug
    // ------------------------------------------------------------------------------------------------------------

    public static void onDebugText(java.util.List<String> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ourSky(minecraft.level)) {
            return;
        }
        lines.add("");
        lines.add(String.format("Petrichor: %s (level %.2f)%s, rain %.2f, intensity %.2f, wetness %.2f",
                ClientWeather.type().id(), ClientWeather.level(), ClientWeather.syncedWithServer() ? " (server)" : "",
                ClientWeather.rain(), ClientWeather.intensity(), ClientWeather.wetness()));
        lines.add(Mist.describe());
        lines.add(String.format("Sunshine %.2f, rainbow %.2f", ClientWeather.sunshine(), Rainbow.strength()));
        lines.add(String.format("Drops %d, effects %d, puddle chunks %d (%d quads), strikes %d",
                PRECIPITATION.lastDrops(), FX.count(), puddles == null ? 0 : puddles.chunkCount(),
                puddles == null ? 0 : puddles.lastQuads(), LIGHTNING.strikeCount()));
        lines.add(String.format("Wind %.2f, %.2f, drip sources %d", ClientWeather.windX(), ClientWeather.windZ(),
                puddles == null ? 0 : puddles.emitterCount()));
        lines.add("Rain sound: " + RainSounds.debugSummary());
        if (puddles != null && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            lines.add("Puddle: " + puddles.describe(hit.getBlockPos().getX(), hit.getBlockPos().getZ()));
        }
    }
}
