package com.pockyl.petrichor.client.render;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
/** Surface normals also carry the existing runoff parameters; vanilla 26.3 BLOCK no longer contains them. */
public final class WeatherVertexFormats {
    private WeatherVertexFormats() {}
    public static final VertexFormat SURFACE = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT).addAttribute("UV2", GpuFormat.RG16_SINT)
            .addAttribute("Normal", GpuFormat.RGBA8_SNORM).build();
}
