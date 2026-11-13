#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform WeatherUniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    vec3 ChunkOffset;
    vec4 ColorModulator;
    float FogStart;
    float FogEnd;
    vec4 FogColor;
    int FogShape;
    float PetrichorTime;
    vec3 SkyColor;
    float Flow;
};

#include <petrichor:port_fog.glsl>
#include <petrichor:petrichor_noise.glsl>









layout(location = 0) in float vertexDistance;
layout(location = 1) in vec2 sheetUv;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec4 lightColor;

layout(location = 0) out vec4 fragColor;

// Water spilling over the edge of a step. x runs along the edge in blocks, y down the face (0 at the lip, 1 at the foot).
void main() {
    float t = PetrichorTime;
    float lip = sheetUv.y;
    // Threads of water: thin falling streaks that come and go along the edge.
    float threads = petrichor_noise(vec2(sheetUv.x * 7.0, t * 0.35), 4096.0);
    float streaks = petrichor_noise(vec2(sheetUv.x * 22.0, lip * 3.0 - t * 4.0), 4096.0);
    float body = smoothstep(0.35, 0.8, threads) * (0.55 + 0.45 * streaks);
    float fadeTop = smoothstep(0.0, 0.08, lip);
    float fadeBottom = 1.0 - smoothstep(0.75, 1.0, lip);
    float alpha = body * fadeTop * fadeBottom * vertexColor.a * Flow;

    // Mostly a dark wet film over the face, with glints of the sky on the threads of water.
    vec3 light = lightColor.rgb;
    vec3 sky = mix(FogColor.rgb, SkyColor, 0.6) * max(light, vec3(0.06));
    float glint = smoothstep(0.65, 0.95, streaks) * smoothstep(0.5, 0.9, threads);
    vec3 film = vertexColor.rgb * light * 0.12;
    vec4 color = vec4(mix(film, sky * 1.1, glint), alpha * (0.45 + 0.4 * glint));
    color *= ColorModulator;
    if (color.a < 0.003) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
