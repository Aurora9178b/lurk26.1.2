#version 330 core

precision lowp float;

out vec4 color;

layout (std140) uniform MainMenuData {
    vec4 u_Data;
};

// License: CC BY 3.0
// Author: Jan Mroz (jaszunio15)
vec2 hash22(vec2 x) {
    return fract(sin(x * mat2(43.37862, 24.58974, 32.37621, 53.32761)) * 4534.3897);
}

float hash12(vec2 x) {
    return fract(sin(dot(x, vec2(43.37861, 34.58761))) * 342.538772);
}

vec2 getCellPoint(vec2 cell) {
    float time = u_Data.z * (hash12(cell + 0.123) - 0.5) * 0.5;
    float c = cos(time);
    float s = sin(time);
    vec2 hash = (hash22(cell) - 0.5) * mat2(c, s, -s, c) + 0.5;
    return hash + cell;
}

float getCellValue(vec2 cell) {
    return hash12(cell);
}

float makeSmooth(float x) {
    return mix(x * x * (3.0 - 2.0 * x), sqrt(x), 0.01);
}

float modifiedVoronoiNoise12(vec2 uv) {
    vec2 rootCell = floor(uv);
    float value = 0.0;

    for (float x = -1.0; x <= 1.0; x++) {
        for (float y = -1.0; y <= 1.0; y++) {
            vec2 cell = rootCell + vec2(x, y);
            vec2 cellPoint = getCellPoint(cell);
            float cellValue = getCellValue(cell);
            float cellDist = distance(uv, cellPoint);
            value += makeSmooth(clamp(1.0 - cellDist, 0.0, 1.0)) * cellValue;
        }
    }

    return value * 0.5;
}

float layeredNoise12(vec2 x) {
    float sum = 0.0;
    float maxValue = 0.0;

    for (float i = 1.0; i <= 2.0; i *= 2.0) {
        float noise = modifiedVoronoiNoise12(x * i) / i;
        sum += noise;
        maxValue += 1.0 / i;
    }

    return sum / maxValue;
}

void main() {
    vec2 size = max(u_Data.xy, vec2(1.0));
    float time = u_Data.z;

    vec2 uv = (gl_FragCoord.xy - 0.5 * size.xy) / size.y;
    vec2 stretchedUV = (gl_FragCoord.xy - 0.5 * size.xy) / size.xy;
    float vignette = smoothstep(0.65, 0.0, length(stretchedUV));

    uv.y -= time * 0.05;
    uv *= 6.0;

    vec4 col = sin(time * 0.1 + uv.y * 0.2 + vec4(0.0, 2.0, 4.0, 6.0)) * 0.5 + 0.5;
    vec4 col2 = sin(time * 0.1 + 0.6 + uv.y * 0.2 + vec4(0.0, 2.0, 4.0, 6.0)) * 0.5 + 0.5;

    uv += layeredNoise12(uv);
    float noise = layeredNoise12(uv) * vignette;

    color = vec4(smoothstep(-0.14, 1.1, mix(col, col2 * 0.2, 1.0 - noise * 2.0)).rgb, 1.0);
}
