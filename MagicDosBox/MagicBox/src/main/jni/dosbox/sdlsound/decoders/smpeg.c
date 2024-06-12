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
 * MPEG-1 Layer 3, or simply, "MP3", decoder for SDL_sound.
 *
 * This driver handles all those highly compressed songs you stole through
 *  Napster.  :)  It depends on the SMPEG library for decoding, which can
 *  be grabbed from: http://www.lokigames.com/development/smpeg.php3
 *
 * This should also be able to extract the audio stream from an MPEG movie.
 *
 * There is an alternative MP3 decoder available, called "mpglib", which
 *  doesn't depend on external libraries (the decoder itself is part of
 *  SDL_sound), and may be more efficient, but less flexible than SMPEG. YMMV.
 *
 * Please see the file COPYING in the source's root directory.
 *
 *  This file written by Ryan C. Gordon. (icculus@icculus.org)
 */

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#ifdef SOUND_SUPPORTS_SMPEG

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

#include "smpeg.h"
#include "extra_rwops.h"


static int _SMPEG_init(void);
static void _SMPEG_quit(void);
static int _SMPEG_open(Sound_Sample *sample, const char *ext);
static void _SMPEG_close(Sound_Sample *sample);
static Uint32 _SMPEG_read(Sound_Sample *sample);
static int _SMPEG_rewind(Sound_Sample *sample);
static int _SMPEG_seek(Sound_Sample *sample, Uint32 ms);

static const char *extensions_smpeg[] = { "MP3", "MPEG", "MPG", NULL };
const Sound_DecoderFunctions __Sound_DecoderFunctions_SMPEG =
{
    {
        extensions_smpeg,
        "MPEG-1 Layer 3 audio through SMPEG",
        "Ryan C. Gordon <icculus@icculus.org>",
        "http://icculus.org/smpeg/"
    },

    _SMPEG_init,       /*   init() method */
    _SMPEG_quit,       /*   quit() method */
    _SMPEG_open,       /*   open() method */
    _SMPEG_close,      /*  close() method */
    _SMPEG_read,       /*   read() method */
    _SMPEG_rewind,     /* rewind() method */
    _SMPEG_seek        /*   seek() method */
};


static int _SMPEG_init(void)
{
    return(1);  /* always succeeds. */
} /* _SMPEG_init */


static void _SMPEG_quit(void)
{
    /* it's a no-op. */
} /* _SMPEG_quit */


static __inline__ void output_version(void)
{
    static int first_time = 1;

    if (first_time)
    {
        SMPEG_version v;
        SMPEG_VERSION(&v);
        SNDDBG(("SMPEG: Compiled against SMPEG v%d.%d.%d.\n",
                    v.major, v.minor, v.patch));
        first_time = 0;
    } /* if */
} /* output_version */


