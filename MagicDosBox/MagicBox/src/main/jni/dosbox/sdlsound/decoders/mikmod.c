/*
 * SDL_sound -- An abstract sound format decoding API.
 * Copyright (C) 2001  Ryan C. Gordon.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Module player for SDL_sound. This driver handles anything MikMod does, and
 *  is based on SDL_mixer.
 *
 * Please see the file COPYING in the source's root directory.
 *
 *  This file written by Torbjörn Andersson (d91tan@Update.UU.SE)
 */

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#ifdef SOUND_SUPPORTS_MIKMOD

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

#include "mikmod.h"


static int MIKMOD_init(void);
static void MIKMOD_quit(void);
static int MIKMOD_open(Sound_Sample *sample, const char *ext);
static void MIKMOD_close(Sound_Sample *sample);
static Uint32 MIKMOD_read(Sound_Sample *sample);
static int MIKMOD_rewind(Sound_Sample *sample);
static int MIKMOD_seek(Sound_Sample *sample, Uint32 ms);

static const char *extensions_mikmod[] =
{
    "669",   /* Composer 669                                                */
    "AMF",   /* DMP Advanced Module Format                                  */
    "DSM",   /* DSIK internal format                                        */
    "FAR",   /* Farandole module                                            */
    "GDM",   /* General DigiMusic module                                    */
    "IMF",   /* Imago Orpheus module                                        */
    "IT",    /* Impulse tracker                                             */
    "M15",   /* 15 instrument MOD / Ultimate Sound Tracker (old M15 format) */
    "MED",   /* Amiga MED module                                            */
    "MOD",   /* Generic MOD (Protracker, StarTracker, FastTracker, etc)     */
    "MTM",   /* MTM module                                                  */
    "OKT",   /* Oktalyzer module                                            */
    "S3M",   /* Screamtracker module                                        */
    "STM",   /* Screamtracker 2 module                                      */
    "STX",   /* STMIK 0.2 module                                            */
    "ULT",   /* Ultratracker module                                         */
    "UNI",   /* UNIMOD - libmikmod's and APlayer's internal module format   */
    "XM",    /* Fasttracker module                                          */
    NULL
};

const Sound_DecoderFunctions __Sound_DecoderFunctions_MIKMOD =
{
    {
        extensions_mikmod,
        "Play modules through MikMod",
        "Torbjörn Andersson <d91tan@Update.UU.SE>",
        "http://mikmod.raphnet.net/"
    },

    MIKMOD_init,       /*   init() method */
    MIKMOD_quit,       /*   quit() method */
    MIKMOD_open,       /*   open() method */
    MIKMOD_close,      /*  close() method */
    MIKMOD_read,       /*   read() method */
    MIKMOD_rewind,     /* rewind() method */
    MIKMOD_seek        /*   seek() method */
};


/* Make MikMod read from a RWops... */

typedef struct MRWOPSREADER {
    MREADER core;
    Sound_Sample *sample;
    int end;
} MRWOPSREADER;

static BOOL _mm_RWopsReader_eof(MREADER *reader)
{
    MRWOPSREADER *rwops_reader = (MRWOPSREADER *) reader;
    Sound_Sample *sample = rwops_reader->sample;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    int pos = SDL_RWtell(internal->rw);

    if (rwops_reader->end == pos)
        return(1);

    return(0);
} /* _mm_RWopsReader_eof */


static BOOL _mm_RWopsReader_read(MREADER *reader, void *ptr, size_t size)
{
    MRWOPSREADER *rwops_reader = (MRWOPSREADER *) reader;
    Sound_Sample *sample = rwops_reader->sample;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    return(SDL_RWread(internal->rw, ptr, size, 1));
} /* _mm_RWopsReader_Read */


static int _mm_RWopsReader_get(MREADER *reader)
{
    char buf;
    MRWOPSREADER *rwops_reader = (MRWOPSREADER *) reader;
    Sound_Sample *sample = rwops_reader->sample;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;

    if (SDL_RWread(internal->rw, &buf, 1, 1) != 1)
        return(EOF);

    return((int) buf);
} /* _mm_RWopsReader_get */


static BOOL _mm_RWopsReader_seek(MREADER *reader, long offset, int whence)
{
    MRWOPSREADER *rwops_reader = (MRWOPSREADER *) reader;
    Sound_Sample *sample = rwops_reader->sample;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;

    return(SDL_RWseek(internal->rw, offset, whence));
} /* _mm_RWopsReader_seek */


static long _mm_RWopsReader_tell(MREADER *reader)
{
    MRWOPSREADER *rwops_reader = (MRWOPSREADER *) reader;
    Sound_Sample *sample = rwops_reader->sample;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;

    return(SDL_RWtell(internal->rw));
} /* _mm_RWopsReader_tell */


