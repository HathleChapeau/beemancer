#version 150

uniform sampler2D DiffuseSampler;
uniform float DarkenAmount;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    fragColor = vec4(color.rgb * DarkenAmount, color.a);
}
