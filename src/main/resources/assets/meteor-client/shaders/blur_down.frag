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
    vec2 offset = u_HalfTexelSize * u_Offset;

    vec4 s[5];
    s[0] = texture(u_Texture, uv);
    s[1] = texture(u_Texture, uv - offset);
    s[2] = texture(u_Texture, uv + offset);
    s[3] = texture(u_Texture, uv + vec2(offset.x, -offset.y));
    s[4] = texture(u_Texture, uv - vec2(offset.x, -offset.y));

    vec3 rgbSum = vec3(0.0);
    float alphaSum = 0.0;
    float weights[5] = float[](4.0, 1.0, 1.0, 1.0, 1.0);

    for(int i = 0; i < 5; i++) {
        rgbSum += s[i].rgb * s[i].a * weights[i];
        alphaSum += s[i].a * weights[i];
    }

    if (alphaSum > 0.0) {
        color = vec4(rgbSum / alphaSum, (s[0].a * 4.0 + s[1].a + s[2].a + s[3].a + s[4].a) / 8.0);
    } else {
        color = vec4(0.0);
    }
}
