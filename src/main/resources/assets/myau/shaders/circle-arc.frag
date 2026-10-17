#version 120

#define PI 3.14159265359

uniform float radialSmoothness, radius, borderThickness, progress;
uniform int change;
uniform vec4 color;
uniform vec2 pos;

void main() {
    vec2 st = gl_FragCoord.xy - (pos + radius + borderThickness);
  //  vec2 rp = st * 2. - 1.;

    float circle = sqrt(dot(st,st));

    //Radius minus circle to get just the outline
    float smoothedAlpha = 1.0 - smoothstep(borderThickness, borderThickness + 3., abs(radius-circle));
    vec4 circleColor = vec4(color.rgb, smoothedAlpha * color.a);

    gl_FragColor = mix(vec4(circleColor.rgb, 0.0), circleColor, smoothstep(0., radialSmoothness, change * (atan(st.y,st.x) - (progress-.5) * PI * 2.5)));
}