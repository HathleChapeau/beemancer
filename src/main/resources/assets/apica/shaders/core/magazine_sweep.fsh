#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform float SweepPos;
uniform float SweepAngle;
uniform float BandWidth;
uniform vec3 FluidColor;

uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord2;
in vec3 localPos;

out vec4 fragColor;

// Rotation 2D matrix
mat2 rotate2D(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c);
}

// Linear fog calculation
vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (vertexDistance <= fogStart) {
        return inColor;
    }
    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

void main() {
    // Sample base texture
    vec4 texColor = texture(Sampler0, texCoord0);
    if (texColor.a < 0.1) {
        discard;
    }

    // Sample lightmap
    vec4 lightColor = texture(Sampler2, texCoord2);

    // Utiliser les UV pour le sweep (stable peu importe la camera)
    vec2 sweepCoord = texCoord0;
    sweepCoord = rotate2D(SweepAngle) * (sweepCoord - 0.5) + 0.5;

    // Sweep position (0-1, pre-calculated in Java)
    float sweepPos = SweepPos;

    // Band intensity with soft edges using smoothstep
    float distToBand = abs(sweepCoord.y - sweepPos);
    // Handle wraparound for seamless looping
    distToBand = min(distToBand, 1.0 - distToBand);
    float bandIntensity = smoothstep(BandWidth, 0.0, distToBand);

    // Apply fluid color to the band (additive glow)
    vec3 bandGlow = FluidColor * bandIntensity * 0.8;

    // Combine: base texture * vertex color * lightmap + glow
    vec3 baseColor = texColor.rgb * vertexColor.rgb * lightColor.rgb;
    vec3 finalColor = baseColor + bandGlow;

    // Apply fog
    fragColor = linear_fog(vec4(finalColor, texColor.a * vertexColor.a), vertexDistance, FogStart, FogEnd, FogColor);
}
