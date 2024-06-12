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
 * Some extra RWops that are needed or are just handy to have.
 *
 * Please see the file COPYING in the source's root directory.
 *
 *  This file written by Ryan C. Gordon. (icculus@icculus.org)
 */

#ifndef _INCLUDE_EXTRA_RWOPS_H_
#define _INCLUDE_EXTRA_RWOPS_H_

#include "SDL.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * The Reference Counter RWops...
 *
 *  This wraps another RWops with a reference counter. When you create a
 *   reference counter RWops, it sets a counter to one. Everytime you call
 *   RWops_RWRefCounter_new(), that's RWops's counter increments by one.
 *   Everytime you call that RWops's close() method, the counter decrements
 *   by one. If the counter hits zero, the original RWops's close() method
 *   is called, and the reference counting wrapper deletes itself. The read,
 *   write, and seek methods just pass through to the original.
 *
 *  This is handy if you have two libraries (in the original case, SDL_sound
 *   and SMPEG), who both want an SDL_RWops, and both want to close it when
 *   they are finished. This resolves that contention. The user creates a
 *   RWops, passes it to SDL_sound, which wraps it in a reference counter and
 *   increments the number of references, and passes the wrapped RWops to
 *   SMPEG. SMPEG "closes" this wrapped RWops when the MP3 has finished
 *   playing, and SDL_sound then closes it, too. This second closing removes
 *   the last reference, and the RWops is smoothly destructed.
 */

/* Return a SDL_RWops that is a reference counting wrapper of (rw). */
SDL_RWops *RWops_RWRefCounter_new(SDL_RWops *rw);

/* Increment a reference counting RWops's refcount by one. */
void RWops_RWRefCounter_addRef(SDL_RWops *rw);

#ifdef __cplusplus
}
#endif

#endif /* !defined _INCLUDE_EXTRA_RWOPS_H_ */

/* end of extra_rwops.h ... */

