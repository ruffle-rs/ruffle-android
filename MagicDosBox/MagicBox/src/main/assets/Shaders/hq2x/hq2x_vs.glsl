/*
Author :
http://code.google.com/r/robinyaogo-learing/source/browse/

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

uniform mat4 uMVPMatrix;
uniform ivec2 p_texSize;

attribute vec4 vPosition;
attribute vec2 a_texCoord;

varying vec4 TexCoord[5];

void main() 
{
   gl_Position = uMVPMatrix * vPosition;
	
   vec2 o = 0.5 * (1.0 / vec2(p_texSize)); 		
   vec2 dg1 = vec2( o.x, o.y);
   vec2 dg2 = vec2(-o.x, o.y); 
   vec2 dx = vec2(o.x, 0.0);
   vec2 dy = vec2(0.0, o.y);
   
   TexCoord[0] = vec4(a_texCoord, 0.0, 0.0);
   TexCoord[1].xy = TexCoord[0].xy - dg1;
   TexCoord[1].zw = TexCoord[0].xy - dy;
   TexCoord[2].xy = TexCoord[0].xy - dg2;
   TexCoord[2].zw = TexCoord[0].xy + dx;
   TexCoord[3].xy = TexCoord[0].xy + dg1;
   TexCoord[3].zw = TexCoord[0].xy + dy;
   TexCoord[4].xy = TexCoord[0].xy + dg2;
   TexCoord[4].zw = TexCoord[0].xy - dx;
}