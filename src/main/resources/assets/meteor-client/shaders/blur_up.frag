#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform BlurData {
    vec2 u_HalfTexelSize;
    float u_Offset;
};

void main() {
    vec2 off = u_HalfTexelSize * u_Offset;

    vec4 s[8];
    s[0] = texture(u_Texture, uv + vec2(-off.x * 2.0, 0.0));
    s[1] = texture(u_Texture, uv + vec2(-off.x, off.y));
    s[2] = texture(u_Texture, uv + vec2(0.0, off.y * 2.0));
    s[3] = texture(u_Texture, uv + off);
    s[4] = texture(u_Texture, uv + vec2(off.x * 2.0, 0.0));
    s[5] = texture(u_Texture, uv + vec2(off.x, -off.y));
    s[6] = texture(u_Texture, uv + vec2(0.0, -off.y * 2.0));
    s[7] = texture(u_Texture, uv - off);

    vec3 rgbSum = vec3(0.0);
    float alphaSum = 0.0;
    float totalA = 0.0;

    for(int i = 0; i < 8; i++) {
        float w = (i == 1 || i == 3 || i == 5 || i == 7) ? 2.0 : 1.0;
        rgbSum += s[i].rgb * s[i].a * w;
        alphaSum += s[i].a * w;
        totalA += s[i].a * w;
    }

    if (alphaSum > 0.0) {
        color = vec4(rgbSum / alphaSum, totalA / 12.0);
    } else {
        color = vec4(0.0);
    }
}
