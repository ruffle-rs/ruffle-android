/*
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

#define USE_JNIGRAPHIC 0

#include <jni.h>
#if USE_JNIGRAPHIC
#include <android/bitmap.h>
#endif

#include "config.h"
#include "AndroidOSfunc.h"
#include "string.h"
#include "loader.h"
#include "SDL.h"
#include "render.h"
#include "keyboard.h"
#include "keycodes.h"
#include <unistd.h>
#include <sys/stat.h>
#include <dirent.h>
#include <queue>
#include <android/log.h>
#include <time.h>
#include <map>
#include <sstream>
#include "javapointers.h"
#include "mouse.h"
#include "EventQueue.h"
//#include "SDL_thread.h"
#include <pthread.h>

//std::queue<struct InputEvent> eventQueue;
EventQueue inputQueue;
std::map<std::string, double> deletedCache;
std::map<std::string, double>::iterator cacheItem;//, cacheItem2;

struct loader_config myLoader;
struct loader_config *loadf;
//SDL_mutex* input_mutex;
pthread_mutex_t mutexlock;
static bool inputLockInitialized;
static int useInputLocks;

bool	enableSound = true;
bool	enableCycleHack = true;
bool	enableRefreshHack = true;
const int cacheTimeout = 15000;

const char *safSupportPath = NULL;
size_t safSupportPathSize;

//extern SDL_AudioCallback mixerCallBack;
extern void (SDLCALL *mixerCallBack)(void *userdata, Uint8 *stream, int len);

void Android_Exit();
char *Android_VideoGetBuffer();

static double now_ms(void)
{
	struct timespec res;
	clock_gettime(CLOCK_REALTIME, &res);
	return 1000.0*res.tv_sec + (double)res.tv_nsec/1e6;
}

int FileExists(const char *path)
{
	struct stat fileStat;
	if ( stat(path, &fileStat) )
	{
		return 0;
	}

	if ( !S_ISREG(fileStat.st_mode) )
	{
		return 0;
	}

	return 1;
}

int DirExists(const char *path)
{
	struct stat fileStat;
	if (stat(path, &fileStat))
	{
		return 0;
	}

	if ( !S_ISDIR(fileStat.st_mode) )
	{
		return 0;
	}

	return 1;
}

void Android_Init(JNIEnv * env, jobject obj, jobject videoBuffer, jint width, jint height, jstring safPath) {
	gEnv = env;
	//input_mutex = SDL_CreateMutex();
	pthread_mutex_init(&mutexlock, 0);
	inputLockInitialized = false;
	useInputLocks = 0;//0 - don't use, 1 use locks

	JavaCallbackThread = env->NewGlobalRef(obj);
	JavaCallbackThreadClass = env->GetObjectClass(JavaCallbackThread);
	JavaVideoRedraw = env->GetMethodID(JavaCallbackThreadClass, "callbackVideoRedraw", "(IIII)V");
	JavaVideoSetMode = env->GetMethodID(JavaCallbackThreadClass, "callbackVideoSetMode", "(II)Landroid/graphics/Bitmap;");
	JavaMouseSetVideoMode = env->GetMethodID(JavaCallbackThreadClass, "callbackMouseSetVideoMode", "(III)V");
	JavaAudioInit = env->GetMethodID(JavaCallbackThreadClass, "callbackAudioInit", "(IIII)I");
	JavaAudioWriteBuffer = env->GetMethodID(JavaCallbackThreadClass, "callbackAudioWriteBuffer", "(I)V");
	JavaAudioGetBuffer = env->GetMethodID(JavaCallbackThreadClass, "callbackAudioGetBuffer", "()[S");
	JavaExit = env->GetMethodID(JavaCallbackThreadClass, "callbackExit", "()V");
	JavaKeybChanged = env->GetMethodID(JavaCallbackThreadClass, "callbackKeybChanged", "(Ljava/lang/String;)V");

	if (safPath != NULL) {
		safSupportPath = env->GetStringUTFChars(safPath, JNI_FALSE);
		safSupportPathSize = strlen(safSupportPath);

		//TODO - should be release with
		//(*env)->ReleaseStringUTFChars(env, javaString, nativeString);

		JavaGetFileDescriptor = env->GetMethodID(JavaCallbackThreadClass, "callbackGetFileDescriptor", "(Ljava/lang/String;ZI)I");
		JavaMkDir = env->GetMethodID(JavaCallbackThreadClass, "callbackMkDir", "(Ljava/lang/String;I)I");
		JavaRmDir = env->GetMethodID(JavaCallbackThreadClass, "callbackRmDir", "(Ljava/lang/String;)I");
		JavaDeleteFile = env->GetMethodID(JavaCallbackThreadClass, "callbackDeleteFile", "(Ljava/lang/String;I)I");
		JavaRename = env->GetMethodID(JavaCallbackThreadClass, "callbackRename", "(Ljava/lang/String;Ljava/lang/String;)I");
	}
	//locnet, 2011-04-28, support 2.1 or below
	JavaVideoGetBuffer = env->GetMethodID(JavaCallbackThreadClass, "callbackVideoGetBuffer", "()Ljava/nio/Buffer;");

	char * buffer = 0;

	if (videoBuffer != 0) {
		buffer = (char *)gEnv->GetDirectBufferAddress(videoBuffer);
		gEnv->DeleteLocalRef(videoBuffer);
		videoBuffer = 0;
	}

	myLoader.videoBuffer = buffer;
	myLoader.width = width;
	myLoader.height = height;
	myLoader.rowbytes = myLoader.width*2;

	loadf=&myLoader;
	enableSound = myLoader.soundEnable;
	enableCycleHack = myLoader.cycleHack;
	enableRefreshHack = myLoader.refreshHack;
}

/*

"r"	    read: Open file for input operations. The file must exist.
"w"	    write: Create an empty file for output operations. If a file with the
        same name already exists, its contents are discarded and the file is treated as a new empty file.
"a"	    append: Open file for output at the end of a file. Output operations always write data at the end of
        the file, expanding it. Repositioning operations (fseek, fsetpos, rewind) are ignored. The file is
        created if it does not exist.
"r+"	read/update: Open a file for update (both for input and output). The file must exist.
"w+"	write/update: Create an empty file and open it for update (both for input and output). If a file
        with the same name already exists its contents are discarded and the file is treated as a new empty file.
"a+"	append/update: Open a file for update (both for input and output) with all output operations writing data
        at the end of the file. Repositioning operations (fseek, fsetpos, rewind) affects the next input operations,
        but output operations move the position back to the end of file. The file is created if it does not exist.

*/
FILE * Android_FTruncate(FILE *fhandle, const char *path, off_t length)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_FTruncate - %s", path);
	char* buf;

	if (length > 0) {
		//buf=new char[length];
		buf = (char*) calloc (length,sizeof(char));
		fseek(fhandle,0,SEEK_SET);
		fread(buf, 1, length, fhandle);
	}

	fclose(fhandle);

	fhandle = fdopen(Android_GetFD(path, true, 1), "wb+");

	if (length>0) {
		fwrite(buf,1,length,fhandle);
		fflush(fhandle);
		//delete(buf);
		free(buf);
	}

	fclose(fhandle);

	fhandle = fdopen(Android_GetFD(path, false, 1), "rb+");
	fseek(fhandle, 0, SEEK_END);

	return fhandle;
}

