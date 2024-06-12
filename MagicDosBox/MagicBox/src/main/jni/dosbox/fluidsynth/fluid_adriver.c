/* FluidSynth - A Software Synthesizer
 *
 * Copyright (C) 2003  Peter Hanappe and others.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307, USA
 */

#include "fluid_adriver.h"
#include "fluid_settings.h"

/*
 * fluid_adriver_definition_t
 */

typedef struct _fluid_audriver_definition_t
{
  char* name;
  fluid_audio_driver_t* (*new)(fluid_settings_t* settings, fluid_synth_t* synth);
  fluid_audio_driver_t* (*new2)(fluid_settings_t* settings,
				fluid_audio_func_t func,
				void* data);
  int (*free)(fluid_audio_driver_t* driver);
  void (*settings)(fluid_settings_t* settings);
} fluid_audriver_definition_t;


#if PULSE_SUPPORT
fluid_audio_driver_t* new_fluid_pulse_audio_driver(fluid_settings_t* settings,
						   fluid_synth_t* synth);
fluid_audio_driver_t* new_fluid_pulse_audio_driver2(fluid_settings_t* settings,
						    fluid_audio_func_t func, void* data);
int delete_fluid_pulse_audio_driver(fluid_audio_driver_t* p);
void fluid_pulse_audio_driver_settings(fluid_settings_t* settings);
#endif

#if ALSA_SUPPORT
fluid_audio_driver_t* new_fluid_alsa_audio_driver(fluid_settings_t* settings,
						  fluid_synth_t* synth);
fluid_audio_driver_t* new_fluid_alsa_audio_driver2(fluid_settings_t* settings,
						 fluid_audio_func_t func, void* data);
int delete_fluid_alsa_audio_driver(fluid_audio_driver_t* p);
void fluid_alsa_audio_driver_settings(fluid_settings_t* settings);
#endif

#if OSS_SUPPORT
fluid_audio_driver_t* new_fluid_oss_audio_driver(fluid_settings_t* settings,
						 fluid_synth_t* synth);
fluid_audio_driver_t* new_fluid_oss_audio_driver2(fluid_settings_t* settings,
						fluid_audio_func_t func, void* data);
int delete_fluid_oss_audio_driver(fluid_audio_driver_t* p);
void fluid_oss_audio_driver_settings(fluid_settings_t* settings);
#endif

#if COREAUDIO_SUPPORT
fluid_audio_driver_t* new_fluid_core_audio_driver(fluid_settings_t* settings,
						  fluid_synth_t* synth);
fluid_audio_driver_t* new_fluid_core_audio_driver2(fluid_settings_t* settings,
						      fluid_audio_func_t func,
						      void* data);
int delete_fluid_core_audio_driver(fluid_audio_driver_t* p);
void fluid_core_audio_driver_settings(fluid_settings_t* settings);
#endif

#if DSOUND_SUPPORT
fluid_audio_driver_t* new_fluid_dsound_audio_driver(fluid_settings_t* settings,
						  fluid_synth_t* synth);
int delete_fluid_dsound_audio_driver(fluid_audio_driver_t* p);
void fluid_dsound_audio_driver_settings(fluid_settings_t* settings);
#endif

#if PORTAUDIO_SUPPORT
void fluid_portaudio_driver_settings (fluid_settings_t *settings);
fluid_audio_driver_t* new_fluid_portaudio_driver(fluid_settings_t* settings,
						 fluid_synth_t* synth);
int delete_fluid_portaudio_driver(fluid_audio_driver_t* p);
#endif

#if JACK_SUPPORT
fluid_audio_driver_t* new_fluid_jack_audio_driver(fluid_settings_t* settings, fluid_synth_t* synth);
fluid_audio_driver_t* new_fluid_jack_audio_driver2(fluid_settings_t* settings,
						 fluid_audio_func_t func, void* data);
int delete_fluid_jack_audio_driver(fluid_audio_driver_t* p);
void fluid_jack_audio_driver_settings(fluid_settings_t* settings);
#endif

#if SNDMAN_SUPPORT
fluid_audio_driver_t* new_fluid_sndmgr_audio_driver(fluid_settings_t* settings,
						  fluid_synth_t* synth);
fluid_audio_driver_t* new_fluid_sndmgr_audio_driver2(fluid_settings_t* settings,
						   fluid_audio_func_t func,
						   void* data);
int delete_fluid_sndmgr_audio_driver(fluid_audio_driver_t* p);
#endif

#if DART_SUPPORT
fluid_audio_driver_t* new_fluid_dart_audio_driver(fluid_settings_t* settings,
                          fluid_synth_t* synth);
