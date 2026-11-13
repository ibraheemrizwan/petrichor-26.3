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

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;

uniform sampler2D Sampler2;






layout(location = 0) out float vertexDistance;
layout(location = 1) out vec2 sheetUv;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec4 lightColor;

void main() {
    vec3 pos = Position + ChunkOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    sheetUv = UV0;
    vertexColor = Color;
    lightColor = texelFetch(Sampler2, UV2 / 16, 0);
}
