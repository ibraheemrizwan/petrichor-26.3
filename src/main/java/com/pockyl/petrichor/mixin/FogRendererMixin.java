package com.pockyl.petrichor.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.pockyl.petrichor.client.WeatherClient;
@Mixin(AtmosphericFogEnvironment.class)
abstract class FogRendererMixin {
    @ModifyExpressionValue(method={"getBaseColor","updateRainFogState"},at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float petrichor$sunShowerFog(float rain) { return WeatherClient.rainShade(rain); }
}