int delete_fluid_dart_audio_driver(fluid_audio_driver_t* p);
void fluid_dart_audio_driver_settings(fluid_settings_t* settings);
#endif

#define AUFILE_SUPPORT 1
#if AUFILE_SUPPORT
fluid_audio_driver_t* new_fluid_file_audio_driver(fluid_settings_t* settings,
						  fluid_synth_t* synth);
int delete_fluid_file_audio_driver(fluid_audio_driver_t* p);
void fluid_file_audio_driver_settings(fluid_settings_t* settings);
#endif

/* Available audio drivers, listed in order of preference */
fluid_audriver_definition_t fluid_audio_drivers[] = {
#if JACK_SUPPORT
  { "jack",
    new_fluid_jack_audio_driver,
    new_fluid_jack_audio_driver2,
    delete_fluid_jack_audio_driver,
    fluid_jack_audio_driver_settings },
#endif
#if ALSA_SUPPORT
  { "alsa",
    new_fluid_alsa_audio_driver,
    new_fluid_alsa_audio_driver2,
    delete_fluid_alsa_audio_driver,
    fluid_alsa_audio_driver_settings },
#endif
#if OSS_SUPPORT
  { "oss",
    new_fluid_oss_audio_driver,
    new_fluid_oss_audio_driver2,
    delete_fluid_oss_audio_driver,
    fluid_oss_audio_driver_settings },
#endif
#if PULSE_SUPPORT
  { "pulseaudio",
    new_fluid_pulse_audio_driver,
    new_fluid_pulse_audio_driver2,
    delete_fluid_pulse_audio_driver,
    fluid_pulse_audio_driver_settings },
#endif
#if COREAUDIO_SUPPORT
  { "coreaudio",
    new_fluid_core_audio_driver,
    new_fluid_core_audio_driver2,
    delete_fluid_core_audio_driver,
    fluid_core_audio_driver_settings },
#endif
#if DSOUND_SUPPORT
  { "dsound",
    new_fluid_dsound_audio_driver,
    NULL,
    delete_fluid_dsound_audio_driver,
    fluid_dsound_audio_driver_settings },
#endif
#if PORTAUDIO_SUPPORT
  { "portaudio",
    new_fluid_portaudio_driver,
    NULL,
    delete_fluid_portaudio_driver,
    fluid_portaudio_driver_settings },
#endif
#if SNDMAN_SUPPORT
  { "sndman",
    new_fluid_sndmgr_audio_driver,
    new_fluid_sndmgr_audio_driver2,
    delete_fluid_sndmgr_audio_driver,
    NULL },
#endif
#if DART_SUPPORT
  { "dart",
    new_fluid_dart_audio_driver,
    NULL,
    delete_fluid_dart_audio_driver,
    fluid_dart_audio_driver_settings },
#endif
#if AUFILE_SUPPORT
  { "file",
    new_fluid_file_audio_driver,
    NULL,
    delete_fluid_file_audio_driver,
    fluid_file_audio_driver_settings },
#endif
  { NULL, NULL, NULL, NULL, NULL }
};




void fluid_audio_driver_settings(fluid_settings_t* settings)
{
  int i;

  fluid_settings_register_str(settings, "audio.sample-format", "16bits", 0, NULL, NULL);
  fluid_settings_add_option(settings, "audio.sample-format", "16bits");
  fluid_settings_add_option(settings, "audio.sample-format", "float");

  fluid_settings_register_int(settings, "audio.output-channels", 2, 2, 32, 0, NULL, NULL);
  fluid_settings_register_int(settings, "audio.input-channels", 0, 0, 2, 0, NULL, NULL);


#if defined(WIN32)
  fluid_settings_register_int(settings, "audio.period-size", 512, 64, 8192, 0, NULL, NULL);
  fluid_settings_register_int(settings, "audio.periods", 8, 2, 64, 0, NULL, NULL);
#elif defined(MACOS9)
  fluid_settings_register_int(settings, "audio.period-size", 64, 64, 8192, 0, NULL, NULL);
  fluid_settings_register_int(settings, "audio.periods", 8, 2, 64, 0, NULL, NULL);
#else
  fluid_settings_register_int(settings, "audio.period-size", 64, 64, 8192, 0, NULL, NULL);
  fluid_settings_register_int(settings, "audio.periods", 16, 2, 64, 0, NULL, NULL);
#endif

  /* Set the default driver */
#if JACK_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "jack", 0, NULL, NULL);
#elif ALSA_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "alsa", 0, NULL, NULL);
#elif PULSE_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "pulseaudio", 0, NULL, NULL);
#elif OSS_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "oss", 0, NULL, NULL);
#elif COREAUDIO_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "coreaudio", 0, NULL, NULL);
#elif DSOUND_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "dsound", 0, NULL, NULL);
#elif SNDMAN_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "sndman", 0, NULL, NULL);
#elif PORTAUDIO_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "portaudio", 0, NULL, NULL);
#elif DART_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "dart", 0, NULL, NULL);
#elif AUFILE_SUPPORT
  fluid_settings_register_str(settings, "audio.driver", "file", 0, NULL, NULL);
#else
  fluid_settings_register_str(settings, "audio.driver", "", 0, NULL, NULL);
#endif

  /* Add all drivers to the list of options */
#if PULSE_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "pulseaudio");
#endif
#if ALSA_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "alsa");
#endif
#if OSS_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "oss");
#endif
#if COREAUDIO_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "coreaudio");
#endif
#if DSOUND_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "dsound");
#endif
#if SNDMAN_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "sndman");
#endif
#if PORTAUDIO_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "portaudio");
#endif
#if JACK_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "jack");
#endif
#if DART_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "dart");
#endif
#if AUFILE_SUPPORT
  fluid_settings_add_option(settings, "audio.driver", "file");
#endif

  for (i = 0; fluid_audio_drivers[i].name != NULL; i++) {
    if (fluid_audio_drivers[i].settings != NULL) {
      fluid_audio_drivers[i].settings(settings);
    }
  }
}