int ParentDirExists(const char *file)
{
	const char * parentDir = strndup(file, strrchr(file, '/') - file);
	cacheItem = deletedCache.find(parentDir);

	if (cacheItem == deletedCache.end()) {
		return DirExists(parentDir)?0:-1;
	}

	if ((now_ms() - cacheItem->second) > cacheTimeout) {
		deletedCache.erase(cacheItem);
	}

	return -1;
}

inline int FileExistAndDirExist(const char *file, bool modeR, bool modeW, bool modePlus)
{
	struct stat buff;
	int result = stat(file, &buff);

	//(file or dir with this name exists) && is dir
	if (!result && S_ISDIR(buff.st_mode))
		return -1;

	//w, wb, wb+, rb+
	//file does not exists
	if (result)
	{
		//rb+ can't create file
		if (modeR)
			return -1;

		//w, wb, wb+ - file will be created if parent directory exists
		return ParentDirExists(file);
	}

	return 1;
}

FILE * Android_OpenFile(const char *file, const char *mode)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_OpenFile start %s %s", file, mode);

	bool modeR = false;
	bool modeW = false;
	bool modePlus = false;

	for(int i=strlen(mode)-1;i>=0;i--)
	{
		//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_OpenFile loop %s %s, %d, %c", file, mode, i, mode[i]);
		switch (mode[i])
		{
			case 'r' : {modeR = true;break;}
			case 'w' : {modeW = true;break;}
			case '+' : {modePlus = true;break;}
		}
	}

	int fileExists;
	cacheItem = deletedCache.find(file);

	if ( cacheItem == deletedCache.end()) {
		//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_Cache OpenFile !cached %s", file);

		//r, rb
		if (modeR && !modePlus)
			return fopen(file, mode);

		//wb, wb+, rb+
		fileExists = FileExistAndDirExist(file, modeR, modeW, modePlus);
		if (fileExists == -1)
			return NULL;
	} else {
		//File was deleted
		if ((now_ms() - cacheItem->second) > cacheTimeout) {
			deletedCache.erase(cacheItem);
		}

		if (modeR || ParentDirExists(file)==-1)
			return NULL;

		fileExists = 0;
	}

	//double t1 = now_ms();

	//int fd = Android_GetFD(file, modeW, fileExists);
	jstring string = gEnv->NewStringUTF(file);
	int fd = gEnv->CallIntMethod( JavaCallbackThread, JavaGetFileDescriptor, string, modeW, fileExists);
	gEnv->DeleteLocalRef(string);

	//double t2 = now_ms();
	//t+=t2-t1;
	//count++;
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_OpenFile time %s %g", file, (t/count));
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_OpenFile time %s %g", file, t2-t1);

	if (fd == -1) {
		//error occured
		return NULL;
	}

	if (fileExists==0) {
		deletedCache.erase(file);
	}

	return fdopen(fd, mode);
}

bool Android_IsSAFDrive(const char *path)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_IsSAFDrive2 %s %s %d %d", path, safSupportPath, safSupport?1:0, safSupportPathSize);

	if (safSupportPath == NULL)
		return false;

	size_t pathLength = strlen(path);

	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_IsSAFDrive2 1");

	if ((safSupportPathSize > pathLength) || (strncasecmp(path, safSupportPath, safSupportPathSize) != 0))
		return false;

	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_IsSAFDrive2 2 '%c'", path[safSupportPathSize]);

	if (pathLength == safSupportPathSize || path[safSupportPathSize] == '/' || path[safSupportPathSize] == '\\') {
		return true;
	}
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_IsSAFDrive2 3");

	return false;
}

int Android_GetFD(const char *fileName, bool modeW, int fileExists)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_GetFD %s", fileName);

	jstring string = gEnv->NewStringUTF(fileName);
	int result = gEnv->CallIntMethod( JavaCallbackThread, JavaGetFileDescriptor, string, modeW, fileExists);
	gEnv->DeleteLocalRef(string);
	return result;
}

