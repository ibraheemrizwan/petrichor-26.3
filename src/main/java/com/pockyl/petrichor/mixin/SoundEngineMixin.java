package com.pockyl.petrichor.mixin;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pockyl.petrichor.client.WeatherClient;
import com.pockyl.petrichor.client.sound.Muffler;
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @WrapMethod(method = "play")
    private SoundEngine.PlayResult petrichor$filterSound(SoundInstance sound, Operation<SoundEngine.PlayResult> original) {
        SoundInstance replacement = WeatherClient.filterSound(sound);
        if (replacement == null) return SoundEngine.PlayResult.NOT_STARTED;
        var result = original.call(replacement);
        var channel = instanceToChannel.get(replacement);
        if (channel != null) channel.execute(source -> com.pockyl.petrichor.client.sound.RainSounds.onSourceReady(replacement, source));
        return result;
    }
    @Inject(method = "reload", at = @At("HEAD"))
    private void petrichor$resetAudio(CallbackInfo ci) { Muffler.reset(); }
}
