package com.pockyl.petrichor.client.render;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
/** Native pipelines retain the original rain, reflection, runoff, atmosphere and screen-grade programs. */
public final class PetrichorShaders {
    private static WeatherShader rain, puddle, sheet, veil, atmosphere, grade, color, colorSky, colorSkyAlpha;
    private PetrichorShaders() {}
    public static void register() {
        color = new WeatherShader("port_color",DefaultVertexFormat.POSITION_COLOR,false,BlendFunction.LIGHTNING);
        colorSky = new WeatherShader("port_color_sky",DefaultVertexFormat.POSITION_COLOR,true,BlendFunction.LIGHTNING);
        colorSkyAlpha = new WeatherShader("port_color_alpha",DefaultVertexFormat.POSITION_COLOR,true,BlendFunction.TRANSLUCENT);
        rain = new WeatherShader("petrichor_rain", DefaultVertexFormat.PARTICLE, false, BlendFunction.TRANSLUCENT);
        puddle = new WeatherShader("petrichor_puddle", WeatherVertexFormats.SURFACE, false, BlendFunction.TRANSLUCENT);
        sheet = new WeatherShader("petrichor_sheet", WeatherVertexFormats.SURFACE, false, BlendFunction.TRANSLUCENT);
        veil = new WeatherShader("petrichor_veil", DefaultVertexFormat.PARTICLE, false, BlendFunction.TRANSLUCENT);
        atmosphere = new WeatherShader("petrichor_atmosphere", DefaultVertexFormat.POSITION, true, BlendFunction.TRANSLUCENT);
        grade = new WeatherShader("petrichor_grade", DefaultVertexFormat.POSITION, true, BlendFunction.TRANSLUCENT);
    }
    public static WeatherShader rain() { return rain; }
    public static WeatherShader puddle() { return puddle; }
    public static WeatherShader sheet() { return sheet; }
    public static WeatherShader veil() { return veil; }
    public static WeatherShader atmosphere() { return atmosphere; }
    public static WeatherShader color() { return color; }
    public static WeatherShader colorSky() { return colorSky; }
    public static WeatherShader colorSkyAlpha() { return colorSkyAlpha; }
    public static WeatherShader grade() { return grade; }
}
