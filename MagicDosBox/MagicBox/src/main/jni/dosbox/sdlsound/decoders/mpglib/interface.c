
#include <stdlib.h>
#include <stdio.h>

#include "SDL_sound.h"

#define __SDL_SOUND_INTERNAL__
#include "SDL_sound_internal.h"

#include "mpg123_sdlsound.h"
#include "mpglib_sdlsound.h"


BOOL InitMP3(struct mpstr *mp)
{
	static int init = 0;

	memset(mp,0,sizeof(struct mpstr));

	mp->framesize = 0;
	mp->fsizeold = -1;
	mp->bsize = 0;
	mp->head = mp->tail = NULL;
	mp->fr.single = -1;
	mp->bsnum = 0;
	mp->synth_bo = 1;

	if(!init) {
		init = 1;
		make_decode_tables(32767);
		init_layer2();
		init_layer3(SBLIMIT);
	}

	return !0;
}

void ExitMP3(struct mpstr *mp)
{
	struct buf *b,*bn;
	
	b = mp->tail;
	while(b) {
		free(b->pnt);
		bn = b->next;
		free(b);
		b = bn;
	}
}

static struct buf *addbuf(struct mpstr *mp,char *buf,int size)
{
	struct buf *nbuf;

	nbuf = malloc( sizeof(struct buf) );
	BAIL_IF_MACRO(!nbuf, ERR_OUT_OF_MEMORY, NULL);

	nbuf->pnt = malloc(size);
	if(!nbuf->pnt) {
		free(nbuf);
		BAIL_MACRO(ERR_OUT_OF_MEMORY, NULL);
	}
	nbuf->size = size;
	memcpy(nbuf->pnt,buf,size);
	nbuf->next = NULL;
	nbuf->prev = mp->head;
	nbuf->pos = 0;

	if(!mp->tail) {
		mp->tail = nbuf;
	}
	else {
	  mp->head->next = nbuf;
	}

	mp->head = nbuf;
	mp->bsize += size;

	return nbuf;
}

static void remove_buf(struct mpstr *mp)
{
  struct buf *buf = mp->tail;
  
  mp->tail = buf->next;
  if(mp->tail)
    mp->tail->prev = NULL;
  else {
    mp->tail = mp->head = NULL;
  }
  
  free(buf->pnt);
  free(buf);

}

static int read_buf_byte(struct mpstr *mp, unsigned long *retval)
{
	int pos;

	pos = mp->tail->pos;
	while(pos >= mp->tail->size) {
		remove_buf(mp);
		pos = mp->tail->pos;
		if(!mp->tail) {
			BAIL_MACRO("MPGLIB: Short read in read_buf_byte()!", 0);
		}
	}

	if (retval != NULL)
		*retval = mp->tail->pnt[pos];

	mp->bsize--;
	mp->tail->pos++;

	return 1;
}

static int read_head(struct mpstr *mp)
{
	unsigned long val;
	unsigned long head;

	if (!read_buf_byte(mp, &val))
		return 0;

	head = val << 8;

	if (!read_buf_byte(mp, &val))
		return 0;

	head |= val;
	head <<= 8;

	if (!read_buf_byte(mp, &val))
		return 0;

	head |= val;
	head <<= 8;

	if (!read_buf_byte(mp, &val))
		return 0;

	head |= val;
	mp->header = head;
	return 1;
}

int decodeMP3(struct mpstr *mp,char *in,int isize,char *out,
		int osize,int *done)
{
	int len;

	BAIL_IF_MACRO(osize < 4608, "MPGLIB: Output buffer too small", MP3_ERR);

	if(in) {
		if(addbuf(mp,in,isize) == NULL) {
			return MP3_ERR;
		}
	}

	/* First decode header */
	if(mp->framesize == 0) {
		if(mp->bsize < 4) {
			return MP3_NEED_MORE;
		}

		if (!read_head(mp))
			return MP3_ERR;

		if (!decode_header(&mp->fr,mp->header))
			return MP3_ERR;

		mp->framesize = mp->fr.framesize;
	}

	if(mp->fr.framesize > mp->bsize)
		return MP3_NEED_MORE;

	wordpointer = mp->bsspace[mp->bsnum] + 512;
	mp->bsnum = (mp->bsnum + 1) & 0x1;
	bitindex = 0;

	len = 0;
	while(len < mp->framesize) {
		int nlen;
		int blen = mp->tail->size - mp->tail->pos;
		if( (mp->framesize - len) <= blen) {
                  nlen = mp->framesize-len;
		}
		else {
                  nlen = blen;
                }
		memcpy(wordpointer+len,mp->tail->pnt+mp->tail->pos,nlen);
                len += nlen;
                mp->tail->pos += nlen;
		mp->bsize -= nlen;
                if(mp->tail->pos == mp->tail->size) {
                   remove_buf(mp);
                }
	}

	*done = 0;
	if(mp->fr.error_protection)
           getbits(16);
        switch(mp->fr.lay) {
          case 1:
	    do_layer1(&mp->fr,(unsigned char *) out,done,mp);
            break;
          case 2:
	    do_layer2(&mp->fr,(unsigned char *) out,done,mp);
            break;
          case 3:
	    do_layer3(&mp->fr,(unsigned char *) out,done,mp);
            break;
        }

	mp->fsizeold = mp->framesize;
	mp->framesize = 0;

	return MP3_OK;
}

int set_pointer(long backstep, struct mpstr *mp)
{
  unsigned char *bsbufold;
  if(mp->fsizeold < 0 && backstep > 0) {
    char err[128];
    snprintf(err, sizeof (err), "MPGLIB: Can't step back! %ld!", backstep);
    BAIL_MACRO(err, MP3_ERR);
  }
  bsbufold = mp->bsspace[mp->bsnum] + 512;
  wordpointer -= backstep;
  if (backstep)
    memcpy(wordpointer,bsbufold+mp->fsizeold-backstep,backstep);
  bitindex = 0;
  return MP3_OK;
}