static int _SMPEG_open(Sound_Sample *sample, const char *ext)
{
    SMPEG *smpeg;
    SMPEG_Info smpeg_info;
    SDL_AudioSpec spec;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SDL_RWops *refCounter;
    const char *err = NULL;

    output_version();

       /*
        * If I understand things correctly, MP3 files don't really have any
        * magic header we can check for. The MP3 player is expected to just
        * pick the first thing that looks like a valid frame and start
        * playing from there.
        *
        * So here's what we do: If the caller insists that this is really
        * MP3 we'll take his word for it. Otherwise, use the same test as
        * SDL_mixer does and check if the stream starts with something that
        * looks like a frame.
        *
        * A frame begins with 11 bits of frame sync (all bits must be set),
        * followed by a two-bit MPEG Audio version ID:
        *
        *   00 - MPEG Version 2.5 (later extension of MPEG 2)
        *   01 - reserved
        *   10 - MPEG Version 2 (ISO/IEC 13818-3)
        *   11 - MPEG Version 1 (ISO/IEC 11172-3)
        *
        * Apparently we don't handle MPEG Version 2.5.
        */
    if (__Sound_strcasecmp(ext, "MP3") != 0)
    {
        Uint8 mp3_magic[2];

        if (SDL_RWread(internal->rw, mp3_magic, sizeof (mp3_magic), 1) != 1)
            BAIL_MACRO("SMPEG: Could not read MP3 magic.", 0);

        if (mp3_magic[0] != 0xFF || (mp3_magic[1] & 0xF0) != 0xF0)
            BAIL_MACRO("SMPEG: Not an MP3 stream.", 0);

            /* If the seek fails, we'll probably miss a frame, but oh well */
        SDL_RWseek(internal->rw, -sizeof (mp3_magic), SEEK_CUR);
    } /* if */

    refCounter = RWops_RWRefCounter_new(internal->rw);
    if (refCounter == NULL)
    {
        SNDDBG(("SMPEG: Failed to create reference counting RWops.\n"));
        return(0);
    } /* if */

    /* replace original RWops. This is safe. Honest. :) */
    internal->rw = refCounter;

    /*
     * increment the refcount, since SMPEG will nuke the RWops if it can't
     *  accept the contained data...
     */
    RWops_RWRefCounter_addRef(refCounter);
    smpeg = SMPEG_new_rwops(refCounter, &smpeg_info, 0);

    err = SMPEG_error(smpeg);
    if (err != NULL)
    {
        __Sound_SetError(err);  /* make a copy before SMPEG_delete()... */
        SMPEG_delete(smpeg);
        return(0);
    } /* if */

    if (!smpeg_info.has_audio)
    {
        SMPEG_delete(smpeg);
        BAIL_MACRO("SMPEG: No audio stream found in data.", 0);
    } /* if */

    SNDDBG(("SMPEG: Accepting data stream.\n"));
    SNDDBG(("SMPEG: has_audio == {%s}.\n", smpeg_info.has_audio ? "TRUE" : "FALSE"));
    SNDDBG(("SMPEG: has_video == {%s}.\n", smpeg_info.has_video ? "TRUE" : "FALSE"));
    SNDDBG(("SMPEG: width == (%d).\n", smpeg_info.width));
    SNDDBG(("SMPEG: height == (%d).\n", smpeg_info.height));
    SNDDBG(("SMPEG: current_frame == (%d).\n", smpeg_info.current_frame));
    SNDDBG(("SMPEG: current_fps == (%f).\n", smpeg_info.current_fps));
    SNDDBG(("SMPEG: audio_string == [%s].\n", smpeg_info.audio_string));
    SNDDBG(("SMPEG: audio_current_frame == (%d).\n", smpeg_info.audio_current_frame));
    SNDDBG(("SMPEG: current_offset == (%d).\n", smpeg_info.current_offset));
    SNDDBG(("SMPEG: total_size == (%d).\n", smpeg_info.total_size));
    SNDDBG(("SMPEG: current_time == (%f).\n", smpeg_info.current_time));
    SNDDBG(("SMPEG: total_time == (%f).\n", smpeg_info.total_time));

    SMPEG_enablevideo(smpeg, 0);
    SMPEG_enableaudio(smpeg, 1);
    SMPEG_loop(smpeg, 0);

    SMPEG_wantedSpec(smpeg, &spec);

        /*
         * One of the MP3s I tried wouldn't work unless I added this line
         * to tell SMPEG that yes, it may have the spec it wants.
         */
    SMPEG_actualSpec(smpeg, &spec);
    sample->actual.format = spec.format;
    sample->actual.rate = spec.freq;
    sample->actual.channels = spec.channels;
    sample->flags = SOUND_SAMPLEFLAG_CANSEEK;
    internal->decoder_private = smpeg;

    SMPEG_play(smpeg);
    return(1);
} /* _SMPEG_open */


static void _SMPEG_close(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SMPEG_delete((SMPEG *) internal->decoder_private);
} /* _SMPEG_close */


static Uint32 _SMPEG_read(Sound_Sample *sample)
{
    Uint32 retval;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SMPEG *smpeg = (SMPEG *) internal->decoder_private;

        /*
         * We have to clear the buffer because apparently SMPEG_playAudio()
         * will mix the decoded audio with whatever's already in it. Nasty.
         */
    memset(internal->buffer, '\0', internal->buffer_size);
    retval = SMPEG_playAudio(smpeg, internal->buffer, internal->buffer_size);
    if (retval < internal->buffer_size)
    {
        char *errMsg = SMPEG_error(smpeg);
        if (errMsg == NULL)
            sample->flags |= SOUND_SAMPLEFLAG_EOF;
        else
        {
            __Sound_SetError(errMsg);
            sample->flags |= SOUND_SAMPLEFLAG_ERROR;
        } /* else */
    } /* if */

    return(retval);
} /* _SMPEG_read */


static int _SMPEG_rewind(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SMPEG *smpeg = (SMPEG *) internal->decoder_private;
    SMPEGstatus status;

        /*
         * SMPEG_rewind() really means "stop and rewind", so we may have to
         * restart it afterwards.
         */
    status = SMPEG_status(smpeg);
    SMPEG_rewind(smpeg);
    /* EW: I think SMPEG_play() has an independent and unrelated meaning
     * to the flag, "SMPEG_PLAYING". This is why the SMPEG_play() call
     * is done in the open() function even though the file is not yet
     * technically playing. I believe SMPEG_play() must always be active
     * because this seems to be what's causing the:
     * "Can't rewind after the file has finished playing once" problem,
     * because always recalling it here seems to make the problem go away.
     */
    /*
    if (status == SMPEG_PLAYING)
        SMPEG_play(smpeg);
    */
    SMPEG_play(smpeg);
    return(1);
} /* _SMPEG_rewind */


static int _SMPEG_seek(Sound_Sample *sample, Uint32 ms)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SMPEG *smpeg = (SMPEG *) internal->decoder_private;
    SMPEGstatus status;

        /*
         * SMPEG_rewind() really means "stop and rewind", so we may have to
         * restart it afterwards.
         */
    status = SMPEG_status(smpeg);
    SMPEG_rewind(smpeg);
    SMPEG_skip(smpeg, ((float) ms) / 1000.0);
    if (status == SMPEG_PLAYING)
        SMPEG_play(smpeg);
    return(1);
} /* _SMPEG_seek */

#endif /* SOUND_SUPPORTS_SMPEG */

/* end of smpeg.c ... */

