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

// Like the vanilla particle shader, but faint drops are kept instead of being cut off at alpha 0.1.
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.004) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