int isDirSuitableForMk(const char *path)
{
	if (DirExists(path)==1)
		return 0;

	const char *parentDir = strndup(path, strrchr(path, '/') - path);
	cacheItem = deletedCache.find(parentDir);
/*
	if (cacheItem == deletedCache.end()) {
		if (DirExists(parentDir)==0)
			return 0;
	} else {
		if ((now_ms() - cacheItem->second) > cacheTimeout) {
			deletedCache.erase(cacheItem);
			if (DirExists(parentDir)==0)
				return 0;
		} else {
			return -2;
		}
	}*/

	if (cacheItem == deletedCache.end()) {
		return DirExists(parentDir);
	}

	if ((now_ms() - cacheItem->second) > cacheTimeout) {
		deletedCache.erase(cacheItem);
	}

	return 0;
}

int Android_MkDir(const char *path)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_MkDir %s", path);
	int checkDir;
	cacheItem = deletedCache.find(path);
	if (cacheItem == deletedCache.end()) {
		checkDir = isDirSuitableForMk(path);
		if (checkDir==0)
			return -1;
	} else {
		if ((now_ms() - cacheItem->second) > cacheTimeout) {
			deletedCache.erase(cacheItem);
			checkDir = isDirSuitableForMk(path);
			if (checkDir==0)
				return -1;
		} else {
			checkDir = -1;
		}
	}

	jstring string = gEnv->NewStringUTF(path);
	int result = gEnv->CallIntMethod( JavaCallbackThread, JavaMkDir, string, checkDir);
	gEnv->DeleteLocalRef(string);

	if (result==0/* && checkDir==-1*/) {
		deletedCache.erase(path);
	}

	return result;
}

int Android_RmDir(const char *path)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_RmDir %s", path);

	jstring string=gEnv->NewStringUTF(path);
	int result = gEnv->CallIntMethod( JavaCallbackThread, JavaRmDir, string);
	gEnv->DeleteLocalRef(string);

	if (result == 0) {
		deletedCache[std::string(path)] = now_ms();
	}

	return result;
}

int Android_deleteFile(const char *path)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_deleteFile %s", path);

	int fileExists;
	cacheItem = deletedCache.find(path);

	if (cacheItem == deletedCache.end()) {
		fileExists = FileExists(path);
		if (fileExists == 0) return -1;
	} else {
		/*if ((now_ms() - cacheItem->second) > cacheTimeout) {
			deletedCache.erase(cacheItem);
			fileExists = FileExists(path);
			if (fileExists == 0) return -1;
		} else {
			fileExists = -1;
		}*/

		//The file is in deleteCache. This means that was deleted with SAF on Java side.
		//The file is removed from deleteCache when timout is expired or if the file was recreated in
		//Android_OpenFile function
		if ((now_ms() - cacheItem->second) > cacheTimeout) {
			deletedCache.erase(cacheItem);
		}

		return -1;
	}

	jstring string=gEnv->NewStringUTF(path);
	int result = gEnv->CallIntMethod( JavaCallbackThread, JavaDeleteFile, string, fileExists);
	gEnv->DeleteLocalRef(string);

	if (result == 0) {
		//Add deleted file to deleteCache
		deletedCache[std::string(path)] = now_ms();
	}

	return result;
}

int Android_Rename(const char *path, const char *name)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_Rename source==%s dest==%s SAF=%d", path, name, isSAF?1:0);

	jstring string1=gEnv->NewStringUTF(path);
	jstring string2=gEnv->NewStringUTF(name);

	int result = gEnv->CallIntMethod( JavaCallbackThread, JavaRename, string1, string2);

	gEnv->DeleteLocalRef(string1);
	gEnv->DeleteLocalRef(string2);

	if (result == 0) {
		//Renamed/Moved file acts like deleted, so we add it to deleteCache
		deletedCache[std::string(path)] = now_ms();

		//new file must be removed from deleteCache
		cacheItem = deletedCache.find(name);
		if (cacheItem != deletedCache.end()) {
			deletedCache.erase(cacheItem);
		}
	}

	return result;
}

bool Android_FileExists(const char *path)
{
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_FileExists %s", path);
	cacheItem = deletedCache.find(path);

	if (cacheItem == deletedCache.end()) {
		return FileExists(path) == 1;
	}

	if ((now_ms() - cacheItem->second) > cacheTimeout) {
		deletedCache.erase(cacheItem);
		//return FileExists(path) == 1;
	}

	return false;
}

int Android_stat(const char *path, struct stat *buf)
{
	cacheItem = deletedCache.find(path);

	if (cacheItem == deletedCache.end()) {
		return stat(path, buf);
	}

	if ((now_ms() - cacheItem->second) > cacheTimeout) {
		deletedCache.erase(cacheItem);
	}

	return -1;
}

int Android_Access(const char *path, int amode)
{
	cacheItem = deletedCache.find(path);

	if (cacheItem == deletedCache.end()) {
		return access(path, amode);
	}

	if ((now_ms() - cacheItem->second) > cacheTimeout) {
		deletedCache.erase(cacheItem);
	}

	return -1;
}

void Android_KeyboardChanged(const char *keyb)
{
	jstring param1=gEnv->NewStringUTF(keyb);
	gEnv->CallVoidMethod(JavaCallbackThread, JavaKeybChanged, param1);
	gEnv->DeleteLocalRef(param1);
}

