#version 120

uniform sampler2D textureAtlas;
uniform float daylight = 1.0;
uniform int swimming = 0;
uniform float animationOffset = 0;
uniform int animationType = 0;

varying float fog;
varying vec3 normal;

vec4 srgbToLinear(vec4 color){
    return pow(color, vec4(1.0 / 2.2));
}

vec4 linearToSrgb(vec4 color){
    return pow(color, vec4(2.2));
}

void main(){
    vec4 texCoord = gl_TexCoord[0];

    // TEXTURE ANIMATION
    if (animationType == 1) {
        texCoord.x *= 16;
        texCoord.y /= 4;

        texCoord.y += animationOffset;
    }

    vec4 color = texture2D(textureAtlas, vec2(texCoord));
    color = srgbToLinear(color);

    if (color.a < 0.1)
        discard;

    color.rgb *= gl_Color.rgb;
    color.a *= gl_Color.a;

    vec2 lightCoord = vec2(gl_TexCoord[1]);

    float daylightPow = clamp(pow(0.8, (1.0-daylight)*15.0) + 0.4, 0.0, 1.0);
    float daylightValue = daylightPow * lightCoord.x;

    float blocklightValue = lightCoord.y;

    vec3 daylightColorValue = vec3(daylightValue);
    vec3 blocklightColorValue = vec3(blocklightValue);

    blocklightColorValue = clamp(blocklightColorValue,0.0,1.0);
    daylightColorValue = clamp(daylightColorValue, 0.0, 1.0);

    blocklightColorValue.x *= 1.5;
    blocklightColorValue.y *= 1.3;
    blocklightColorValue.z *= 0.6;

    color.xyz *= daylightColorValue + blocklightColorValue * (1.0-daylightValue);

    if (swimming == 0) {
        gl_FragColor.rgb = linearToSrgb(mix(color, vec4(0.9,0.9,0.9,1.0) * daylight, fog)).rgb;
        gl_FragColor.a = color.a;
    } else {
        gl_FragColor.rgb = linearToSrgb(mix(color, vec4(0.0,0.0,0.0,1.0) * daylight, fog*16.0)).rgb;
        gl_FragColor.rg *= 0.4;
        gl_FragColor.b *= 0.7;
        gl_FragColor.a *= color.a;
    }
}