/*
    SDL - Simple DirectMedia Layer
    Copyright (C) 1997-2009 Sam Lantinga

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Sam Lantinga
    slouken@libsdl.org
*/

#ifndef _SDL_screenkeyboard_h
#define _SDL_screenkeyboard_h

#include "SDL_stdinc.h"
#include "SDL_video.h"
#include "SDL_keysym.h"

/* On-screen keyboard exposed to the application, it's yet available on Android platform only */

#include "begin_code.h"
/* Set up for C function definitions, even when using C++ */
#ifdef __cplusplus
extern "C" {
#endif

/* Button IDs */
enum { 

	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_DPAD = 0, /* Joystick/D-Pad button */

	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_0, /* Main (usually Fire) button */
	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_1,
	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_2,
	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_3,
	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_4,
	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_5,
	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_6,

	SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_MAX = SDL_ANDRIOD_SCREENKEYBOARD_BUTTON_6
};

/* All functions return 0 on failure and 1 on success */

extern DECLSPEC int SDLCALL SDL_ANDROID_SetScreenKeyboardButtonPos(int buttonId, SDL_Rect * pos);
extern DECLSPEC int SDLCALL SDL_ANDROID_GetScreenKeyboardButtonPos(int buttonId, SDL_Rect * pos);

extern DECLSPEC int SDLCALL SDL_ANDROID_SetScreenKeyboardButtonKey(int buttonId, SDLKey key);
/* Returns SDLK_UNKNOWN on failure */
extern DECLSPEC SDLKey SDLCALL SDL_ANDROID_GetScreenKeyboardButtonKey(int buttonId);

/* Buttons 0 and 1 may have auto-fire state */
extern DECLSPEC int SDLCALL SDL_ANDROID_SetScreenKeyboardAutoFireButtonsAmount(int nbuttons);
extern DECLSPEC int SDLCALL SDL_ANDROID_GetScreenKeyboardAutoFireButtonsAmount();

extern DECLSPEC int SDLCALL SDL_ANDROID_SetScreenKeyboardShown(int shown);
extern DECLSPEC int SDLCALL SDL_ANDROID_GetScreenKeyboardShown();

extern DECLSPEC int SDLCALL SDL_ANDROID_GetScreenKeyboardSize();

#ifdef __cplusplus
}
#endif
#include "close_code.h"

#endif