void Android_ShutDown(bool forced) {
	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_ShutDown");
	//locnet, 2011-04-28, support 2.1 or below
	JavaVideoGetBuffer = NULL;

	JavaAudioGetBuffer = NULL;
	JavaAudioWriteBuffer = NULL;
	JavaAudioInit = NULL;
	JavaVideoRedraw = NULL;
	JavaKeybChanged = NULL;
	JavaCallbackThreadClass = NULL;

	//SAF support
	if (JavaGetFileDescriptor != NULL) {
		JavaGetFileDescriptor = NULL;
		JavaMkDir = NULL;
		JavaRmDir = NULL;
		JavaDeleteFile = NULL;
		JavaRename = NULL;
	}
//	SDL_DestroyMutex(input_mutex);
//	while (eventQueue.size())
//		eventQueue.pop();
	pthread_mutex_destroy(&mutexlock);
	deletedCache.clear();
	if (forced) {
		exit(0);
	} else {
		//atexit(Android_Exit);
		//exit(0);
		Android_Exit();
	}
}
/*
//addition key not defined in keycodes.h
#define AKEYCODE_ESCAPE	111
#define AKEYCODE_FORWARD_DEL	112
#define AKEYCODE_CTRL_LEFT		113
#define AKEYCODE_HOME	122
#define AKEYCODE_END	123
#define AKEYCODE_INSERT	124

#define AKEYCODE_F1	131
#define AKEYCODE_F2	132
#define AKEYCODE_F3	133
#define AKEYCODE_F4	134
#define AKEYCODE_F5	135
#define AKEYCODE_F6	136
#define AKEYCODE_F7	137
#define AKEYCODE_F8	138
#define AKEYCODE_F9	139
#define AKEYCODE_F10	140
#define AKEYCODE_F11	141
#define AKEYCODE_F12	142
*/
int getKeyFromUnicode(int unicode, jint &shift);

extern "C" jint Java_magiclib_core_NativeControl_nativeGetMouseMaxX(JNIEnv * env, jobject obj)
{
	return Mouse_GetMaxX();
}

extern "C" jint Java_magiclib_core_NativeControl_nativeGetMouseMaxY(JNIEnv * env, jobject obj)
{
	return Mouse_GetMaxY();
}

