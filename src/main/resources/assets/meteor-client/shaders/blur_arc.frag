#version 330 core

out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform BlurRectData {
    vec4 u_ResolutionQuality;
    vec4 u_Rect;
    vec4 u_Color;
    vec4 u_Radii;
};

float roundRectDistance(vec2 position, vec4 rect, vec4 radius) {
    vec2 halfSize = (rect.zw - rect.xy) * 0.5;
    vec2 center = (rect.xy + rect.zw) * 0.5;
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

vec4 blur() {
    const float TAU = 6.28318530718;

    vec2 resolution = u_ResolutionQuality.xy;
    float quality = max(u_ResolutionQuality.z, 0.0);
    vec2 sampleRadius = quality / resolution;
    vec2 uv = gl_FragCoord.xy / resolution;

    vec4 blurred = texture(u_Texture, uv);
    float stepSize = TAU / 16.0;

    for (float d = 0.0; d < TAU; d += stepSize) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            blurred += texture(u_Texture, uv + vec2(cos(d), sin(d)) * sampleRadius * i);
        }
    }

    return blurred / 81.0 + u_Color;
}

void main() {
    vec2 fragPosition = vec2(gl_FragCoord.x, u_ResolutionQuality.y - gl_FragCoord.y);
    vec4 bounds = vec4(u_Rect.xy, u_Rect.xy + u_Rect.zw);
    float dist = roundRectDistance(fragPosition, bounds, u_Radii);
    float delta = fwidth(dist);
    float alpha = 1.0 - smoothstep(-delta, delta, dist);

    if (alpha < 0.001) discard;
    color = vec4(blur().rgb, alpha);
}
