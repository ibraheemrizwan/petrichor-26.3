#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform WeatherUniforms {
    mat4 InvViewProj;
    vec3 CameraPos;
    float PetrichorTime;
    vec2 Drift;
    float Haze;
    float Falloff;
    float BaseY;
    float Strength;
    float Overcast;
    float Gloom;
    float Shafts;
    float Glow;
    float Flash;
    vec3 SunDir;
    float Mist;
    float MistY;
    float MistHeight;
    vec4 FogColor;
};

// A full-screen quad given directly in clip space.
layout(location = 0) in vec3 Position;

layout(location = 0) out vec2 ndc;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    ndc = Position.xy;
}