/**
 * Create a new audio driver.
 * @param settings Configuration settings used to select and create the audio
 *   driver.
 * @param synth Synthesizer instance for which the audio driver is created for.
 * @return The new audio driver instance.
 *
 * Creates a new audio driver for a given 'synth' instance with a defined set
 * of configuration 'settings'.
 */
fluid_audio_driver_t*
new_fluid_audio_driver(fluid_settings_t* settings, fluid_synth_t* synth)
{
  int i;
  fluid_audio_driver_t* driver = NULL;
  char* name;

  fluid_settings_getstr(settings, "audio.driver", &name);

  for (i = 0; fluid_audio_drivers[i].name != NULL; i++) {
    if (fluid_settings_str_equal(settings, "audio.driver", fluid_audio_drivers[i].name)) {
      FLUID_LOG(FLUID_DBG, "Using '%s' audio driver", fluid_audio_drivers[i].name);
      driver = (*fluid_audio_drivers[i].new)(settings, synth);
      if (driver) {
	driver->name = fluid_audio_drivers[i].name;
      }
      return driver;
    }
  }

  FLUID_LOG(FLUID_ERR, "Couldn't find the requested audio driver: %s", name);
  return NULL;
}

/**
 * Create a new audio driver.
 * @param settings Configuration settings used to select and create the audio
 *   driver.
 * @param func Function called to fill audio buffers for audio playback
 * @param data User defined data pointer to pass to 'func'
 * @return The new audio driver instance.
 *
 * Like new_fluid_audio_driver() but allows for custom audio processing before
 * audio is sent to audio driver.  It is the responsibility of the callback
 * 'func' to render the audio into the buffers.
 * NOTE: Not as efficient as new_fluid_audio_driver().
 */
fluid_audio_driver_t*
new_fluid_audio_driver2(fluid_settings_t* settings, fluid_audio_func_t func, void* data)
{
  int i;
  fluid_audio_driver_t* driver = NULL;
  char* name;

  fluid_settings_getstr(settings, "audio.driver", &name);

  for (i = 0; fluid_audio_drivers[i].name != NULL; i++) {
    if (fluid_settings_str_equal(settings, "audio.driver", fluid_audio_drivers[i].name) &&
	(fluid_audio_drivers[i].new2 != NULL)) {
      FLUID_LOG(FLUID_DBG, "Using '%s' audio driver", fluid_audio_drivers[i].name);
      driver = (*fluid_audio_drivers[i].new2)(settings, func, data);
      if (driver) {
	driver->name = fluid_audio_drivers[i].name;
      }
      return driver;
    }
  }

  FLUID_LOG(FLUID_ERR, "Couldn't find the requested audio driver: %s", name);
  return NULL;
}

/**
 * Deletes an audio driver instance.
 * @param driver Audio driver instance to delete
 *
 * Shuts down an audio driver and deletes its instance.
 */
void
delete_fluid_audio_driver(fluid_audio_driver_t* driver)
{
  int i;

  for (i = 0; fluid_audio_drivers[i].name != NULL; i++) {
    if (fluid_audio_drivers[i].name == driver->name) {
      fluid_audio_drivers[i].free(driver);
      return;
    }
  }
}
