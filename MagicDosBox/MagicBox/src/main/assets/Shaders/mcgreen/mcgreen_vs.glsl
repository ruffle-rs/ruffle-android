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
attribute vec4 vPosition;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;

void main() 
{
	gl_Position = uMVPMatrix * vPosition;
	v_texCoord = a_texCoord;
}