extern "C" jint Java_magiclib_core_NativeControl_nativeKey(JNIEnv * env, jobject obj, jint keyCode, jint down, jint ctrl, jint alt, jint shift)
{
	keyCode = keyCode & 0xFF;

	int dosboxKeycode = KBD_NONE;
	int unicode = (keyCode >> 8) & 0xFF;

	if (unicode != 0)
	{
		dosboxKeycode = getKeyFromUnicode(unicode, shift);
	}

	if (dosboxKeycode == KBD_NONE)
	{
		switch (keyCode)
		{
			case AKEYCODE_CTRL_LEFT:    dosboxKeycode = KBD_leftctrl;    break;
			case AKEYCODE_CTRL_RIGHT:   dosboxKeycode = KBD_rightctrl;   break;
			case AKEYCODE_ALT_LEFT:     dosboxKeycode = KBD_leftalt;     break;
			case AKEYCODE_ALT_RIGHT:    dosboxKeycode = KBD_rightalt;    break;
			case AKEYCODE_SHIFT_LEFT:   dosboxKeycode = KBD_leftshift;   break;
			case AKEYCODE_SHIFT_RIGHT:  dosboxKeycode = KBD_rightshift;  break;

			case AKEYCODE_INSERT:       dosboxKeycode = KBD_insert;      break;
			case AKEYCODE_HOME:         dosboxKeycode = KBD_home;        break;
			case AKEYCODE_FORWARD_DEL:  dosboxKeycode = KBD_delete;      break;
			case AKEYCODE_MOVE_HOME:    dosboxKeycode = KBD_home;        break;
			case AKEYCODE_MOVE_END:	    dosboxKeycode = KBD_end;         break;

			case AKEYCODE_AT:		    dosboxKeycode = KBD_2; shift = 1;	   break;
			case AKEYCODE_POUND:	    dosboxKeycode = KBD_3; shift = 1;	   break;
			case AKEYCODE_STAR:		    dosboxKeycode = KBD_8; shift = 1;	   break;
			case AKEYCODE_PLUS:		    dosboxKeycode = KBD_equals; shift = 1; break;

			case AKEYCODE_ESCAPE:	    dosboxKeycode = KBD_esc;			  break;
			case AKEYCODE_TAB:		    dosboxKeycode = KBD_tab;			  break;
			case AKEYCODE_DEL:		    dosboxKeycode = KBD_backspace;	 	  break;
			case AKEYCODE_ENTER:	    dosboxKeycode = KBD_enter;			  break;
			case AKEYCODE_SPACE:	    dosboxKeycode = KBD_space;			  break;
			case AKEYCODE_CAPS_LOCK:	dosboxKeycode = KBD_capslock;   	  break;
			case AKEYCODE_SCROLL_LOCK:	dosboxKeycode = KBD_scrolllock;   	  break;

			case AKEYCODE_DPAD_LEFT:	dosboxKeycode = KBD_left;			  break;
			case AKEYCODE_DPAD_UP:		dosboxKeycode = KBD_up;			      break;
			case AKEYCODE_DPAD_DOWN:	dosboxKeycode = KBD_down;			  break;
			case AKEYCODE_DPAD_RIGHT:	dosboxKeycode = KBD_right;			  break;

			case AKEYCODE_GRAVE:	    dosboxKeycode = KBD_grave;			  break;
			case AKEYCODE_MINUS:	    dosboxKeycode = KBD_minus;			  break;
			case AKEYCODE_EQUALS:	    dosboxKeycode = KBD_equals;			  break;
			case AKEYCODE_BACKSLASH:	dosboxKeycode = KBD_backslash;		  break;
			case AKEYCODE_LEFT_BRACKET:	dosboxKeycode = KBD_leftbracket;	  break;
			case AKEYCODE_RIGHT_BRACKET:dosboxKeycode = KBD_rightbracket;     break;
			case AKEYCODE_SEMICOLON:	dosboxKeycode = KBD_semicolon;		  break;
			case AKEYCODE_APOSTROPHE:	dosboxKeycode = KBD_quote;			  break;
			case AKEYCODE_PERIOD:	    dosboxKeycode = KBD_period;			  break;
			case AKEYCODE_COMMA:	    dosboxKeycode = KBD_comma;			  break;
			case AKEYCODE_SLASH:		dosboxKeycode = KBD_slash;			  break;
			case AKEYCODE_NUMPAD_LEFT_PAREN:  dosboxKeycode = KBD_9;shift = 1;break;
			case AKEYCODE_NUMPAD_RIGHT_PAREN: dosboxKeycode = KBD_0;shift = 1;break;
			case AKEYCODE_PAGE_UP:		dosboxKeycode = KBD_pageup; 	break;
			case AKEYCODE_PAGE_DOWN:	dosboxKeycode = KBD_pagedown; 	break;
			case AKEYCODE_BREAK:         dosboxKeycode = KBD_pause; break;

			case AKEYCODE_A:		dosboxKeycode = KBD_a;			break;
			case AKEYCODE_B:		dosboxKeycode = KBD_b;			break;
			case AKEYCODE_C:		dosboxKeycode = KBD_c;			break;
			case AKEYCODE_D:		dosboxKeycode = KBD_d;			break;
			case AKEYCODE_E:		dosboxKeycode = KBD_e;			break;
			case AKEYCODE_F:		dosboxKeycode = KBD_f;			break;
			case AKEYCODE_G:		dosboxKeycode = KBD_g;			break;
			case AKEYCODE_H:		dosboxKeycode = KBD_h;			break;
			case AKEYCODE_I:		dosboxKeycode = KBD_i;			break;
			case AKEYCODE_J:		dosboxKeycode = KBD_j;			break;
			case AKEYCODE_K:		dosboxKeycode = KBD_k;			break;
			case AKEYCODE_L:		dosboxKeycode = KBD_l;			break;
			case AKEYCODE_M:		dosboxKeycode = KBD_m;			break;
			case AKEYCODE_N:		dosboxKeycode = KBD_n;			break;
			case AKEYCODE_O:		dosboxKeycode = KBD_o;			break;
			case AKEYCODE_P:		dosboxKeycode = KBD_p;			break;
			case AKEYCODE_Q:		dosboxKeycode = KBD_q;			break;
			case AKEYCODE_R:		dosboxKeycode = KBD_r;			break;
			case AKEYCODE_S:		dosboxKeycode = KBD_s;			break;
			case AKEYCODE_T:		dosboxKeycode = KBD_t;			break;
			case AKEYCODE_U:		dosboxKeycode = KBD_u;			break;
			case AKEYCODE_V:		dosboxKeycode = KBD_v;			break;
			case AKEYCODE_W:		dosboxKeycode = KBD_w;			break;
			case AKEYCODE_X:		dosboxKeycode = KBD_x;			break;
			case AKEYCODE_Y:		dosboxKeycode = KBD_y;			break;
			case AKEYCODE_Z:		dosboxKeycode = KBD_z;			break;

			case AKEYCODE_0:		dosboxKeycode = KBD_0;			break;
			case AKEYCODE_1:		dosboxKeycode = KBD_1;			break;
			case AKEYCODE_2:		dosboxKeycode = KBD_2;			break;
			case AKEYCODE_3:		dosboxKeycode = KBD_3;			break;
			case AKEYCODE_4:		dosboxKeycode = KBD_4;			break;
			case AKEYCODE_5:		dosboxKeycode = KBD_5;			break;
			case AKEYCODE_6:		dosboxKeycode = KBD_6;			break;
			case AKEYCODE_7:		dosboxKeycode = KBD_7;			break;
			case AKEYCODE_8:		dosboxKeycode = KBD_8;			break;
			case AKEYCODE_9:		dosboxKeycode = KBD_9;			break;

			case AKEYCODE_F1:		dosboxKeycode = KBD_f1;			break;
			case AKEYCODE_F2:		dosboxKeycode = KBD_f2;			break;
			case AKEYCODE_F3:		dosboxKeycode = KBD_f3;			break;
			case AKEYCODE_F4:		dosboxKeycode = KBD_f4;			break;
			case AKEYCODE_F5:		dosboxKeycode = KBD_f5;			break;
			case AKEYCODE_F6:		dosboxKeycode = KBD_f6;			break;
			case AKEYCODE_F7:		dosboxKeycode = KBD_f7;			break;
			case AKEYCODE_F8:		dosboxKeycode = KBD_f8;			break;
			case AKEYCODE_F9:		dosboxKeycode = KBD_f9;			break;
			case AKEYCODE_F10:		dosboxKeycode = KBD_f10;		break;
			case AKEYCODE_F11:		dosboxKeycode = KBD_f11;		break;
			case AKEYCODE_F12:		dosboxKeycode = KBD_f12;		break;
			case AKEYCODE_NUM_LOCK: dosboxKeycode = KBD_numlock;	break;
			case AKEYCODE_NUMPAD_0: dosboxKeycode = KBD_kp0;		break;
			case AKEYCODE_NUMPAD_1: dosboxKeycode = KBD_kp1;		break;
			case AKEYCODE_NUMPAD_2: dosboxKeycode = KBD_kp2;		break;
			case AKEYCODE_NUMPAD_3: dosboxKeycode = KBD_kp3;		break;
			case AKEYCODE_NUMPAD_4: dosboxKeycode = KBD_kp4;		break;
			case AKEYCODE_NUMPAD_5: dosboxKeycode = KBD_kp5;		break;
			case AKEYCODE_NUMPAD_6: dosboxKeycode = KBD_kp6;		break;
			case AKEYCODE_NUMPAD_7: dosboxKeycode = KBD_kp7;		break;
			case AKEYCODE_NUMPAD_8: dosboxKeycode = KBD_kp8;		break;
			case AKEYCODE_NUMPAD_9: dosboxKeycode = KBD_kp9;		break;
			case AKEYCODE_NUMPAD_MULTIPLY: dosboxKeycode = KBD_kpmultiply;	break;
			case AKEYCODE_NUMPAD_DIVIDE: dosboxKeycode = KBD_kpdivide;	break;
			case AKEYCODE_NUMPAD_SUBTRACT: dosboxKeycode = KBD_kpminus;		break;
			case AKEYCODE_NUMPAD_ADD: dosboxKeycode = KBD_kpplus;		break;
			case AKEYCODE_NUMPAD_ENTER: dosboxKeycode = KBD_kpenter;	break;
			case AKEYCODE_NUMPAD_DOT: dosboxKeycode = KBD_kpperiod;	break;
			case AKEYCODE_NUMPAD_COMMA: dosboxKeycode = KBD_comma;	break;
			case AKEYCODE_NUMPAD_EQUALS: dosboxKeycode = KBD_equals;	break;


			default:
				break;
		}
	}

	if (dosboxKeycode != KBD_NONE)
	{
		int modifier = 0;

		if (ctrl)
			modifier |= KEYBOARD_CTRL_FLAG;
		if (alt)
			modifier |= KEYBOARD_ALT_FLAG;
		if (shift)
			modifier |= KEYBOARD_SHIFT_FLAG;

		int useLock = useInputLocks;
		if (useLock) {
			pthread_mutex_lock(&mutexlock);
		}

		InputEvent *event = inputQueue.getWriteEvent();
		if (!event) {
			if (useLock) {
				pthread_mutex_unlock(&mutexlock);
			}
			return 0;
		}

		event->keycode = dosboxKeycode;
//		event->aKeycode = keyCode;
		event->eventType = (down)?SDL_KEYDOWN:SDL_KEYUP;
		event->modifier = modifier;

		inputQueue.incWriteIndex();

		if (useLock) {
			pthread_mutex_unlock(&mutexlock);
		}

		return 1;
	}

	return 0;
}

