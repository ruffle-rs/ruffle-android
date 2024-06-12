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
 * Speex decoder for SDL_sound.
 *
 * This driver handles Speex audio data. Speex is a codec for speech that is
 *  meant to be transmitted over narrowband network connections. Epic Games
 *  estimates that their VoIP solution, built on top of Speex, uses around
 *  500 bytes per second or less to transmit relatively good sounding speech.
 *
 * This decoder processes the .spx files that the speexenc program produces.
 *
 * Speex isn't meant for general audio compression. Something like Ogg Vorbis
 *  will give better results in that case.
 *
 * Further Speex information can be found at http://www.speex.org/
 *
 * This code is based on speexdec.c (see the Speex website).
 *
 * Please see the file COPYING in the source's root directory.
 *
 *  This file written by Ryan C. Gordon. (icculus@icculus.org)
 */

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#ifdef SOUND_SUPPORTS_SPEEX

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <ogg/ogg.h>
#include <speex/speex.h>
#include <speex/speex_header.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

static int SPEEX_init(void);
static void SPEEX_quit(void);
static int SPEEX_open(Sound_Sample *sample, const char *ext);
static void SPEEX_close(Sound_Sample *sample);
static Uint32 SPEEX_read(Sound_Sample *sample);
static int SPEEX_rewind(Sound_Sample *sample);
static int SPEEX_seek(Sound_Sample *sample, Uint32 ms);

static const char *extensions_speex[] = { "spx", NULL };
const Sound_DecoderFunctions __Sound_DecoderFunctions_SPEEX =
{
    {
        extensions_speex,
        "SPEEX speech compression format",
        "Ryan C. Gordon <icculus@icculus.org>",
        "http://www.icculus.org/SDL_sound/"
    },

    SPEEX_init,       /*   init() method */
    SPEEX_quit,       /*   quit() method */
    SPEEX_open,       /*   open() method */
    SPEEX_close,      /*  close() method */
    SPEEX_read,       /*   read() method */
    SPEEX_rewind,     /* rewind() method */
    SPEEX_seek        /*   seek() method */
};

#define SPEEX_USE_PERCEPTUAL_ENHANCER 1
#define SPEEX_MAGIC  0x5367674F  /* "OggS" in ASCII (littleendian) */
#define SPEEX_OGG_BUFSIZE 200

/* this is what we store in our internal->decoder_private field... */
typedef struct
{
    ogg_sync_state oy;
    ogg_page og;
    ogg_packet op;
    ogg_stream_state os;
    void *state;
    SpeexBits bits;
    int header_count;
    int frame_size;
    int nframes;
    int frames_avail;
    float *decode_buf;
    int decode_total;
    int decode_pos;
    int have_ogg_packet;
} speex_t;


static int SPEEX_init(void)
{
    return(1);   /* no-op. */
} /* SPEEX_init */


static void SPEEX_quit(void)
{
    /* no-op. */
} /* SPEEX_quit */


static int process_header(speex_t *speex, Sound_Sample *sample)
{
    SpeexMode *mode;
    SpeexHeader *hptr;
    SpeexHeader header;
    int enh_enabled = SPEEX_USE_PERCEPTUAL_ENHANCER;
    int tmp;

    hptr = speex_packet_to_header((char*) speex->op.packet, speex->op.bytes);
    BAIL_IF_MACRO(!hptr, "SPEEX: Cannot read header", 0);
    memcpy(&header, hptr, sizeof (SpeexHeader)); /* move to stack. */
    free(hptr);  /* lame that this forces you to malloc... */

    BAIL_IF_MACRO(header.mode >= SPEEX_NB_MODES, "SPEEX: Unknown mode", 0);
    BAIL_IF_MACRO(header.mode < 0, "SPEEX: Unknown mode", 0);
    mode = speex_mode_list[header.mode];
    BAIL_IF_MACRO(header.speex_version_id > 1, "SPEEX: Unknown version", 0);
    BAIL_IF_MACRO(mode->bitstream_version < header.mode_bitstream_version,
                  "SPEEX: Unsupported bitstream version", 0);
    BAIL_IF_MACRO(mode->bitstream_version > header.mode_bitstream_version,
                  "SPEEX: Unsupported bitstream version", 0);

    speex->state = speex_decoder_init(mode);
    BAIL_IF_MACRO(!speex->state, "SPEEX: Decoder initialization error", 0);

    speex_decoder_ctl(speex->state, SPEEX_SET_ENH, &enh_enabled);
    speex_decoder_ctl(speex->state, SPEEX_GET_FRAME_SIZE, &speex->frame_size);

    speex->decode_buf = (float *) malloc(speex->frame_size * sizeof (float));
    BAIL_IF_MACRO(!speex->decode_buf, ERR_OUT_OF_MEMORY, 0);

    speex->nframes = header.frames_per_packet;
    if (!speex->nframes)
        speex->nframes = 1;

    /* !!! FIXME: Write converters to match desired format.
       !!! FIXME:  We have to convert from Float32 anyhow. */
    /* !!! FIXME: Is it a performance hit to alter sampling rate?
       !!! FIXME:  If not, try to match desired rate. */
    /* !!! FIXME: We force mono output, but speexdec.c has code for stereo.
       !!! FIXME:  Use that if sample->desired.channels == 2? */
    tmp = header.rate;
    speex_decoder_ctl(speex->state, SPEEX_SET_SAMPLING_RATE, &tmp);
    speex_decoder_ctl(speex->state, SPEEX_GET_SAMPLING_RATE, &tmp);
    sample->actual.rate = tmp;
    sample->actual.channels = 1;
    sample->actual.format = AUDIO_S16SYS;

    SNDDBG(("SPEEX: %dHz, mono, %svbr, %s mode.\n",
            (int) sample->actual.rate,
            header.vbr ? "" : "not ",
            mode->modeName));

    /* plus 2: one for this header, one for the comment header. */
    speex->header_count = header.extra_headers + 2;
    return(1);
} /* process_header */


