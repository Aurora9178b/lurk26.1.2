#version 330 core

precision lowp float;

out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform RectData {
    vec4 u_Rect;
    vec4 u_Params;
    vec4 u_UVData;
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

    vec2 topLeft = u_Rect.xy - u_Rect.zw;
    vec2 fullSize = u_Rect.zw * 2.0;

    vec2 linear_uv = (logicalPos - topLeft) / fullSize;

    vec2 tex_uv = vec2(
        u_UVData.x + linear_uv.x * u_UVData.z, // uStart + progress * uWidth
        u_UVData.y + linear_uv.y * u_UVData.w  // vStart + progress * vHeight
    );

    vec4 texColor = texture(u_Texture, tex_uv);

    if (texColor.a == 0.0) {
        discard;
    }

    color = vec4(texColor.rgb, texColor.a * smoothedAlpha);
}
