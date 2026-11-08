#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform LiquidGlassData {
    vec4 u_Resolution;
    vec4 u_Rect;
    vec4 u_Shape;
    vec4 u_Effects;
    vec4 u_GlowInfo;
    vec4 u_RefractionInfo;
    vec4 u_Tint;
};

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

float refractionCurve(float x) {
    return 1.0 - 2.3 * pow(5.2 * 2.718281828459045, -6.9 * x - 0.7);
}

float roundRectDistance(vec2 position, vec4 rect, vec4 radius) {
    vec2 halfSize = rect.zw;
    vec2 center = rect.xy;
    vec2 p = position - center;
    vec2 s = step(0.0, p);
    float rCurrent = mix(
        mix(radius.x, radius.w, s.y),
        mix(radius.y, radius.z, s.y),
        s.x
    );
    vec2 q = abs(p) - halfSize + rCurrent;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - rCurrent;
}

float glowValue(vec2 localUV) {
    vec2 glowUV = localUV * 2.0 - 1.0;
    return sin(atan(glowUV.y, glowUV.x) - 0.5);
}

bool outOfBounds(vec2 sampleUV) {
    return max(sampleUV.x, sampleUV.y) > 1.0 || min(sampleUV.x, sampleUV.y) < 0.0;
}

void main() {
    vec2 frag = vec2(gl_FragCoord.x, u_Resolution.y - gl_FragCoord.y);
    vec2 localUV = (frag - (u_Rect.xy - u_Rect.zw)) / max(u_Rect.zw * 2.0, vec2(0.0001));
    float d = roundRectDistance(frag, u_Rect, u_Shape);

    float aa = max(0.003, fwidth(d));
    float edge = 1.0 - smoothstep(-aa, aa, d);
    if (edge <= 0.0) {
        discard;
    }

    float dist = max(-d / max(1.0, min(u_Rect.z, u_Rect.w)), 0.0);
    vec2 sampleUV = vec2(gl_FragCoord.x / u_Resolution.x, gl_FragCoord.y / u_Resolution.y);

    if (abs(u_Effects.y) > 0.0001) {
        float refraction = pow(refractionCurve(dist), u_Effects.y);
        vec2 local = frag - u_Rect.xy;
        vec2 midPointNDC = vec2(u_Rect.x / u_Resolution.x, 1.0 - u_Rect.y / u_Resolution.y) * 2.0 - 1.0;
        vec2 localOffsetNDC = vec2(local.x / u_Resolution.x, -local.y / u_Resolution.y) * 2.0;
        vec2 targetNDC = midPointNDC + localOffsetNDC * refraction;
        sampleUV = targetNDC * 0.5 + vec2(0.5);
    }

    if (outOfBounds(sampleUV)) {
        color = texture(u_Texture, sampleUV);
        return;
    }

    vec3 glassColor = texture(u_Texture, sampleUV).rgb;
    glassColor += vec3((rand(frag * 1e-3) - 0.5) * u_Effects.x);

    float glow = glowValue(localUV);
    float glowMask = smoothstep(u_GlowInfo.x, u_GlowInfo.y, dist);
    float glowStrength = glow * u_Effects.z * glowMask + 1.0 + u_Effects.w;
    glassColor *= glowStrength;

    color = vec4(glassColor, edge);
}
