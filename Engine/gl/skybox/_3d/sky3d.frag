#version 150

in vec4 pass_textureCoords;
in vec3 pass_normals;

uniform sampler2D sampler;
uniform sampler2D lightmap;
uniform vec3 lightDirection;

out vec4 out_color;
const float lightMin = 0.1;
const float lightScale = 0.9;

void main(void){
	vec4 color, light;
	color = texture(sampler, pass_textureCoords.xy);
	light = texture(lightmap, pass_textureCoords.zw);
	
	if (color.a == 0.0) {
		discard;
	}
	
	color.a = 1.0;
	out_color = color * (light + lightMin) * lightScale;
	
}
