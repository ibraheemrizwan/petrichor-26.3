package com.pockyl.petrichor.mixin;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.petrichor.client.WeatherClient;
/** Native pipeline draws must occur after vanilla closes its render pass. */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
    @Inject(method="render",at=@At("RETURN"))
    private void petrichor$skyWeather(CallbackInfo ci) { WeatherClient.afterSky(); }
}