extern "C" void Java_magiclib_core_NativeControl_nativeJoystick(JNIEnv * env, jobject obj, jint x, jint y, jint action, jint button)
{
	if (!inputLockInitialized) {
		useInputLocks = 1;
		inputLockInitialized = true;
	}
	pthread_mutex_lock(&mutexlock);

	InputEvent	*event = inputQueue.getWriteEvent();
	if (!event) {
		pthread_mutex_unlock(&mutexlock);
		return;
	}

	event->eventType = SDL_NOEVENT;

	switch (action) {
		case 0:
			event->eventType = SDL_JOYBUTTONDOWN;
			event->keycode = button;
			break;
		case 1:
			event->eventType = SDL_JOYBUTTONUP;
			event->keycode = button;
			break;
		case 2:
			event->eventType = SDL_JOYAXISMOTION;
			event->x = x;
			event->y = y;
			break;
	}

	if 	(event->eventType != SDL_NOEVENT)
		inputQueue.incWriteIndex();

	pthread_mutex_unlock(&mutexlock);
}

extern "C" void Java_magiclib_core_NativeControl_nativeDpad(JNIEnv * env, jobject obj, jint x, jint y, jint action, jint button)
{
	if (!inputLockInitialized) {
		useInputLocks = 1;
		inputLockInitialized = true;
	}
	pthread_mutex_lock(&mutexlock);

	InputEvent	*event = inputQueue.getWriteEvent();
	if (!event) {
		pthread_mutex_unlock(&mutexlock);
		return;
	}

	event->eventType = SDL_NOEVENT;

	switch (action) {
		case 0:
			event->eventType = SDL_JOYBUTTONDOWN;
			event->keycode = button;
			break;
		case 1:
			event->eventType = SDL_JOYBUTTONUP;
			event->keycode = button;
			break;
		case 2:
			event->eventType = SDL_DPADAXISMOTION;
			event->x = x;
			event->y = y;
			break;
	}

	if 	(event->eventType != SDL_NOEVENT)
		inputQueue.incWriteIndex();

	pthread_mutex_unlock(&mutexlock);
}

extern "C" void Java_magiclib_core_NativeControl_nativeMouse(JNIEnv * env, jobject obj, jint x, jint y, jint down_x, jint down_y, jint action, jint button)
{
	int useLock = useInputLocks;
	if (useLock) {
		pthread_mutex_lock(&mutexlock);
	}
	InputEvent *event = inputQueue.getWriteEvent();
	if (!event) {
		if (useLock) {
			pthread_mutex_unlock(&mutexlock);
		}
		return;
	}

	event->eventType = SDL_NOEVENT;

	switch (action) {
		case 0:
			event->eventType = SDL_MOUSEBUTTONDOWN;
			event->down_x = down_x;
			event->down_y = down_y;
			event->keycode = button;
			break;
		case 1:
			event->eventType = SDL_MOUSEBUTTONUP;
			event->keycode = button;
			break;
		case 2:
			event->eventType = SDL_MOUSEMOTION;
			event->down_x = down_x;
			event->down_y = down_y;
			event->x = x;
			event->y = y;

			break;
	}

	if 	(event->eventType != SDL_NOEVENT)
		inputQueue.incWriteIndex();

	if (useLock) {
		pthread_mutex_unlock(&mutexlock);
	}
}

