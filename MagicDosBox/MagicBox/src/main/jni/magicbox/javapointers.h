#include <jni.h>

#ifndef _javapointers_h
#define _javapointers_h

JNIEnv	*gEnv = NULL;
jobject JavaCallbackThread = NULL;
jmethodID JavaVideoRedraw = NULL;
jmethodID JavaVideoSetMode = NULL;
jmethodID JavaMouseSetVideoMode = NULL;
jmethodID JavaAudioWriteBuffer = NULL;
jmethodID JavaAudioGetBuffer = NULL;
jmethodID JavaAudioInit = NULL;
jmethodID JavaExit = NULL;
jmethodID JavaGetFileDescriptor = NULL;
jmethodID JavaMkDir = NULL;
jmethodID JavaRmDir = NULL;
jmethodID JavaDeleteFile = NULL;
jmethodID JavaRename = NULL;
jmethodID JavaKeybChanged = NULL;
jclass JavaCallbackThreadClass = NULL;

//locnet, 2011-04-28, support 2.1 or below
jmethodID JavaVideoGetBuffer = NULL;

#endif
