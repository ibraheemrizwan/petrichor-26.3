package com.pockyl.petrichor.mixin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.pockyl.petrichor.compat.Seasons;
import com.pockyl.petrichor.client.WeatherClient;
/** Seasonal precipitation and suppression of duplicate vanilla rain sounds/splashes. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method="getPrecipitationAt",at=@At("RETURN"),cancellable=true)
    private void petrichor$seasonalPrecipitation(BlockPos pos,CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (Seasons.active()) cir.setReturnValue(Seasons.precipitationAt((ClientLevel)(Object)this,pos));
    }
    @Inject(method="tickWeatherEffects",at=@At("HEAD"),cancellable=true)
    private void petrichor$rainEffects(CallbackInfo ci) { if (WeatherClient.ownsRain()) ci.cancel(); }
}