void Android_AudioGetBuffer() {
	if ((loadf != 0) && (loadf->abort == 0) && (gEnv != 0))
		loadf->audioBuffer = (jshortArray)gEnv->CallObjectMethod( JavaCallbackThread, JavaAudioGetBuffer );
}

extern bool CPU_CycleAutoAdjust;
extern bool CPU_SkipCycleAutoAdjust;

void Android_AudioWriteBuffer()
{
	short size = 0;

	if ((loadf != 0) && (loadf->abort == 0) && (gEnv != 0)) {

		if ((mixerCallBack != 0) && (loadf->audioBuffer != 0)) {
			jboolean isCopy = JNI_TRUE;
			jsize len = gEnv->GetArrayLength(loadf->audioBuffer)*((CPU_CycleAutoAdjust||CPU_SkipCycleAutoAdjust)?0.8:1);
			jshort *audioBuffer = gEnv->GetShortArrayElements(loadf->audioBuffer, &isCopy);

			//if (isCopy != JNI_TRUE ) {
			size = 0;

			(*mixerCallBack)(&size, (unsigned char *)audioBuffer, (len << 1));

			//}
			gEnv->ReleaseShortArrayElements(loadf->audioBuffer, audioBuffer, 0);

			if (size > 0) {
				gEnv->CallVoidMethod( JavaCallbackThread, JavaAudioWriteBuffer, (int)size);
			}
		}
	}
}

int Android_OpenAudio(int rate, int channels, int encoding, int bufSize)
{
	if ((loadf != 0) && (loadf->abort == 0) && (gEnv != 0))
		return gEnv->CallIntMethod( JavaCallbackThread, JavaAudioInit, rate, channels, encoding, bufSize );
	else
		return 0;
}

InputEvent * Android_PollEvent()
{
	//if there is only one reader, then probably I don't need use locks in this function
	int useLock = 0;//useInputLocks;
	if (useLock) {
		//SDL_LockMutex(input_mutex);
		pthread_mutex_lock(&mutexlock);
	}

	InputEvent *event = inputQueue.getReadEvent();
	if (event) {
		inputQueue.incReadIndex();
		if (useLock) {
			//SDL_UnlockMutex(input_mutex);
			pthread_mutex_unlock(&mutexlock);
		}
		return event;
	}
	if (useLock) {
		//SDL_UnlockMutex(input_mutex);
		pthread_mutex_unlock(&mutexlock);
	}
	return 0;
}

void Android_Exit() {
	if (gEnv != 0) {
		JNIEnv *env = gEnv;

		gEnv = NULL;

		env->CallVoidMethod( JavaCallbackThread, JavaExit);

		env->DeleteGlobalRef(JavaCallbackThread);
		JavaCallbackThread = NULL;
	}
}

void Android_SetVideoMode(int width, int height, int depth) {
//__android_log_write(ANDROID_LOG_INFO, "MagicBox", "Android_SetVideoMode");
	if ((loadf != 0) && (gEnv != 0)) {
		if ((width != loadf->width) || (height != loadf->height)) {
			jobject bmph = gEnv->CallObjectMethod( JavaCallbackThread, JavaVideoSetMode, width, height );

			//if (bmph)
			{
				loadf->bmph = bmph;
				loadf->width = width;
				loadf->height = height;
				loadf->rowbytes = width*2;

#if !USE_JNIGRAPHIC
				//locnet, 2011-04-28, support 2.1 or below
				loadf->videoBuffer = Android_VideoGetBuffer();
#endif
			}
		}
	}
}

void Android_MouseSetVideoMode(int mode, int width, int height)
{
	if (gEnv != 0)
	{
		gEnv->CallVoidMethod( JavaCallbackThread, JavaMouseSetVideoMode, mode, width, height );
	}
}

char *Android_VideoGetBuffer() {
	char * result = 0;

	jobject videoBuffer = gEnv->CallObjectMethod( JavaCallbackThread, JavaVideoGetBuffer );
	if (videoBuffer != 0) {
		result = (char *)gEnv->GetDirectBufferAddress(videoBuffer);
		gEnv->DeleteLocalRef(videoBuffer);
		videoBuffer = 0;
	}

	return result;
}

void	Android_LockSurface() {
	if ((gEnv != 0) && (loadf != 0) && (loadf->bmph != 0)) {
#if USE_JNIGRAPHIC
		void* pixels = 0;

		AndroidBitmap_lockPixels(gEnv, loadf->bmph, &pixels);
		loadf->videoBuffer = (char *)pixels;
#else
		//locnet, 2011-04-28, support 2.1 or below
		if (loadf->videoBuffer == 0)
			loadf->videoBuffer = Android_VideoGetBuffer();
#endif
	}
}

void	Android_UnlockSurface(int startLine, int endLine)
{
	if ((gEnv != 0) && (loadf != 0) && (loadf->bmph != 0) && (loadf->videoBuffer != 0)) {
#if USE_JNIGRAPHIC
		AndroidBitmap_unlockPixels(gEnv, loadf->bmph);
		loadf->videoBuffer = 0;
#endif
	}

	if ((loadf != 0) && (loadf->abort == 0) && (gEnv != 0) && (endLine > startLine))
		gEnv->CallVoidMethod( JavaCallbackThread, JavaVideoRedraw, loadf->width, loadf->height, startLine, endLine );
}

