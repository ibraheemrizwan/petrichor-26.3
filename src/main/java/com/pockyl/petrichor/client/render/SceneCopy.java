package com.pockyl.petrichor.client.render;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
/** Native copies of the opaque scene for screen-space reflections. */
public final class SceneCopy {
    private static TextureTarget world;
    private static long frame, worldFrame = -1;
    private SceneCopy() {}
    public static void newFrame() { frame++; }
    public static TextureTarget world() {
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(world == null) world = new TextureTarget("Petrichor scene",main.width,main.height,main.getColorTexture().getFormat(),main.getDepthTexture().getFormat());
        if(world.width!=main.width || world.height!=main.height) world.resize(main.width,main.height);
        if(worldFrame!=frame) { world.copyColorFrom(main); world.copyDepthFrom(main); worldFrame=frame; }
        return world;
    }
}
