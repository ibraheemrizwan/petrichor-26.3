#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform WeatherUniforms {
    float Pass;
    float Darken;
    float Exposure;
    vec3 FlashColor;
    vec2 ScreenSize;
};

// Two passes over the finished picture. Pass 0 is drawn with multiplying blending: darkening, a little deeper at the
// edges. Pass 1 adds light: the overexposure of a close lightning flash.






layout(location = 0) in vec2 ndc;

layout(location = 0) out vec4 fragColor;

void main() {
    float aspect = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 q = vec2(ndc.x * aspect, ndc.y);
    // 0 in the centre, 1 in the corners.
    float d = length(q) / length(vec2(aspect, 1.0));
    if (Pass < 0.5) {
        float edge = smoothstep(0.3, 1.05, d);
        float keep = 1.0 - Darken * (0.85 + 0.15 * edge);
        fragColor = vec4(vec3(keep), 1.0);
    } else {
        // The flash floods the view, a little less at the edges.
        fragColor = vec4(FlashColor * Exposure * (1.0 - 0.3 * d), 1.0);
    }
}
