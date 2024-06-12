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

/**
 * Program to test the TiMidity core, without having to worry about decoder
 * and/or playsound bugs. It's not meant to be robust or user-friendly.
 */

#include <stdio.h>
#include <stdlib.h>
#include "SDL.h"
#include "timidity.h"

int done_flag = 0;
MidiSong *song;
    
static void audio_callback(void *userdata, Uint8 *stream, int len)
{
    if (Timidity_PlaySome(song, stream, len) == 0)
	done_flag = 1;
}

int main(int argc, char *argv[])
{
    SDL_AudioSpec audio;
    SDL_RWops *rw;

    if (SDL_Init(SDL_INIT_AUDIO) < 0)
    {
	fprintf(stderr, "Couldn't initialize SDL: %s\n", SDL_GetError());
	return 1;
    }

    atexit(SDL_Quit);

    if (argc != 2)
    {
	fprintf(stderr, "Usage: %s midifile\n", argv[0]);
	return 1;
    }

    audio.freq = 44100;
    audio.format = AUDIO_S16SYS;
    audio.channels = 2;
    audio.samples = 4096;
    audio.callback = audio_callback;

    if (SDL_OpenAudio(&audio, NULL) < 0)
    {
	fprintf(stderr, "Couldn't open audio device. %s\n", SDL_GetError());
	return 1;
    }

    if (Timidity_Init() < 0)
    {
	fprintf(stderr, "Could not initialise TiMidity.\n");
	return 1;
    }
    
    rw = SDL_RWFromFile(argv[1], "rb");
    if (rw == NULL)
    {
	fprintf(stderr, "Could not create RWops from MIDI file.\n");
	return 1;
    }
	
    song = Timidity_LoadSong(rw, &audio);
    SDL_RWclose(rw);
    
    if (song == NULL)
    {
	fprintf(stderr, "Could not open MIDI file.\n");
	return 1;
    }

    Timidity_SetVolume(song, 100);
    Timidity_Start(song);
    
    SDL_PauseAudio(0);
    while (!done_flag)
    {
	SDL_Delay(10);
    }
    SDL_PauseAudio(1);
    Timidity_FreeSong(song);
    Timidity_Exit();
    
    return 0;
}
