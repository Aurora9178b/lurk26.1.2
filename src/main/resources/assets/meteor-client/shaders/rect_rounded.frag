#version 330 core

precision lowp float;

out vec4 color;

layout (std140) uniform RectData {
    vec4 u_Rect;
    vec4 u_Params;
    vec4 u_Color;
};

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

void main() {
    float physicalY = u_Params.x - gl_FragCoord.y;
    vec2 logicalPos = vec2(gl_FragCoord.x, physicalY) / u_Params.y;

    float dist = roundedBoxSDF(logicalPos - u_Rect.xy, u_Rect.zw, u_Params.z);
    float smoothedAlpha = 1.0 - smoothstep(0.0, 1.0, dist);

    if (smoothedAlpha <= 0.0) {
        discard;
    }

    color = vec4(u_Color.rgb, u_Color.a * smoothedAlpha);
}
