#version 330 core

precision lowp float;

layout (location = 0) in vec2 pos;

layout (std140) uniform BlurRectData {
    vec4 u_ResolutionQuality;
    vec4 u_Rect;
    vec4 u_Color;
    vec4 u_Radii;
};

void main() {
    gl_Position = vec4(pos, 0.0, 1.0);
}
