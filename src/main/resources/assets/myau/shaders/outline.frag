#version 120

uniform vec2 texelSize, direction;
uniform sampler2D textureIn;
uniform float radius;

#define offset direction * texelSize

void main() {
    vec4 center = texture2D(textureIn, gl_TexCoord[0].st);
    center.rgb *= center.a;
    if (center.a > 0) discard;
    for (float r = 1.0; r <= radius; r++) {
        vec4 alphaCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);
        vec4 alphaCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);
        alphaCurrent1.rgb *= alphaCurrent1.a;
        alphaCurrent2.rgb *= alphaCurrent2.a;
        center += alphaCurrent1 + alphaCurrent2;
    }

    gl_FragColor = vec4(center.rgb / center.a, center.a);

}

