#version 330 core

precision lowp float;

out vec4 color;

layout (std140) uniform RectCornersData {
    vec4 u_Rect;
    vec4 u_Params;
    vec4 u_Color;
    vec4 u_Radii;
};

float roundedBoxSDF(vec2 centerPos, vec2 size, vec4 radii) {
    vec2 corner = step(0.0, centerPos);
    float radius = mix(
        mix(radii.x, radii.w, corner.y),
        mix(radii.y, radii.z, corner.y),
        corner.x
    );

    vec2 q = abs(centerPos) - size + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    float physicalY = u_Params.x - gl_FragCoord.y;
    vec2 logicalPos = vec2(gl_FragCoord.x, physicalY) / u_Params.y;

    float dist = roundedBoxSDF(logicalPos - u_Rect.xy, u_Rect.zw, u_Radii);
    float delta = max(fwidth(dist), 0.001);
    float smoothedAlpha = 1.0 - smoothstep(-delta, delta, dist);

    if (smoothedAlpha <= 0.0) {
        discard;
    }

    color = vec4(u_Color.rgb, u_Color.a * smoothedAlpha);
}
