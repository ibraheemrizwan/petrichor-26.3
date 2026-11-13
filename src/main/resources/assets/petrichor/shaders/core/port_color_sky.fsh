#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform WeatherUniforms { mat4 ModelViewMat; mat4 ProjMat; };
layout(location = 0) in vec4 vertexColor;
layout(location = 0) out vec4 fragColor;
void main(){fragColor=vertexColor;}
