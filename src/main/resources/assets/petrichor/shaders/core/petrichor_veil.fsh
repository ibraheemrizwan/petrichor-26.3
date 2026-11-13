#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform WeatherUniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    vec4 ColorModulator;
    float FogStart;
    float FogEnd;
    vec4 FogColor;
    int FogShape;
};

#include <petrichor:port_fog.glsl>

uniform sampler2D Sampler0;






layout(location = 0) in float vertexDistance;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// Distant curtains of rain: a tiling sheet of streaks, tinted like the haze it hangs in.
void main() {
    vec4 color = texture(Sampler0, fract(texCoord0)) * vertexColor * ColorModulator;
    if (color.a < 0.003) {
        discard;
    }
    // Only part of the fog is applied: far curtains stay as lighter streaks in the haze instead of vanishing in it.
    vec4 fogged = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    fragColor = vec4(mix(fogged.rgb, color.rgb, 0.45), color.a);
}
