/* 

    TiMidity -- Experimental MIDI to WAVE converter
    Copyright (C) 1995 Tuukka Toivonen <toivonen@clinet.fi>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

    output.c
    
    Audio output (to file / device) functions.
*/

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

#include "options.h"
#include "output.h"

/*****************************************************************/
/* Some functions to convert signed 32-bit data to other formats */

void s32tos8(void *dp, Sint32 *lp, Sint32 c)
{
  Sint8 *cp=(Sint8 *)(dp);
  Sint32 l;
  while (c--)
    {
      l=(*lp++)>>(32-8-GUARD_BITS);
      if (l>127) l=127;
      else if (l<-128) l=-128;
      *cp++ = (Sint8) (l);
    }
}

void s32tou8(void *dp, Sint32 *lp, Sint32 c)
{
  Uint8 *cp=(Uint8 *)(dp);
  Sint32 l;
  while (c--)
    {
      l=(*lp++)>>(32-8-GUARD_BITS);
      if (l>127) l=127;
      else if (l<-128) l=-128;
      *cp++ = 0x80 ^ ((Uint8) l);
    }
}

void s32tos16(void *dp, Sint32 *lp, Sint32 c)
{
  Sint16 *sp=(Sint16 *)(dp);
  Sint32 l;
  while (c--)
    {
      l=(*lp++)>>(32-16-GUARD_BITS);
      if (l > 32767) l=32767;
      else if (l<-32768) l=-32768;
      *sp++ = (Sint16)(l);
    }
}

void s32tou16(void *dp, Sint32 *lp, Sint32 c)
{
  Uint16 *sp=(Uint16 *)(dp);
  Sint32 l;
  while (c--)
    {
      l=(*lp++)>>(32-16-GUARD_BITS);
      if (l > 32767) l=32767;
      else if (l<-32768) l=-32768;
      *sp++ = 0x8000 ^ (Uint16)(l);
    }
}

void s32tos16x(void *dp, Sint32 *lp, Sint32 c)
{
  Sint16 *sp=(Sint16 *)(dp);
  Sint32 l;
  while (c--)
    {
      l=(*lp++)>>(32-16-GUARD_BITS);
      if (l > 32767) l=32767;
      else if (l<-32768) l=-32768;
      *sp++ = SDL_Swap16((Sint16)(l));
    }
}

void s32tou16x(void *dp, Sint32 *lp, Sint32 c)
{
  Uint16 *sp=(Uint16 *)(dp);
  Sint32 l;
  while (c--)
    {
      l=(*lp++)>>(32-16-GUARD_BITS);
      if (l > 32767) l=32767;
      else if (l<-32768) l=-32768;
      *sp++ = SDL_Swap16(0x8000 ^ (Uint16)(l));
    }
}
