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
 * QuickTime decoder for sound formats that QuickTime supports.
 * April 28, 2002
 *
 * This driver handles .mov files with a sound track. In
 * theory, it could handle any format that QuickTime supports.
 * In practice, it may only handle a select few of these formats.
 *
 * It seems able to play back AIFF and other standard Mac formats.
 * Rewinding is not supported yet.
 *
 * The routine QT_create_data_ref() needs to be
 * tweaked to support different media types. 
 * This code was originally written to get MP3 support,
 * as it turns out, this isn't possible using this method.
 *
 * The only way to get streaming MP3 support through QuickTime,
 * and hence support for SDL_RWops, is to write
 * a DataHandler component, which suddenly gets much more difficult :-(
 *
 *  This file was written by Darrell Walisser (walisser@mac.com)
 *  Portions have been borrowed from the "MP3Player" sample code,
 *  courtesy of Apple.
 */

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#ifdef SOUND_SUPPORTS_QUICKTIME
#ifdef macintosh
typedef long int32_t;
#  define OPAQUE_UPP_TYPES 0
#  include <QuickTime.h>
#else
#  include <QuickTime/QuickTime.h>
#  include <Carbon/Carbon.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

static int QT_init(void);
static void QT_quit(void);
static int QT_open(Sound_Sample *sample, const char *ext);
static void QT_close(Sound_Sample *sample);
static Uint32 QT_read(Sound_Sample *sample);
static int QT_rewind(Sound_Sample *sample);
static int QT_seek(Sound_Sample *sample, Uint32 ms);

#define QT_MAX_INPUT_BUFFER (32*1024) /* Maximum size of internal buffer (internal->buffer_size) */

static const char *extensions_quicktime[] = { "mov", NULL };
const Sound_DecoderFunctions __Sound_DecoderFunctions_QuickTime =
  {
    {
      extensions_quicktime,
      "QuickTime format",
      "Darrell Walisser <dwaliss1@purdue.edu>",
      "http://www.icculus.org/SDL_sound/"
    },
    
    QT_init,       /* init() method   */
    QT_quit,       /* quit() method   */
    QT_open,       /* open() method   */
    QT_close,      /* close() method  */
    QT_read,       /* read() method   */
    QT_rewind,     /* rewind() method */
    QT_seek        /* seek() method   */
  };

typedef struct {

  ExtendedSoundComponentData	compData;
  Handle                        hSource;           /* source media buffer */
  Media                         sourceMedia;       /* sound media identifier */
  TimeValue                     getMediaAtThisTime;
  TimeValue                     sourceDuration;
  Boolean                       isThereMoreSource;
  UInt32                        maxBufferSize;

} SCFillBufferData, *SCFillBufferDataPtr;

typedef struct {

  Movie              movie;
  Track              track;
  Media              media;
  AudioFormatAtomPtr atom;
  SoundComponentData source_format;
  SoundComponentData dest_format;
  SoundConverter     converter;
  SCFillBufferData   buffer_data;
  SoundConverterFillBufferDataUPP fill_buffer_proc;

} qt_t;




/*
 * This procedure creates a description of the raw data
 * read from SDL_RWops so that QuickTime can identify
 * the codec it needs to use to decompress it.
 */
static Handle QT_create_data_ref (const char *file_extension) {

  Handle    tmp_handle, data_ref;
  StringPtr file_name = "\p"; /* empty since we don't know the file name! */
  OSType    file_type;
  StringPtr mime_type;
  long      atoms[3];

/*
  if (__Sound_strcasecmp (file_extension, "mp3")==0) {
    file_type = 'MPEG';
    mime_type = "\pvideo/mpeg";
  }
  else {
  
    return NULL;
  }
*/

  if (__Sound_strcasecmp (file_extension, "mov") == 0) {
  
  	file_type = 'MooV';
  	mime_type = "\pvideo/quicktime";
  }
  else {
  
  	return NULL;
  }

  tmp_handle = NewHandle(0);
  assert (tmp_handle != NULL);
  assert (noErr == PtrToHand (&tmp_handle, &data_ref, sizeof(Handle)));
  assert (noErr == PtrAndHand (file_name, data_ref, file_name[0]+1));
  
  atoms[0] = EndianU32_NtoB (sizeof(long) * 3);
  atoms[1] = EndianU32_NtoB (kDataRefExtensionMacOSFileType);
  atoms[2] = EndianU32_NtoB (file_type);

  assert (noErr == PtrAndHand (atoms, data_ref, sizeof(long)*3));

  atoms[0] = EndianU32_NtoB (sizeof(long)*2 + mime_type[0]+1);
  atoms[1] = EndianU32_NtoB (kDataRefExtensionMIMEType);
  
  assert (noErr == PtrAndHand (atoms, data_ref, sizeof(long)*2));
  assert (noErr == PtrAndHand (mime_type, data_ref, mime_type[0]+1));

  return data_ref;
}

/*
 * This procedure is a hook for QuickTime to grab data from the
 * SDL_RWOps data structure when it needs it
 */
static pascal OSErr QT_get_movie_data_proc (long offset, long size, 
                                            void *data, void *user_data)
{
  SDL_RWops* rw = (SDL_RWops*)user_data;
  OSErr error;
	
  if (offset == SDL_RWseek (rw, offset, SEEK_SET)) {
	  
    if (size == SDL_RWread (rw, data, 1, size)) {
      error = noErr;
    }
    else {
      error = notEnoughDataErr;
    }
  }
  else {
    error = fileOffsetTooBigErr;
  }
  
  return (error);
}

/* * ----------------------------
 * SoundConverterFillBufferDataProc
 *
 * the callback routine that provides the source data for conversion -
 * it provides data by setting outData to a pointer to a properly
 * filled out ExtendedSoundComponentData structure
 */
static pascal Boolean QT_sound_converter_fill_buffer_data_proc (SoundComponentDataPtr *outData, void *inRefCon)
{
  SCFillBufferDataPtr pFillData = (SCFillBufferDataPtr)inRefCon;
	
  OSErr err = noErr;
							
  /* if after getting the last chunk of data the total time is over
   * the duration, we're done
   */
  if (pFillData->getMediaAtThisTime >= pFillData->sourceDuration) {
    pFillData->isThereMoreSource = false;
    pFillData->compData.desc.buffer = NULL;
    pFillData->compData.desc.sampleCount = 0;
    pFillData->compData.bufferSize = 0;
  }
	
  if (pFillData->isThereMoreSource) {
	
    long		sourceBytesReturned;
    long		numberOfSamples;
    TimeValue	sourceReturnedTime, durationPerSample;
							
    HUnlock(pFillData->hSource);

    err = GetMediaSample
      (pFillData->sourceMedia,/* specifies the media for this operation */
       pFillData->hSource,    /* function returns the sample data into this handle */
       pFillData->maxBufferSize, /* maximum number of bytes of sample data to be returned */
       &sourceBytesReturned,  /* the number of bytes of sample data returned */
       pFillData->getMediaAtThisTime,/* starting time of the sample to
					be retrieved (must be in
					Media's TimeScale) */
       &sourceReturnedTime,/* indicates the actual time of the returned sample data */
       &durationPerSample, /* duration of each sample in the media */
       NULL, /* sample description corresponding to the returned sample data (NULL to ignore) */
       NULL, /* index value to the sample description that corresponds
		to the returned sample data (NULL to ignore) */
       0, /* maximum number of samples to be returned (0 to use a
	     value that is appropriate for the media) */
       &numberOfSamples, /* number of samples it actually returned */
       NULL);		 /* flags that describe the sample (NULL to ignore) */
							 
    HLock(pFillData->hSource);

    if ((noErr != err) || (sourceBytesReturned == 0)) {
      pFillData->isThereMoreSource = false;
      pFillData->compData.desc.buffer = NULL;
      pFillData->compData.desc.sampleCount = 0;		
      
      if ((err != noErr) && (sourceBytesReturned > 0))
	DebugStr("\pGetMediaSample - Failed in FillBufferDataProc");
    }
    
    pFillData->getMediaAtThisTime = sourceReturnedTime + (durationPerSample * numberOfSamples);
    pFillData->compData.bufferSize = sourceBytesReturned; 
  }
  
  /* set outData to a properly filled out ExtendedSoundComponentData struct */
  *outData = (SoundComponentDataPtr)&pFillData->compData;
	
  return (pFillData->isThereMoreSource);
}
    

static int QT_init_internal () {

  OSErr error;
	
  error = EnterMovies(); /* initialize the movie toolbox */
	
  return (error == noErr);
}

static void QT_quit_internal () {

  ExitMovies();
}

static qt_t* QT_open_internal (Sound_Sample *sample, const char *extension)
{
  Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
	
  qt_t              *instance;
  OSErr              error;
  Movie              movie;
  Track              sound_track;
  Media              sound_track_media;
  AudioFormatAtomPtr source_sound_decomp_atom;
		
  SoundDescriptionV1Handle source_sound_description;
  Handle source_sound_description_extension;
  Size   source_sound_description_extension_size;
  Handle data_ref;

  data_ref = QT_create_data_ref (extension);

  /* create a movie that will read data using SDL_RWops */
  error = NewMovieFromUserProc 
    (&movie, 
     0, 
     NULL, 
     NewGetMovieUPP(QT_get_movie_data_proc),
     (void*) internal->rw,
     data_ref,
     'hndl');
	                      
  if (error != noErr) {
	
    return NULL;
  }
	
  /* get the first sound track of the movie; other tracks will be ignored */
  sound_track = GetMovieIndTrackType (movie, 1, SoundMediaType, movieTrackMediaType);
  if (sound_track == NULL) {
	
    /* movie needs a sound track! */
		
    return NULL;
  }

  /* get and return the sound track media */
  sound_track_media = GetTrackMedia (sound_track);
  if (sound_track_media == NULL) {
	
    return NULL;
  }
	
  /* create a description of the source sound so we can convert it later */
  source_sound_description = (SoundDescriptionV1Handle)NewHandle(0);
  assert (source_sound_description != NULL); /* out of memory */
	
  GetMediaSampleDescription (sound_track_media, 1, 
			     (SampleDescriptionHandle)source_sound_description);
  error = GetMoviesError();
  if (error != noErr) {
	
    return NULL;
  }
	
  source_sound_description_extension = NewHandle(0);
  assert (source_sound_description_extension != NULL); /* out of memory */
	
  error = GetSoundDescriptionExtension ((SoundDescriptionHandle) source_sound_description, 
					&source_sound_description_extension, 
					siDecompressionParams);
	
  if (error == noErr) {
	
    /* copy extension to atom format description if we have an extension */

    source_sound_description_extension_size = 
      GetHandleSize (source_sound_description_extension);
    HLock (source_sound_description_extension);

    source_sound_decomp_atom = (AudioFormatAtom*) 
      NewPtr (source_sound_description_extension_size);
    assert (source_sound_decomp_atom != NULL); /* out of memory */

    BlockMoveData (*source_sound_description_extension, 
		   source_sound_decomp_atom,
		   source_sound_description_extension_size);
		               
    HUnlock (source_sound_description_extension);
  }
	
  else {
	
    source_sound_decomp_atom = NULL;
  }

  instance = (qt_t*) malloc (sizeof(*instance));
  assert (instance != NULL); /* out of memory */
		
  instance->movie = movie;
  instance->track = sound_track;
  instance->media = sound_track_media;
  instance->atom  = source_sound_decomp_atom;
	
  instance->source_format.flags = 0;
  instance->source_format.format = (*source_sound_description)->desc.dataFormat;
  instance->source_format.numChannels = (*source_sound_description)->desc.numChannels;
  instance->source_format.sampleSize = (*source_sound_description)->desc.sampleSize;
  instance->source_format.sampleRate = (*source_sound_description)->desc.sampleRate;
  instance->source_format.sampleCount = 0;
  instance->source_format.buffer = NULL;
  instance->source_format.reserved = 0;
	
  instance->dest_format.flags = 0;
  instance->dest_format.format = kSoundNotCompressed;
  instance->dest_format.numChannels = (*source_sound_description)->desc.numChannels;
  instance->dest_format.sampleSize = (*source_sound_description)->desc.sampleSize;
  instance->dest_format.sampleRate = (*source_sound_description)->desc.sampleRate;
  instance->dest_format.sampleCount = 0;
  instance->dest_format.buffer = NULL;
  instance->dest_format.reserved = 0;
	
  sample->actual.channels = (*source_sound_description)->desc.numChannels;
  sample->actual.rate = (*source_sound_description)->desc.sampleRate >> 16;
	
  if ((*source_sound_description)->desc.sampleSize == 16) {
	
    sample->actual.format = AUDIO_S16SYS;
  }
  else if ((*source_sound_description)->desc.sampleSize == 8) {
	
    sample->actual.format = AUDIO_U8;
  }
  else {
	
    /* 24-bit or others... (which SDL can't handle) */
    return NULL;
  }
	
  DisposeHandle (source_sound_description_extension);
  DisposeHandle ((Handle)source_sound_description);
	
  /* This next code sets up the SoundConverter component */
  error = SoundConverterOpen (&instance->source_format, &instance->dest_format,
			      &instance->converter);
	
  if (error != noErr) {
	
    return NULL;
  }
	
  error = SoundConverterSetInfo (instance->converter, siDecompressionParams, 
				 instance->atom);
  if (error == siUnknownInfoType) {
		
    /* ignore */	
  } 
  else if (error != noErr) {
	
    /* reall error */
    return NULL;
  }

  error = SoundConverterBeginConversion (instance->converter);
  if (error != noErr) {
    
    return NULL;
  }
		
  instance->buffer_data.sourceMedia = instance->media;
  instance->buffer_data.getMediaAtThisTime = 0;		
  instance->buffer_data.sourceDuration = GetMediaDuration(instance->media);
  instance->buffer_data.isThereMoreSource = true;
  instance->buffer_data.maxBufferSize = QT_MAX_INPUT_BUFFER;
  /* allocate source media buffer */
  instance->buffer_data.hSource = NewHandle((long)instance->buffer_data.maxBufferSize); 
  assert (instance->buffer_data.hSource != NULL); /* out of memory */

  instance->buffer_data.compData.desc = instance->source_format;
  instance->buffer_data.compData.desc.buffer = (Byte *)*instance->buffer_data.hSource;
  instance->buffer_data.compData.desc.flags = kExtendedSoundData;
  instance->buffer_data.compData.recordSize = sizeof(ExtendedSoundComponentData);
  instance->buffer_data.compData.extendedFlags = 
    kExtendedSoundSampleCountNotValid | kExtendedSoundBufferSizeValid;
  instance->buffer_data.compData.bufferSize = 0;
	
  instance->fill_buffer_proc = 
    NewSoundConverterFillBufferDataUPP (QT_sound_converter_fill_buffer_data_proc);
	
  return (instance);

} /* QT_open_internal */

static void QT_close_internal (qt_t *instance)
{

} /* QT_close_internal */

static Uint32 QT_read_internal(Sound_Sample *sample)
{
  Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
  qt_t *instance = (qt_t*) internal->decoder_private;
  long output_bytes, output_frames, output_flags;
  OSErr error;
		
  error = SoundConverterFillBuffer 
    (instance->converter,	     /* a sound converter */
     instance->fill_buffer_proc,     /* the callback UPP */
     &instance->buffer_data,	     /* refCon passed to FillDataProc */
     internal->buffer,		     /* the decompressed data 'play' buffer */
     internal->buffer_size,          /* size of the 'play' buffer */
     &output_bytes,	             /* number of output bytes */
     &output_frames,                 /* number of output frames */
     &output_flags);                 /* fillbuffer retured advisor flags */
    
  if (output_flags & kSoundConverterHasLeftOverData) {
    
    sample->flags |= SOUND_SAMPLEFLAG_EAGAIN;
  }
  else {
    
    sample->flags |= SOUND_SAMPLEFLAG_EOF;
  }
    
  if (error != noErr) {
    
    sample->flags |= SOUND_SAMPLEFLAG_ERROR;
  }
    
  return (output_bytes);

} /* QT_read_internal */

static int QT_rewind_internal (Sound_Sample *sample)
{

  return 0;

} /* QT_rewind_internal */



static int QT_init(void)
{
  return (QT_init_internal());

} /* QT_init */

static void QT_quit(void)
{
  QT_quit_internal();

} /* QT_quit */

static int QT_open(Sound_Sample *sample, const char *ext)
{
  Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
  qt_t *instance;

  instance = QT_open_internal(sample, ext);
  internal->decoder_private = (void*)instance;

  return(instance != NULL);
    
} /* QT_open */


static void QT_close(Sound_Sample *sample)
{
  Sound_SampleInternal *internal = (Sound_SampleInternal *) sample->opaque;
  qt_t *instance = (qt_t *) internal->decoder_private;

  QT_close_internal (instance);

  free (instance);

} /* QT_close */


static Uint32 QT_read(Sound_Sample *sample)
{    
  return(QT_read_internal(sample));

} /* QT_read */


static int QT_rewind(Sound_Sample *sample)
{
    
  return(QT_rewind_internal(sample));
    
} /* QT_rewind */


static int QT_seek(Sound_Sample *sample, Uint32 ms)
{
    BAIL_MACRO("QUICKTIME: Seeking not implemented", 0);
} /* QT_seek */


#endif /* SOUND_SUPPORTS_QUICKTIME */

/* end of quicktime.c ... */

