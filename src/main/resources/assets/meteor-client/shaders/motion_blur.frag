#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform BlurData {
    float u_Alpha;
};

void main() {
    vec4 texColor = texture(u_Texture, uv);
    color = vec4(texColor.rgb, u_Alpha);
}