static MREADER *_mm_new_rwops_reader(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;

    MRWOPSREADER *reader = (MRWOPSREADER *) malloc(sizeof (MRWOPSREADER));
    if (reader != NULL)
    {
        int failed_seek = 1;
        int here;
        reader->core.Eof  = _mm_RWopsReader_eof;
        reader->core.Read = _mm_RWopsReader_read;
        reader->core.Get  = _mm_RWopsReader_get;
        reader->core.Seek = _mm_RWopsReader_seek;
        reader->core.Tell = _mm_RWopsReader_tell;
        reader->sample = sample;

        /* RWops does not explicitly support an eof check, so we shall find
           the end manually - this requires seek support for the RWop */
        here = SDL_RWtell(internal->rw);
        if (here != -1)
        {
            reader->end = SDL_RWseek(internal->rw, 0, SEEK_END);
            if (reader->end != -1)
            {
                /* Move back */
                if (SDL_RWseek(internal->rw, here, SEEK_SET) != -1)
                    failed_seek = 0;
            } /* if */
        } /* if */

        if (failed_seek)
        {
            free(reader);
            reader = NULL;
        } /* if */
    } /* if */

    return((MREADER *) reader);
} /* _mm_new_rwops_reader */


static void _mm_delete_rwops_reader(MREADER *reader)
{
        /* SDL_sound will delete the RWops and sample at a higher level... */
    if (reader != NULL)
        free(reader);
} /* _mm_delete_rwops_reader */



static int MIKMOD_init(void)
{
    MikMod_RegisterDriver(&drv_nos);
    
    /* Quick and dirty hack to prevent an infinite loop problem
     * found when using SDL_mixer and SDL_sound together and 
     * both have MikMod compiled in. So, check to see if
     * MikMod has already been registered first before calling
     * RegisterAllLoaders()
     */
    if(MikMod_InfoLoader() == NULL)
    {
        MikMod_RegisterAllLoaders();
    }  
        /*
         * Both DMODE_SOFT_MUSIC and DMODE_16BITS should be set by default,
         * so this is just for clarity. I haven't experimented with any of
         * the other flags. There are a few which are said to give better
         * sound quality.
         */
    md_mode |= (DMODE_SOFT_MUSIC | DMODE_16BITS);
    md_mixfreq = 0;
    md_reverb = 1;

    BAIL_IF_MACRO(MikMod_Init(""), MikMod_strerror(MikMod_errno), 0);

    return(1);  /* success. */
} /* MIKMOD_init */


static void MIKMOD_quit(void)
{
    MikMod_Exit();
    md_mixfreq = 0;
} /* MIKMOD_quit */


static int MIKMOD_open(Sound_Sample *sample, const char *ext)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MREADER *reader;
    MODULE *module;

    reader = _mm_new_rwops_reader(sample);
    BAIL_IF_MACRO(reader == NULL, ERR_OUT_OF_MEMORY, 0);
    module = Player_LoadGeneric(reader, 64, 0);
    _mm_delete_rwops_reader(reader);
    BAIL_IF_MACRO(module == NULL, "MIKMOD: Not a module file.", 0);

    module->extspd  = 1;
    module->panflag = 1;
    module->wrap    = 0;
    module->loop    = 0;

    if (md_mixfreq == 0)
        md_mixfreq = (!sample->desired.rate) ? 44100 : sample->desired.rate;

    sample->actual.channels = 2;
    sample->actual.rate = md_mixfreq;
    sample->actual.format = AUDIO_S16SYS;
    internal->decoder_private = (void *) module;

    Player_Start(module);
    Player_SetPosition(0);

    sample->flags = SOUND_SAMPLEFLAG_NONE;

    SNDDBG(("MIKMOD: Name: %s\n", module->songname));
    SNDDBG(("MIKMOD: Type: %s\n", module->modtype));
    SNDDBG(("MIKMOD: Accepting data stream\n"));

    return(1); /* we'll handle this data. */
} /* MIKMOD_open */


static void MIKMOD_close(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MODULE *module = (MODULE *) internal->decoder_private;

    Player_Free(module);
} /* MIKMOD_close */


static Uint32 MIKMOD_read(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MODULE *module = (MODULE *) internal->decoder_private;

        /* Switch to the current module, stopping any previous one. */
    Player_Start(module);
    if (!Player_Active())
    {
        sample->flags |= SOUND_SAMPLEFLAG_EOF;
        return(0);
    } /* if */
    return((Uint32) VC_WriteBytes(internal->buffer, internal->buffer_size));
} /* MIKMOD_read */


static int MIKMOD_rewind(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MODULE *module = (MODULE *) internal->decoder_private;

    Player_Start(module);
    Player_SetPosition(0);
    return(1);
} /* MIKMOD_rewind */


static int MIKMOD_seek(Sound_Sample *sample, Uint32 ms)
{
#if 0
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MODULE *module = (MODULE *) internal->decoder_private;

        /*
         * Heaven may know what the argument to Player_SetPosition() is.
         * I, however, haven't the faintest idea.
         */
    Player_Start(module);
    Player_SetPosition(ms);
    return(1);
#else
    BAIL_MACRO("MIKMOD: Seeking not implemented", 0);
#endif
} /* MIKMOD_seek */

#endif /* SOUND_SUPPORTS_MIKMOD */


/* end of mikmod.c ... */
