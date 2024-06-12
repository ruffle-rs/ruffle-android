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

#define CPU_AUTODETERMINE_NONE		0x00
#define CPU_AUTODETERMINE_CORE		0x01
#define CPU_AUTODETERMINE_CYCLES	0x02

#include <jni.h>
#include <stdlib.h>
#include "config.h"
#include "loader.h"
#include "render.h"
#include <string>
//#include <android/log.h>
#include "mouse.h"

int dosbox_main(int argc, const char* argv[]);
void DOSBOX_UnlockSpeed( bool pressed );
void swapInNextDisk(bool pressed);

extern Bit32s androidWrapperVersion;
extern struct loader_config myLoader;
extern struct loader_config *loadf;

char start_command[256]="";
char dosbox_config[256]="";

bool enableSoundHack=false;

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeStart(JNIEnv * env, jobject obj, jobject buffer, jint width, jint height, jstring safPath, jint ver)
{
	androidWrapperVersion = ver;

	Android_Init(env, obj, buffer, width, height, safPath);

	//__android_log_print(ANDROID_LOG_INFO, "MagicBox", "dosbox_config : %s",  dosbox_config);

	const char * argv[] = { "dosbox", "-conf", dosbox_config, "-c", start_command };
	//const char * argv[] = { "dosbox", "-conf", "/sdcard/dosbox.conf", "-c", start_command };
	dosbox_main((!start_command[0])?3:5, argv);

    //__android_log_print(ANDROID_LOG_INFO, "MagicBox", "Android_ShutDown");
	Android_ShutDown(false);
}

extern Render_t render;
extern bool CPU_CycleAutoAdjust;
extern bool CPU_SkipCycleAutoAdjust;
extern Bit32s CPU_CycleMax;
extern Bit32s CPU_CycleLimit;
extern Bit32s CPU_OldCycleMax;
extern Bit32s CPU_CyclePercUsed;
extern Bitu CPU_AutoDetermineMode;

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeSetOption(JNIEnv * env, jobject obj, jint option, jint value, jobject value2)
{
	switch (option)
	{
		case 1:
		{
			myLoader.soundEnable = value;
			enableSound = (value != 0);
			break;
		}
		case 2:
		{
			myLoader.memsize = value;
			break;
		}
		case 3:
		{
			strcpy(start_command, (env)->GetStringUTFChars((jstring)value2, 0));
			break;
		}
		case 4:
		{
			enableSoundHack = (value != 0);
			break;
		}
		case 5:
		{
			strcpy(dosbox_config, (env)->GetStringUTFChars((jstring)value2, 0));
			break;
		}
		case 10:
		{
			CPU_CycleAutoAdjust = false;
			CPU_CycleMax = value;
			CPU_OldCycleMax = value;
			CPU_SkipCycleAutoAdjust = false;
			CPU_CycleLimit = value;
			myLoader.cycles = value;
			break;
		}
		case 11:
		{
			myLoader.frameskip = value;
			render.frameskip.max = value;
			break;
		}
		case 12:
		{
			myLoader.refreshHack = value;
			enableRefreshHack = (value != 0);
			break;
		}
		case 13:
		{
			myLoader.cycleHack = value;
			enableCycleHack = (value != 0);
			break;
		}
		case 14:
		{
			if (!CPU_CycleAutoAdjust)
			{
					//max
					CPU_AutoDetermineMode=CPU_AUTODETERMINE_NONE;

					CPU_CyclePercUsed=100;
					CPU_CycleMax=0;
					CPU_CycleAutoAdjust=true;
					CPU_CycleLimit=-1;
			}

			break;

		}
		case 15:
		{
			swapInNextDisk(true);
			break;
		}
		case 16: {
			myLoader.calcSensitivity = (value == 100)? 0 : 1;

			if (value != 100) {
				myLoader.sensitivity = value / 100.0f;
			}
			break;
		}
		case 17:
			DOSBOX_UnlockSpeed((value != 0));
			break;/*
		case 15:
			JOYSTICK_Enable(0, (value != 0));
			break;*/
		/*case 51:
			swapInNextDisk(true);
			break;*/
	}
}
void GetJStringContent(JNIEnv *AEnv, jstring AStr, std::string &ARes)
{
  if (!AStr)
  {
    ARes.clear();
    return;
  }

  const char *s = AEnv->GetStringUTFChars(AStr,NULL);
  ARes=s;
  AEnv->ReleaseStringUTFChars(AStr,s);
}

extern void SaveGameState(const std::string &path);
extern void LoadGameState(const std::string &path);

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeSaveState(JNIEnv * env, jobject obj, jstring path)
{
	std::string str;

	GetJStringContent(env, path, str);

	SaveGameState(str);
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeLoadState(JNIEnv * env, jobject obj, jstring path)
{
//	__android_log_print(ANDROID_LOG_INFO, "MagicBox", "load");

	std::string str;

	GetJStringContent(env, path, str);

	LoadGameState(str);
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeMouseMax(JNIEnv * env, jobject obj, jboolean enabled, jint max_width, jint max_height)
{
	Mouse_SetMouseMax(enabled, max_width, max_height);
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeSetAbsoluteMouseType(JNIEnv * env, jobject obj, jint type)
{
	Mouse_SetAbsoluteType(type);
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeMouseRoundMaxByVideoMode(JNIEnv * env, jobject obj, jboolean enabled)
{
	Mouse_SetRoundMaxByVideMode(enabled);
}

extern "C" jint Java_bruenor_magicbox_MagicLauncher_nativeGetMouseVideoWidth(JNIEnv * env, jobject obj)
{
	return Mouse_GetVideoWidth();
}

extern "C" jint Java_bruenor_magicbox_MagicLauncher_nativeGetMouseVideoHeight(JNIEnv * env, jobject obj)
{
	return Mouse_GetVideoHeight();
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeInit(JNIEnv * env, jobject obj)
{
	loadf = 0;
	myLoader.memsize = 2;
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;

	myLoader.abort = 0;
	myLoader.pause = 0;

	myLoader.frameskip = 0;
	myLoader.cycles = 1500;
	myLoader.soundEnable = 1;
	myLoader.cycleHack = 1;
	myLoader.refreshHack = 1;
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativePause(JNIEnv * env, jobject obj, jint state)
{
	if ((state == 0) || (state == 1))
		myLoader.pause = state;
	else
		myLoader.pause = (myLoader.pause)?0:1;
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeStop(JNIEnv * env, jobject obj)
{
	myLoader.abort = 1;
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeForceStop(JNIEnv * env, jobject obj)
{
	Android_ShutDown(true);
}

extern "C" void Java_bruenor_magicbox_MagicLauncher_nativeShutDown(JNIEnv * env, jobject obj)
{
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;
}

extern "C" jint Java_bruenor_magicbox_MagicLauncher_nativeGetLibArchitecture(JNIEnv * env, jobject obj)
{
	#if (DBXLIB_ARMEABI)
		return 0;	
	#else	
		#if (DBXLIB_ARMEABI_V7)		
			return 1;
		#else
			#if (DBXLIB_X86)
				return 2;
			#else
			    return -1;
			#endif
		#endif
	#endif	
}