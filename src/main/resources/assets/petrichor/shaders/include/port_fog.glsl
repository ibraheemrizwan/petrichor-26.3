float fog_distance(vec3 position, int shape) {
    return shape == 0 ? length(position) : max(length(position.xz), abs(position.y));
}
vec4 linear_fog(vec4 color, float distance, float start, float end, vec4 fog) {
    if (distance <= start) return color;
    float amount = end <= start ? 1.0 : smoothstep(start, end, distance);
    return vec4(mix(color.rgb, fog.rgb, amount * fog.a), color.a);
}