/* !!! FIXME: this code sucks. */
static int SPEEX_open(Sound_Sample *sample, const char *ext)
{
    int set_error_str = 1;
    int bitstream_initialized = 0;
    Uint8 *buffer = NULL;
    int packet_count = 0;
    speex_t *speex = NULL;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    SDL_RWops *rw = internal->rw;
    Uint32 magic;

    /* Quick rejection. */
    /*
     * !!! FIXME: If (ext) is .spx, ignore bad magic number and assume
     * !!! FIXME:  this is a corrupted file...try to sync up further in
     * !!! FIXME:  stream. But for general purposes we can't read the
     * !!! FIXME:  whole RWops here in case it's not a Speex file at all.
     */
    magic = SDL_ReadLE32(rw);  /* make sure this is an ogg stream. */
    BAIL_IF_MACRO(magic != SPEEX_MAGIC, "SPEEX: Not a complete ogg stream", 0);
    BAIL_IF_MACRO(SDL_RWseek(rw, -4, SEEK_CUR) < 0, ERR_IO_ERROR, 0);

    speex = (speex_t *) malloc(sizeof (speex_t));
    BAIL_IF_MACRO(speex == NULL, ERR_OUT_OF_MEMORY, 0);
    memset(speex, '\0', sizeof (speex_t));

    speex_bits_init(&speex->bits);
    if (ogg_sync_init(&speex->oy) != 0) goto speex_open_failed;

    while (1)
    {
        int rc;
        Uint8 *buffer = (Uint8*)ogg_sync_buffer(&speex->oy, SPEEX_OGG_BUFSIZE);
        if (buffer == NULL) goto speex_open_failed;
        rc = SDL_RWread(rw, buffer, 1, SPEEX_OGG_BUFSIZE);
        if (rc <= 0) goto speex_open_failed;
        if (ogg_sync_wrote(&speex->oy, rc) != 0) goto speex_open_failed;
        while (ogg_sync_pageout(&speex->oy, &speex->og) == 1)
        {
            if (!bitstream_initialized)
            {
                if (ogg_stream_init(&speex->os, ogg_page_serialno(&speex->og)))
                    goto speex_open_failed;
                bitstream_initialized = 1;
            } /* if */

            if (ogg_stream_pagein(&speex->os, &speex->og) != 0)
                goto speex_open_failed;

            while (ogg_stream_packetout(&speex->os, &speex->op) == 1)
            {
                if (speex->op.e_o_s)
                    goto speex_open_failed;  /* end of stream already?! */

                packet_count++;
                if (packet_count == 1)  /* need speex header. */
                {
                    if (!process_header(speex, sample))
                    {
                        set_error_str = 0; /* process_header will set error string. */
                        goto speex_open_failed;
                    } /* if */
                } /* if */

                if (packet_count > speex->header_count)
                {
                    /* if you made it here, you're ready to get a waveform. */
                    SNDDBG(("SPEEX: Accepting data stream.\n"));

                    /* sample->actual is configured in process_header()... */
                    speex->have_ogg_packet = 1;
                    sample->flags = SOUND_SAMPLEFLAG_NONE;
                    internal->decoder_private = speex;
                    return(1); /* we'll handle this data. */
                } /* if */
            } /* while */

        } /* while */

    } /* while */

    assert(0);  /* shouldn't hit this point. */

speex_open_failed:
    if (speex != NULL)
    {
        if (speex->state != NULL)
            speex_decoder_destroy(speex->state);
        if (bitstream_initialized)
            ogg_stream_clear(&speex->os);
        speex_bits_destroy(&speex->bits);
        ogg_sync_clear(&speex->oy);
        free(speex->decode_buf);
        free(speex);
    } /* if */

    if (set_error_str)
        BAIL_MACRO("SPEEX: decoding error", 0);

    return(0);
} /* SPEEX_open */


