#version 120

uniform sampler2D inTexture, textureToCheck;
uniform vec2 texelSize, direction;
uniform float radius;
uniform float weights[256];

#define offset texelSize * direction

void main() {
    vec2 uv = gl_TexCoord[0].st;
    
    vec4 color = texture2D(inTexture, uv) * weights[0];
    float totalWeight = weights[0];
    
    for (float f = 1.0; f <= radius; f++) {
        color += texture2D(inTexture, uv + f * offset) * weights[int(f)];
        color += texture2D(inTexture, uv - f * offset) * weights[int(f)];
        totalWeight += weights[int(f)] * 2.0;
    }
    
    gl_FragColor = color / totalWeight;
}