#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MaskSampler;
uniform float DarkenAmount;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    vec4 mask = texture(MaskSampler, texCoord);

    // Ou le masque a des pixels (silhouette entite), on garde la luminosite originale.
    // Partout ailleurs, on assombrit.
    float maskStrength = clamp(mask.a, 0.0, 1.0);
    float brightness = mix(DarkenAmount, 1.0, maskStrength);

    fragColor = vec4(color.rgb * brightness, color.a);
}
