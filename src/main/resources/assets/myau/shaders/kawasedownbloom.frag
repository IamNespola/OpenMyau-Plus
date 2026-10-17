#version 120
uniform sampler2D inTexture;
uniform vec2 offset, halfpixel, iResolution;
void main() {
    vec2 uv = vec2(gl_FragCoord.xy / iResolution);
    vec4 sum = texture2D(inTexture, gl_TexCoord[0].st) * 4.0;
    sum += texture2D(inTexture, uv - halfpixel.xy * offset);
    sum += texture2D(inTexture, uv + halfpixel.xy * offset);
    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
    sum += texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);
    gl_FragColor = vec4(sum.rgb * .125, sum.a * .125);
}