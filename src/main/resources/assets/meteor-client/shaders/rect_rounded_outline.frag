#version 330 core

precision lowp float;

layout (std140) uniform RectData {
    vec4 u_Rect;
    vec4 u_Params;
    vec4 u_ColorData;
};

out vec4 fragColor;

float roundedRectSDF(vec2 p, vec2 center, vec2 halfSize, float radius) {
    vec2 d = abs(p - center) - halfSize + vec2(radius);
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - radius;
}

void main() {
    vec2 u_Center = u_Rect.xy;
    vec2 u_HalfSize = u_Rect.zw;
    float u_FbHeight = u_Params.x;
    float u_Scale = u_Params.y;
    float u_Radius = u_Params.z;
    float u_OutlineWidth = u_Params.w;
    vec4 u_Color = u_ColorData;

    vec2 pos = gl_FragCoord.xy;
    pos.y = u_FbHeight - pos.y;
    pos /= u_Scale;

    float dist = roundedRectSDF(pos, u_Center, u_HalfSize, u_Radius);

    float outlineDist = abs(dist);
    float alpha = smoothstep(u_OutlineWidth, 0.0, outlineDist);

    fragColor = vec4(u_Color.rgb, u_Color.a * alpha);
}
