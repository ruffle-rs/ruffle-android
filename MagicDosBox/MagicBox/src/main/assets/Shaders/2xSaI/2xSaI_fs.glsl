/*
Based on :
http://code.google.com/r/robinyaogo-learing/source/browse/

and guest.r@gmail.com original shader

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

#version 100

#ifdef GL_ES
precision mediump float;
#endif	

varying vec2 v_texCoord;
uniform sampler2D s_texture;

uniform ivec2 p_texSize;

void main()                                            
{
   vec2 UL, UR, DL, DR;
   float u_width=float(p_texSize.x);
   float u_height=float(p_texSize.y);
	
   float dx = pow(u_width, -1.0) * 0.25;
   float dy = pow(u_height, -1.0) * 0.25;
   vec3 dt = vec3(1.0, 1.0, 1.0);
    
   UL =   v_texCoord + vec2(-dx, -dy);
   UR =    v_texCoord + vec2(dx, -dy);
   DL =    v_texCoord + vec2(-dx, dy);
   DR =    v_texCoord + vec2(dx, dy);
   
   vec3 c00 = texture2D(s_texture, UL).xyz;
   vec3 c20 = texture2D(s_texture, UR).xyz;
   vec3 c02 = texture2D(s_texture, DL).xyz;
   vec3 c22 = texture2D(s_texture, DR).xyz;

   float m1=dot(abs(c00-c22),dt)+0.001;
   float m2=dot(abs(c02-c20),dt)+0.001;
  
   gl_FragColor = vec4((m1*(c02+c20)+m2*(c22+c00))/(2.0*(m1+m2)),1.0);
}
 
 
 
 
 
 
 
 
 