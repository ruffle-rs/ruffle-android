/*
   Based on :
   Copyright (C) 2006 guest(r) - guest.r@gmail.com

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

#version 100

#ifdef GL_ES
precision lowp float;
#endif	

varying vec2 v_texCoord;
uniform sampler2D s_texture;

 // number of colors the display can show
const float colors = 256.0;

void main()                                            
{
	vec3 ink = vec3(0.55, 0.41, 0.0);  
	vec3 c11 = texture2D(s_texture, v_texCoord.xy).xyz;
	float lct = floor(colors*length(c11))/colors;

	gl_FragColor = vec4(lct*ink,1);
}
