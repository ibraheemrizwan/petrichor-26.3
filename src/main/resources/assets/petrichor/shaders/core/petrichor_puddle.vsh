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
    mat4 InvProjMat;
    float Wetness;
    float Coverage;
    float RainAmount;
    float Flow;
    float SsrSteps;
};

#include <petrichor:port_fog.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;

uniform sampler2D Sampler2;






layout(location = 0) out float vertexDistance;
layout(location = 1) out vec2 worldUv;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec4 lightColor;
layout(location = 4) out vec3 relativePos;
layout(location = 5) out vec3 viewPos;
layout(location = 6) out vec3 flow;

void main() {
    vec3 pos = Position + ChunkOffset;
    vec4 view = ModelViewMat * vec4(pos, 1.0);
    gl_Position = ProjMat * view;

    vertexDistance = fog_distance(pos, FogShape);
    worldUv = UV0;
    vertexColor = Color;
    lightColor = texelFetch(Sampler2, UV2 / 16, 0);
    relativePos = pos;
    viewPos = view.xyz;
    // The normal carries the runoff: x and z the direction water runs, y how much of it.
    flow = Normal;
}
