package com.pockyl.petrichor.client.compat;

import net.minecraft.util.Mth;

import com.pockyl.petrichor.Petrichor;
import com.pockyl.petrichor.client.ClientWeather;
import com.pockyl.petrichor.client.WeatherClient;
import com.pockyl.petrichor.client.render.Rainbow;

/**
 * Hands the rain to shader packs once per frame, before the world is drawn ({@code petrichor:shader_data}, see
 * {@link ShaderData}): its type, sun showers, wetness, rainbows, lightning and wind. Iris masks the mod's own rainbow,
 * atmosphere and sky flash while a pack is on, so a pack that reads this can draw them itself.
 * <pre>
 * 3  rain 0..1, rain level / 3 (drizzle .. thunderstorm), sunshine (sun shower), wetness
 * 4  rainbow strength, rainbow side (1 east, -1 west), lightning flash, sky view
 * 5  gust factor of the rain (1 = steady), puddles tagged with ShaderPacks.PUDDLE_RENDER_TYPE (1), -, -
 * </pre>
 */
public final class PackWeather {
    private static final ShaderData DATA = new ShaderData(Petrichor.MOD_ID, 6);

    private PackWeather() {
    }

    public static void onFrameStart(float partialTick) {
        if (!DATA.begin(ShaderPacks.inUse())) {
            return;
        }
        float rain = ClientWeather.rain();
        DATA.wind(ClientWeather.windX(), ClientWeather.windZ(), partialTick);
        DATA.set(3, rain, Mth.clamp(ClientWeather.level() / 3.0F, 0.0F, 1.0F), ClientWeather.sunshine(), ClientWeather.wetness());
        DATA.set(4, Rainbow.strength(), Rainbow.sunSide(), WeatherClient.flash(partialTick), WeatherClient.skyView(partialTick));
        DATA.set(5, rain > 0.001F ? ClientWeather.intensity() / rain : 1.0F, 1.0F, 0.0F, 0.0F);
        DATA.upload();
    }
}
