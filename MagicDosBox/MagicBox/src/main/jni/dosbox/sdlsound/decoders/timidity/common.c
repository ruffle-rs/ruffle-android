/*

    TiMidity -- Experimental MIDI to WAVE converter
    Copyright (C) 1995 Tuukka Toivonen <toivonen@clinet.fi>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

   common.c

   */

#if HAVE_CONFIG_H
#  include <config.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

#include "options.h"
#include "common.h"

/* The paths in this list will be tried whenever we're reading a file */
static PathList *pathlist = NULL; /* This is a linked list */

/* This is meant to find and open files for reading */
SDL_RWops *open_file(char *name)
{
  SDL_RWops *rw;

  if (!name || !(*name))
    {
      SNDDBG(("Attempted to open nameless file.\n"));
      return 0;
    }

  /* First try the given name */

  SNDDBG(("Trying to open %s\n", name));
  if ((rw = SDL_RWFromFile(name, "rb")))
    return rw;

  if (name[0] != PATH_SEP)
  {
    char current_filename[1024];
    PathList *plp = pathlist;
    int l;

    while (plp)  /* Try along the path then */
      {
	*current_filename = 0;
	l = strlen(plp->path);
	if(l)
	  {
	    strcpy(current_filename, plp->path);
	    if(current_filename[l - 1] != PATH_SEP)
	    {
	      current_filename[l] = PATH_SEP;
	      current_filename[l + 1] = '\0';
	    }
	  }
	strcat(current_filename, name);
	SNDDBG(("Trying to open %s\n", current_filename));
	if ((rw = SDL_RWFromFile(current_filename, "rb")))
	  return rw;
	plp = plp->next;
      }
  }
  
  /* Nothing could be opened. */
  SNDDBG(("Could not open %s\n", name));
  return 0;
}

/* This'll allocate memory or die. */
void *safe_malloc(size_t count)
{
  void *p;

  p = malloc(count);
  if (p == NULL)
    SNDDBG(("Sorry. Couldn't malloc %d bytes.\n", count));

  return p;
}

/* This adds a directory to the path list */
void add_to_pathlist(char *s)
{
  PathList *plp = safe_malloc(sizeof(PathList));

  if (plp == NULL)
      return;

  plp->path = safe_malloc(strlen(s) + 1);
  if (plp->path == NULL)
  {
      free(plp);
      return;
  }

  strcpy(plp->path, s);
  plp->next = pathlist;
  pathlist = plp;
}

void free_pathlist(void)
{
    PathList *plp = pathlist;
    PathList *next;

    while (plp)
    {
	next = plp->next;
	free(plp->path);
	free(plp);
	plp = next;
    }
    pathlist = NULL;
}
