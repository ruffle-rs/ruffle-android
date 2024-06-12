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
 * MIDI decoder for SDL_sound.
 *
 * This driver handles MIDI data through a stripped-down version of TiMidity.
 *  See the documentation in the timidity subdirectory.
 *
 * Please see the file COPYING in the source's root directory.
 *
 *  This file written by Torbjörn Andersson. (d91tan@Update.UU.SE)
 */

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#ifdef SOUND_SUPPORTS_MIDI

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

#include "timidity.h"


static int MIDI_init(void);
static void MIDI_quit(void);
static int MIDI_open(Sound_Sample *sample, const char *ext);
static void MIDI_close(Sound_Sample *sample);
static Uint32 MIDI_read(Sound_Sample *sample);
static int MIDI_rewind(Sound_Sample *sample);
static int MIDI_seek(Sound_Sample *sample, Uint32 ms);

static const char *extensions_midi[] = { "MIDI", "MID", NULL };
const Sound_DecoderFunctions __Sound_DecoderFunctions_MIDI =
{
    {
        extensions_midi,
        "MIDI decoder, using a subset of TiMidity",
        "Torbjörn Andersson <d91tan@Update.UU.SE>",
        "http://www.icculus.org/SDL_sound/"
    },

    MIDI_init,       /*   init() method */
    MIDI_quit,       /*   quit() method */
    MIDI_open,       /*   open() method */
    MIDI_close,      /*  close() method */
    MIDI_read,       /*   read() method */
    MIDI_rewind,     /* rewind() method */
    MIDI_seek        /*   seek() method */
};


static int MIDI_init(void)
{
    BAIL_IF_MACRO(Timidity_Init() < 0, "MIDI: Could not initialise", 0);
    return(1);
} /* MIDI_init */


static void MIDI_quit(void)
{
    Timidity_Exit();
} /* MIDI_quit */


static int MIDI_open(Sound_Sample *sample, const char *ext)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SDL_RWops *rw = internal->rw;
    SDL_AudioSpec spec;
    MidiSong *song;

    spec.channels = 2;
    spec.format = AUDIO_S16SYS;
    spec.freq = 44100;
    spec.samples = 4096;
    
    song = Timidity_LoadSong(rw, &spec);
    BAIL_IF_MACRO(song == NULL, "MIDI: Not a MIDI file.", 0);
    Timidity_SetVolume(song, 100);
    Timidity_Start(song);

    SNDDBG(("MIDI: Accepting data stream.\n"));

    internal->decoder_private = (void *) song;

    sample->actual.channels = 2;
    sample->actual.rate = 44100;
    sample->actual.format = AUDIO_S16SYS;
    
    sample->flags = SOUND_SAMPLEFLAG_CANSEEK;
    return(1); /* we'll handle this data. */
} /* MIDI_open */


static void MIDI_close(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MidiSong *song = (MidiSong *) internal->decoder_private;

    Timidity_FreeSong(song);
} /* MIDI_close */


static Uint32 MIDI_read(Sound_Sample *sample)
{
    Uint32 retval;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MidiSong *song = (MidiSong *) internal->decoder_private;

    retval = Timidity_PlaySome(song, internal->buffer, internal->buffer_size);

        /* Make sure the read went smoothly... */
    if (retval == 0)
        sample->flags |= SOUND_SAMPLEFLAG_EOF;

    else if (retval == -1)
        sample->flags |= SOUND_SAMPLEFLAG_ERROR;

        /* (next call this EAGAIN may turn into an EOF or error.) */
    else if (retval < internal->buffer_size)
        sample->flags |= SOUND_SAMPLEFLAG_EAGAIN;

    return(retval);
} /* MIDI_read */


static int MIDI_rewind(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MidiSong *song = (MidiSong *) internal->decoder_private;
    
    Timidity_Start(song);
    return(1);
} /* MIDI_rewind */


static int MIDI_seek(Sound_Sample *sample, Uint32 ms)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    MidiSong *song = (MidiSong *) internal->decoder_private;

    Timidity_Seek(song, ms);
    return(1);
} /* MIDI_seek */

#endif /* SOUND_SUPPORTS_MIDI */


/* end of midi.c ... */

