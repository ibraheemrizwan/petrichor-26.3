package com.pockyl.petrichor.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.petrichor.client.WeatherClient;
import com.pockyl.petrichor.client.render.PetrichorEffects;
@Mixin(LightmapRenderStateExtractor.class)
abstract class LightmapExtractorMixin {
    @Inject(method="extract",at=@At("TAIL"))
    private void petrichor$stormLight(LightmapRenderState state,float partial,CallbackInfo ci) {
        if (WeatherClient.ourSky(Minecraft.getInstance().level)) PetrichorEffects.applyLightmap(state,partial);
    }
}
