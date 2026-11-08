#version 330 core

precision lowp float;

out vec4 color;

layout (std140) uniform RectData {
    vec4 u_Rect;
    vec4 u_Params;
    vec4 u_Color;
};

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    vec2 q = abs(centerPos) - size + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    float physicalY = u_Params.x - gl_FragCoord.y;
    vec2 logicalPos = vec2(gl_FragCoord.x, physicalY) / u_Params.y;

    float radius = max(u_Params.z, 0.0);
    float blurRadius = max(u_Params.w, 0.001);
    float dist = roundedBoxSDF(logicalPos - u_Rect.xy, u_Rect.zw, radius);

    float normalizedDist = clamp(dist / blurRadius, 0.0, 1.0);
    float shadowAlpha = pow(1.0 - normalizedDist, 1.5);

    float delta = fwidth(dist);
    float outsideAlpha = smoothstep(-delta, 0.0, dist);
    shadowAlpha *= outsideAlpha;

    if (shadowAlpha <= 0.0) {
        discard;
    }

    color = vec4(u_Color.rgb, u_Color.a * shadowAlpha);
}
