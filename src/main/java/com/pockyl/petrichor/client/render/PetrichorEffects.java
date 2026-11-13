package com.pockyl.petrichor.client.render;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import com.pockyl.petrichor.client.WeatherClient;
/** Storm tint and lightning now change extracted lighting, replacing the old dimension-effects override. */
public final class PetrichorEffects {
    private PetrichorEffects() {}
    public static void applyLightmap(LightmapRenderState state,float partialTick) {
        if (!state.needsUpdate) return;
        float gloom=WeatherClient.gloom();
        var colors=new Vector3f(state.skyLightColor);
        float luma=colors.x*0.3F+colors.y*0.59F+colors.z*0.11F;
        colors.lerp(new Vector3f(luma*0.92F,luma*0.96F,luma),gloom*0.35F).mul(1-gloom*0.14F);
        float flash=WeatherClient.flash(partialTick);
        colors.add(flash*0.6F,flash*0.65F,flash*0.8F);
        state.skyLightColor=colors;
    }
}
