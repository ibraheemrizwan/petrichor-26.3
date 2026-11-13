package com.pockyl.petrichor.mixin;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.petrichor.client.WeatherClient;
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void petrichor$screenGrade(CallbackInfo ci) { WeatherClient.renderScreen(); }
}
