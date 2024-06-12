/* Programmer: Nicholas Wertzberger
 * 		Email: wertnick@gmail.com
 *
 * A listing of helper functions for integrating with the JNI.
 */
#ifndef _STREAM_UTIL_H
#define _STREAM_UTIL_H

enum {
	BAD_MEM = -1,
};

void
JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg, int code);

#endif
