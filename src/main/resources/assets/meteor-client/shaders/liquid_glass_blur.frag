#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform LiquidGlassBlurData {
    vec4 u_Resolution;
    vec4 u_BlurInfo;
};

const float TAU = 6.28318530718;

void main() {
    vec2 inputResolution = u_Resolution.xy;
    float quality = u_BlurInfo.z;
    vec2 radius = (u_BlurInfo.xy * quality) / max(inputResolution, vec2(1.0));

    vec4 sum = texture(u_Texture, uv);

    float stepSize = TAU / 16.0;
    for (float d = 0.0; d < TAU; d += stepSize) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            sum += texture(u_Texture, uv + vec2(cos(d), sin(d)) * radius * i);
        }
    }

    color = sum / 81.0;
}