void 	Android_ResetScreen()
{
	if ((gEnv != 0) && (loadf != 0) && (loadf->bmph != 0)) {
		void* pixels = 0;

#if USE_JNIGRAPHIC
		AndroidBitmap_lockPixels(gEnv, loadf->bmph, &pixels);
#else
		//locnet, 2011-04-28, support 2.1 or below
		pixels = Android_VideoGetBuffer();
#endif

		if (pixels != 0)
			memset(pixels, 0, loadf->width*loadf->height*2);

#if USE_JNIGRAPHIC
		AndroidBitmap_unlockPixels(gEnv, loadf->bmph);
#endif
	}
}


int getKeyFromUnicode(int unicode, jint &shift)
{
	switch (unicode) {
		case '!': case '@':	case '#': case '$': case '%': case '^': case '&': case '*':	case '(': case ')':
		case '~': case '_': case '+': case '?': case '{': case '}': case ':': case '"': case '<': case '>':
		case '|':
			shift = KEYBOARD_SHIFT_FLAG;
			break;
		default:
			if ((unicode >= 'A') && (unicode <= 'Z')) {
				shift = KEYBOARD_SHIFT_FLAG;
			}
			break;
	}
	int dosboxKeycode = KBD_NONE;
	switch (unicode){
		case '!': case '1': dosboxKeycode = KBD_1; break;
		case '@': case '2': dosboxKeycode = KBD_2; break;
		case '#': case '3': dosboxKeycode = KBD_3; break;
		case '$': case '4': dosboxKeycode = KBD_4; break;
		case '%': case '5': dosboxKeycode = KBD_5; break;
		case '^': case '6': dosboxKeycode = KBD_6; break;
		case '&': case '7': dosboxKeycode = KBD_7; break;
		case '*': case '8': dosboxKeycode = KBD_8; break;
		case '(': case '9': dosboxKeycode = KBD_9; break;
		case ')': case '0': dosboxKeycode = KBD_0; break;
		case 'a': case 'A':dosboxKeycode = KBD_a; break;
		case 'b': case 'B': dosboxKeycode = KBD_b; break;
		case 'c': case 'C': dosboxKeycode = KBD_c; break;
		case 'd': case 'D': dosboxKeycode = KBD_d; break;
		case 'e': case 'E': dosboxKeycode = KBD_e; break;
		case 'f': case 'F': dosboxKeycode = KBD_f; break;
		case 'g': case 'G': dosboxKeycode = KBD_g; break;
		case 'h': case 'H': dosboxKeycode = KBD_h; break;
		case 'i': case 'I': dosboxKeycode = KBD_i; break;
		case 'j': case 'J': dosboxKeycode = KBD_j; break;
		case 'k': case 'K': dosboxKeycode = KBD_k; break;
		case 'l': case 'L': dosboxKeycode = KBD_l; break;
		case 'm': case 'M': dosboxKeycode = KBD_m; break;
		case 'n': case 'N': dosboxKeycode = KBD_n; break;
		case 'o': case 'O': dosboxKeycode = KBD_o; break;
		case 'p': case 'P': dosboxKeycode = KBD_p; break;
		case 'q': case 'Q': dosboxKeycode = KBD_q; break;
		case 'r': case 'R': dosboxKeycode = KBD_r; break;
		case 's': case 'S': dosboxKeycode = KBD_s; break;
		case 't': case 'T': dosboxKeycode = KBD_t; break;
		case 'u': case 'U': dosboxKeycode = KBD_u; break;
		case 'v': case 'V': dosboxKeycode = KBD_v; break;
		case 'w': case 'W': dosboxKeycode = KBD_w; break;
		case 'x': case 'X': dosboxKeycode = KBD_x; break;
		case 'y': case 'Y': dosboxKeycode = KBD_y; break;
		case 'z': case 'Z': dosboxKeycode = KBD_z; break;
		case 0x08: dosboxKeycode = KBD_backspace; break;
		case 0x09: dosboxKeycode = KBD_tab; break;
		case 0x20: dosboxKeycode = KBD_space; break;
		case 0x0A: dosboxKeycode = KBD_enter; break;
		case '~': case '`': dosboxKeycode = KBD_grave; break;
		case '_': case '-': dosboxKeycode = KBD_minus; break;
		case '+': case '=': dosboxKeycode = KBD_equals; break;
		case '?': case '/': dosboxKeycode = KBD_slash; break;
		case '{': case '[': dosboxKeycode = KBD_leftbracket; break;
		case '}': case ']': dosboxKeycode = KBD_rightbracket; break;
		case ':': case ';': dosboxKeycode = KBD_semicolon; break;
		case '"': case '\'': dosboxKeycode = KBD_quote; break;
		case '<': case ',': dosboxKeycode = KBD_comma; break;
		case '>': case '.': dosboxKeycode = KBD_period; break;
		case '|': case '\\': dosboxKeycode = KBD_backslash; break;
		case 0x1B: dosboxKeycode = KBD_esc; break;
		case 0x1C: dosboxKeycode = KBD_left; break;
		case 0x1D: dosboxKeycode = KBD_right; break;
		case 0x1E: dosboxKeycode = KBD_up; break;
		case 0x1F: dosboxKeycode = KBD_down; break;
		default: dosboxKeycode = KBD_NONE; break;
	}
	return dosboxKeycode;
}