static void SPEEX_close(Sound_Sample *sample)
{
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    speex_t *speex = (speex_t *) internal->decoder_private;
    speex_decoder_destroy(speex->state);
    ogg_stream_clear(&speex->os);
    speex_bits_destroy(&speex->bits);
    ogg_sync_clear(&speex->oy);
    free(speex->decode_buf);
    free(speex);
} /* SPEEX_close */


static Uint32 copy_from_decoded(speex_t *speex,
                                Sound_SampleInternal *internal,
                                Uint32 _cpypos)
{
    /*
     * !!! FIXME: Obviously, this all needs to change if we allow for
     * !!! FIXME:  more than mono, S16SYS data.
     */
    Uint32 cpypos = _cpypos >> 1;
    Sint16 *dst = ((Sint16 *) internal->buffer) + cpypos;
    Sint16 *max;
    Uint32 maxoutput = (internal->buffer_size >> 1) - cpypos;
    Uint32 maxavail = speex->decode_total - speex->decode_pos;
    float *src = speex->decode_buf + speex->decode_pos;

    if (maxavail < maxoutput)
        maxoutput = maxavail;

    speex->decode_pos += maxoutput;
    cpypos += maxoutput;

    for (max = dst + maxoutput; dst < max; dst++, src++)
    {
        /* !!! FIXME: This screams for vectorization. */
        register float f = *src;
        if (f > 32000.0f)  /* eh, speexdec.c clamps like this, too. */
            f = 32000.0f;
        else if (f < -32000.0f)
            f = -32000.0f;
        *dst = (Sint16) (0.5f + f);
    } /* for */
    
    return(cpypos << 1);
} /* copy_from_decoded */


/* !!! FIXME: this code sucks. */
static Uint32 SPEEX_read(Sound_Sample *sample)
{
    Uint32 retval = 0;
    Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
    speex_t *speex = (speex_t *) internal->decoder_private;
    SDL_RWops *rw = internal->rw;
    Uint8 *buffer;
    int rc;

    while (1)
    {
        /* see if there's some already-decoded leftovers... */
        if (speex->decode_total != speex->decode_pos)
        {
            retval = copy_from_decoded(speex, internal, retval);
            if (retval >= internal->buffer_size)
                return(retval);  /* whee. */
        } /* if */

        /* okay, decoded buffer is spent. What else do we have? */
        speex->decode_total = speex->decode_pos = 0;

        if (speex->frames_avail) /* have more frames to decode? */
        {
            rc = speex_decode(speex->state, &speex->bits, speex->decode_buf);
            if (rc < 0) goto speex_read_failed;
            if (speex_bits_remaining(&speex->bits) < 0) goto speex_read_failed;
            speex->frames_avail--;
            speex->decode_total = speex->frame_size;
            continue;  /* go fill the output buffer... */
        } /* if */

        /* need to get more speex frames from available ogg packets... */
        if (speex->have_ogg_packet)
        {
            speex_bits_read_from(&speex->bits,
                                 (char *) speex->op.packet,
                                 speex->op.bytes);

            speex->frames_avail += speex->nframes;
            if (ogg_stream_packetout(&speex->os, &speex->op) <= 0)
                speex->have_ogg_packet = 0;
            continue;  /* go decode these frames. */
        } /* if */

        /* need to get more ogg packets from bitstream... */

        if (speex->op.e_o_s)   /* okay, we're really spent. */
        {
            sample->flags |= SOUND_SAMPLEFLAG_EOF;
            return(retval);
        } /* if */

        while ((!speex->op.e_o_s) && (!speex->have_ogg_packet))
        {
            buffer = (Uint8 *) ogg_sync_buffer(&speex->oy, SPEEX_OGG_BUFSIZE);
            if (buffer == NULL) goto speex_read_failed;
            rc = SDL_RWread(rw, buffer, 1, SPEEX_OGG_BUFSIZE);
            if (rc <= 0) goto speex_read_failed;
            if (ogg_sync_wrote(&speex->oy, rc) != 0) goto speex_read_failed;

            /* got complete ogg page? */
            if (ogg_sync_pageout(&speex->oy, &speex->og) == 1)
            {
                if (ogg_stream_pagein(&speex->os, &speex->og) != 0)
                    goto speex_read_failed;

                /* got complete ogg packet? */
                if (ogg_stream_packetout(&speex->os, &speex->op) == 1)
                    speex->have_ogg_packet = 1;
            } /* if */
        } /* while */
    } /* while */

    assert(0);  /* never hit this. Either return or goto speex_read_failed */

speex_read_failed:
    sample->flags |= SOUND_SAMPLEFLAG_ERROR;
    /* !!! FIXME: "i/o error" is better in some situations. */
    BAIL_MACRO("SPEEX: Decoding error", retval);
} /* SPEEX_read */


static int SPEEX_rewind(Sound_Sample *sample)
{
    /* !!! FIXME */ return(0);
} /* SPEEX_rewind */


static int SPEEX_seek(Sound_Sample *sample, Uint32 ms)
{
    /* !!! FIXME */ return(0);
} /* SPEEX_seek */


#endif /* SOUND_SUPPORTS_SPEEX */

/* end of speex.c ... */

