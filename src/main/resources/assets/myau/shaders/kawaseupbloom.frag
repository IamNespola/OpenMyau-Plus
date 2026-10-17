#version 120
uniform sampler2D inTexture, textureToCheck;
uniform vec2 halfpixel, offset, iResolution;
uniform int check;
void main() {
    vec2 uv = vec2(gl_FragCoord.xy / iResolution);
    vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
    sum += texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;
    sum += texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;
    
    vec4 bloom = vec4(sum.rgb / 12.0, mix(1.0, texture2D(textureToCheck, gl_TexCoord[0].st).a, check));
    
    // Add the bloom on top with additive blending
    if (check == 1) {
        vec4 original = texture2D(textureToCheck, gl_TexCoord[0].st);
        gl_FragColor = bloom + original;
    } else {
        gl_FragColor = bloom;
    }
}