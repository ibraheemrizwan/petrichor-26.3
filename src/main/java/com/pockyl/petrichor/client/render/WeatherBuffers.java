package com.pockyl.petrichor.client.render;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
/** Reusable frame geometry storage; completed meshes are consumed and released by WeatherShader. */
public final class WeatherBuffers {
    private static final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(1 << 18);
    private WeatherBuffers() {}
    public static BufferBuilder begin(PrimitiveTopology mode, VertexFormat format) { return new BufferBuilder(SCRATCH,mode,format); }
}
