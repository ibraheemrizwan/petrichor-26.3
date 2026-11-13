#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform WeatherUniforms {
    float Pass;
    float Darken;
    float Exposure;
    vec3 FlashColor;
    vec2 ScreenSize;
};

// A full-screen quad given directly in clip space.
layout(location = 0) in vec3 Position;

layout(location = 0) out vec2 ndc;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    ndc = Position.xy;
}
