precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D s_texture;
uniform float alpha;

void main() 
{
    //gl_FragColor = vec4(texture2D(s_texture, v_texCoord).xyz, texture2D(s_texture, v_texCoord).w * alpha);
	gl_FragColor = texture2D( s_texture, v_texCoord );
	//gl_FragColor = vec4(gl_FragColor.xyz, gl_FragColor.w * alpha);
	gl_FragColor.a = gl_FragColor.w * alpha;
}