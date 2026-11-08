#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform RoundedData {
    vec4 u_Rect;
    vec2 u_LogicalSize;
    float u_Radius;
};

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

void main() {
    vec2 logicalPos = vec2(uv.x * u_LogicalSize.x, (1.0 - uv.y) * u_LogicalSize.y);
    float dist = roundedBoxSDF(logicalPos - u_Rect.xy, u_Rect.zw, u_Radius);
    float smoothedAlpha = 1.0 - smoothstep(0.0, 1.0, dist);

    if (smoothedAlpha <= 0.0) {
        discard;
    }

    vec4 blur = texture(u_Texture, uv);
    color = vec4(blur.rgb, blur.a * smoothedAlpha);
}
