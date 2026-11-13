package com.pockyl.petrichor.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.pockyl.petrichor.ClientConfig;
public final class PetrichorBoltRenderer extends LightningBoltRenderer {
    public PetrichorBoltRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void submit(LightningBoltRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!ClientConfig.BOLTS.get()) super.submit(state,pose,collector,camera);
    }
}
