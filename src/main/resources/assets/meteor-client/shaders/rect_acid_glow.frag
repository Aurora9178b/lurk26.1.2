#version 330 core

precision lowp float;

layout (std140) uniform AcidGlowData {
    vec4 u_Rect;
    vec4 u_Params;
    vec4 u_Color1;
    vec4 u_Color2;
    vec4 u_Color3;
    vec4 u_Color4;
    vec4 u_Extra;
};

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    return length(max(abs(center) - size + radius, 0.0)) - radius;
}

vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4) {
    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

void main() {
    vec2 center = u_Rect.xy;
    vec2 halfSize = u_Rect.zw;
    float fbHeight = u_Params.x;
    float scale = u_Params.y;
    float radius = u_Params.z;
    float internalAlpha = u_Params.w;
    float blend = max(u_Extra.x, 0.001);
    float glow = u_Extra.y;

    vec2 pos = vec2(gl_FragCoord.x, fbHeight - gl_FragCoord.y) / scale;
    vec2 topLeft = center - halfSize;
    vec2 normCoord = (pos - topLeft) / max(halfSize * 2.0, vec2(0.001));

    float distance = roundedBoxSDF(pos - center, halfSize, radius);
    float smoothedAlpha = (1.0 - smoothstep(-10.0, 10.0, distance)) * u_Color1.a;
    float smoothedAlpha2 = (1.0 - smoothstep(-1.0, 1.0, distance)) * u_Color1.a;

    vec3 gradientColor = createGradient(normCoord, u_Color1.rgb, u_Color2.rgb, u_Color3.rgb, u_Color4.rgb);

    if (smoothedAlpha2 < glow) {
        fragColor = vec4(gradientColor, smoothedAlpha);
    } else {
        vec3 insideColor = createGradient(normCoord, u_Color1.rgb / blend, u_Color2.rgb / blend, u_Color3.rgb / blend, u_Color4.rgb / blend);
        fragColor = vec4(insideColor, internalAlpha);
    }

    if (fragColor.a <= 0.0) discard;
}
