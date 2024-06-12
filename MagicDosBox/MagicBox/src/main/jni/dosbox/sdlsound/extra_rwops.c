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

#include <stdio.h>
#include <stdlib.h>
#include "SDL.h"


    /*
     * The Reference Counter RWops...
     */


typedef struct
{
    SDL_RWops *rw;  /* The actual RWops we're refcounting... */
    int refcount;   /* The refcount; starts at 1. If goes to 0, delete. */
} RWRefCounterData;


/* Just pass through to the actual SDL_RWops's method... */
static int refcounter_seek(SDL_RWops *rw, int offset, int whence)
{
    RWRefCounterData *data = (RWRefCounterData *) rw->hidden.unknown.data1;
    return(data->rw->seek(data->rw, offset, whence));
} /* refcounter_seek */


/* Just pass through to the actual SDL_RWops's method... */
static int refcounter_read(SDL_RWops *rw, void *ptr, int size, int maxnum)
{
    RWRefCounterData *data = (RWRefCounterData *) rw->hidden.unknown.data1;
    return(data->rw->read(data->rw, ptr, size, maxnum));
} /* refcounter_read */


/* Just pass through to the actual SDL_RWops's method... */
static int refcounter_write(SDL_RWops *rw, const void *ptr, int size, int num)
{
    RWRefCounterData *data = (RWRefCounterData *) rw->hidden.unknown.data1;
    return(data->rw->write(data->rw, ptr, size, num));
} /* refcounter_write */


/*
 * Decrement the reference count. If there are no more references, pass
 *   through to the actual SDL_RWops's method, and then clean ourselves up.
 */
static int refcounter_close(SDL_RWops *rw)
{
    int retval = 0;
    RWRefCounterData *data = (RWRefCounterData *) rw->hidden.unknown.data1;
    data->refcount--;
    if (data->refcount <= 0)
    {
        retval = data->rw->close(data->rw);
        free(data);
        SDL_FreeRW(rw);
    } /* if */

    return(retval);
} /* refcounter_close */


void RWops_RWRefCounter_addRef(SDL_RWops *rw)
{
    RWRefCounterData *data = (RWRefCounterData *) rw->hidden.unknown.data1;
    data->refcount++;
} /* RWops_RWRefCounter_addRef */


SDL_RWops *RWops_RWRefCounter_new(SDL_RWops *rw)
{
    SDL_RWops *retval = NULL;

    if (rw == NULL)
    {
        SDL_SetError("NULL argument to RWops_RWRefCounter_new().");
        return(NULL);
    } /* if */

    retval = SDL_AllocRW();
    if (retval != NULL)
    {
        RWRefCounterData *data;
        data = (RWRefCounterData *) malloc(sizeof (RWRefCounterData));
        if (data == NULL)
        {
            SDL_SetError("Out of memory.");
            SDL_FreeRW(retval);
            retval = NULL;
        } /* if */
        else
        {
            data->rw = rw;
            data->refcount = 1;
            retval->hidden.unknown.data1 = data;
            retval->seek = refcounter_seek;
            retval->read = refcounter_read;
            retval->write = refcounter_write;
            retval->close = refcounter_close;
        } /* else */
    } /* if */

    return(retval);
} /* RWops_RWRefCounter_new */


/* end of extra_rwops.c ... */


