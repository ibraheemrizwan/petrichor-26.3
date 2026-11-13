package com.pockyl.petrichor.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;

import com.pockyl.petrichor.ClientConfig;
import com.pockyl.petrichor.client.ClientWeather;
import com.pockyl.petrichor.client.Mist;
import com.pockyl.petrichor.client.WeatherClient;
import com.pockyl.petrichor.client.compat.ShaderPacks;

/**
 * The air of a rainy day, drawn over the world from its depth buffer before the rain itself (shader
 * {@code petrichor_atmosphere}). Replaces the flat wall of fog: the haze deepens with distance and in low ground, so
 * hills behind hills fade in layers; heavier showers drift through it with the wind; the sky becomes a cloud deck with
 * rain shafts at the horizon, lit by lightning. Low mist lies in front of it all: on the ground, up into the crowns of a
 * forest, in wisps (see {@link Mist}).
 */
public final class Atmosphere {
    /** Optical depth that counts as "can no longer see": 95% haze. */
    private static final float OPAQUE = 3.0F;
    private static TextureTarget depth;
    private static final com.mojang.blaze3d.vertex.ByteBufferBuilder SCREEN = new com.mojang.blaze3d.vertex.ByteBufferBuilder(64);

    private Atmosphere() {
    }

    /** Whether the atmosphere draws the rain's haze this frame (otherwise the vanilla fog is pulled in). */
    public static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        // A shader pack draws its own sky and fog (and would hide this pass anyway).
        // Underground or deep indoors the rain's haze is outside, behind the walls.
        float view = WeatherClient.skyView(1.0F);
        boolean rain = ClientWeather.rain() * view > 0.001F && ClientConfig.FOG.get() > 0.0;
        boolean mist = Mist.density() * view > 1.0E-4F;
        if (!ClientConfig.ATMOSPHERE.get() || PetrichorShaders.atmosphere() == null || !rain && !mist || ShaderPacks.inUse()) {
            return false;
        }
        Camera camera = minecraft.gameRenderer.mainCamera();
        if (camera.getFluidInCamera() != FogType.NONE) {
            return false;
        }
        return !(camera.entity() instanceof LivingEntity living)
                || !living.hasEffect(MobEffects.BLINDNESS) && !living.hasEffect(MobEffects.DARKNESS);
    }

    /** Haze per block at the ground for the current rain. */
    public static float haze() {
        float rain = ClientWeather.rain();
        float gust = rain > 0.001F ? ClientWeather.intensity() / rain : 1.0F;
        return OPAQUE / ClientWeather.visibility * (float) (double) ClientConfig.FOG.get() * Math.clamp(0.75F + 0.25F * gust, 0.7F, 1.3F);
    }

    public static void render(ClientLevel level, float partialTick, double camX, double camY, double camZ) {
        WeatherShader shader = PetrichorShaders.atmosphere();
        if (shader == null || !active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        captureDepth(minecraft);

        float rain = ClientWeather.rain();
        float heaviness = ClientWeather.heaviness;
        // In a sun shower the air stays clear and the sky blue.
        float shade = ClientWeather.shade();
        float fog = (float) Math.min(1.0, ClientConfig.FOG.get());
        float sunAngle = Minecraft.getInstance().gameRenderer.mainCamera().attributeProbe().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, partialTick) * Mth.DEG_TO_RAD;
        float sunX = -Mth.sin(sunAngle);
        float sunY = Mth.cos(sunAngle);
        double seconds = (level.getGameTime() + (double) partialTick) / 20.0 % 7200.0;

        Matrix4f inverse = new Matrix4f(WeatherShader.projection()).mul(WeatherShader.modelView()).invert();
        shader.safeGetUniform("InvViewProj").set(inverse);
        shader.safeGetUniform("CameraPos").set((float) (camX % 16384.0), (float) camY, (float) (camZ % 16384.0));
        shader.safeGetUniform("PetrichorTime").set((float) seconds);
        shader.safeGetUniform("Drift").set((float) (ClientWeather.driftX(partialTick) % 65536.0),
                (float) (ClientWeather.driftZ(partialTick) % 65536.0));
        shader.safeGetUniform("Haze").set(haze());
        shader.safeGetUniform("Falloff").set(1.0F / 40.0F);
        shader.safeGetUniform("BaseY").set((float) level.getSeaLevel());
        shader.safeGetUniform("Strength").set(rain * WeatherClient.skyView(partialTick) * (0.4F + 0.6F * shade));
        // A drizzle only greys the sky; a downpour hides it behind a heavy deck.
        shader.safeGetUniform("Overcast").set(Math.min(1.0F, 0.55F + 0.45F * heaviness) * fog * shade);
        shader.safeGetUniform("Gloom").set(Math.max(Math.clamp((heaviness - 0.4F) / 0.6F, 0.0F, 1.0F), ClientWeather.thunder() * 0.9F));
        shader.safeGetUniform("Shafts").set(Mth.clamp((heaviness - 0.3F) / 0.7F, 0.0F, 1.0F) * shade);
        shader.safeGetUniform("Glow").set(Math.max(0.0F, sunY + 0.2F) * (1.0F - 0.6F * heaviness) * (1.6F - 0.6F * shade) * 0.6F);
        shader.safeGetUniform("Flash").set(WeatherClient.flash(partialTick));
        shader.safeGetUniform("Mist").set(Mist.density() * WeatherClient.skyView(partialTick));
        shader.safeGetUniform("MistY").set(Float.isNaN(Mist.baseY()) ? (float) camY : Mist.baseY());
        shader.safeGetUniform("MistHeight").set(Mist.height());
        shader.safeGetUniform("SunDir").set(sunX, sunY, 0.0F);

        WeatherShader.use(shader);
        WeatherShader.texture(0, depth.getDepthTextureView());
        BufferBuilder quad = new BufferBuilder(SCREEN, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
        quad.addVertex(-1.0F, -1.0F, 0.0F);
        quad.addVertex(1.0F, -1.0F, 0.0F);
        quad.addVertex(1.0F, 1.0F, 0.0F);
        quad.addVertex(-1.0F, 1.0F, 0.0F);
        WeatherShader.drawCurrent(quad.buildOrThrow());
    }

    /** Copies the world's depth so the shader can read it while drawing over the same target. */
    private static void captureDepth(Minecraft minecraft) {
        RenderTarget main = minecraft.gameRenderer.mainRenderTarget();
        if (depth == null) depth = new TextureTarget("Petrichor atmosphere depth", main.width, main.height, null, main.getDepthTexture().getFormat());
        else if (depth.width != main.width || depth.height != main.height) depth.resize(main.width, main.height);
        depth.copyDepthFrom(main);
    }
}
