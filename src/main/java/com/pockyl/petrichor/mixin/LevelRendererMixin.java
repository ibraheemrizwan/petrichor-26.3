package com.pockyl.petrichor.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.pockyl.petrichor.client.WeatherClient;
/** Keep snow in vanilla's weather pass, including seasonal snow. */
@Mixin(WeatherEffectRenderer.class)
abstract class LevelRendererMixin {
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation petrichor$snowOnly(ClientLevel level,BlockPos pos,Operation<Biome.Precipitation> original) {
        var precipitation=original.call(level,pos);
        return precipitation==Biome.Precipitation.RAIN && WeatherClient.ownsRain() ? Biome.Precipitation.NONE : precipitation;
    }
}
