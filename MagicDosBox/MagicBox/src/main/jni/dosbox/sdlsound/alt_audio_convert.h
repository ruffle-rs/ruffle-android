/*
 *  Extended Audio Converter for SDL (Simple DirectMedia Layer)
 *  Copyright (C) 2002  Frank Ranostaj
 *                      Institute of Applied Physik
 *                      Johann Wolfgang Goethe-Universität
 *                      Frankfurt am Main, Germany
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this library; if not, write to the Free
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Frank Ranostaj
 *  ranostaj@stud.uni-frankfurt.de
 *
 * (This code blatantly abducted for SDL_sound. Thanks, Frank! --ryan.)
 */

#ifndef _INCLUDE_AUDIO_CONVERT_H_
#define _INCLUDE_AUDIO_CONVERT_H_

#include "SDL_audio.h"
#define Sound_AI_Loop 0x2
#define _fsize 32

typedef struct{
    Sint16 numerator;
    Sint16 denominator;
} Fraction;

typedef struct{
   Sint16 c[16][4*_fsize];
   Uint8 incr[16];
   Fraction ratio;
   int mask;
} VarFilter;

typedef struct{
   Uint8* buffer;
   int mode;
   VarFilter *filter;
} AdapterC;

typedef int (*Adapter) ( AdapterC Data, int length );

typedef struct{
   VarFilter filter;
   int filter_index;
   Adapter adapter[32];
/* buffer must be len*len_mult(+len_add) big */
   int len_mult;
   int len_add;
   double add;

/* the following elements are provided for compatibility: */
/* the size of the output is approx  len*len_ratio */
   double len_ratio;
   Uint8* buf;             /* input/output buffer */
   int needed;             /* 0 if nothing to be done, 1 otherwise */
   int len;                /* Length of the input */
   int len_cvt;            /* Length of converted audio buffer */
} Sound_AudioCVT;

#define SDL_SOUND_Loop 0x10

#ifndef SNDDECLSPEC
#define SNDDECLSPEC DECLSPEC
#endif

extern SNDDECLSPEC int Sound_AltConvertAudio( Sound_AudioCVT *Data,
    Uint8* buffer, int length, int mode );

extern SNDDECLSPEC int Sound_AltBuildAudioCVT( Sound_AudioCVT *Data,
   SDL_AudioSpec src, SDL_AudioSpec dst );

extern SNDDECLSPEC int Sound_estimateBufferSize( Sound_AudioCVT *Data,
   int length );

#endif /* _INCLUDE_AUDIO_CONVERT_H_ */

