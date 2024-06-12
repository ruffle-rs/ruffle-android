/*
 *  Copyright (C) 2013-2016 Antony Hornacek (magicbox@imejl.sk)
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
package bruenor.magicbox;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;

import magiclib.IO.FileBrowser;
import magiclib.IO.Storages;
import magiclib.controls.Dialog;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;

abstract interface SoundSettingsEventListener
{
	public abstract void onPick(int rate, int blockSize, int preBuffer);
}

class uiSoundSettings extends Dialog
{
	private int rate;
	private int blockSize;
	private int preBuffer;
	
	private ImageButton rateMinus;
	private ImageButton ratePlus;
	private ImageButton blockMinus;
	private ImageButton blockPlus;	
	private ImageButton preBufferMinus;
	private ImageButton preBufferPlus;
	
	private ImageButton confirm;
	private TextView rateInfo;
	private TextView blockInfo;
	private TextView preBufferInfo;
	
	private SoundSettingsEventListener event; 
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.sound_settings_warning,   "aus_msg_warning");
		localize(R.id.sound_settings_freqrate,  "aus_freqrate");
		localize(R.id.sound_settings_blocksize, "aus_blocksize");
		localize(R.id.sound_settings_prebuffer, "aus_prebuffer");
		localize(R.id.sound_settings_prebuffer, "aus_prebuffer");
	};
	
	public uiSoundSettings(Context context, boolean automaticAdjustement)
	{
		super(context);
		
		setContentView(R.layout.sound_settings);
		setCaption("aus_caption");
		
		View.OnClickListener rateEvent = getRateEvent();
		
		rateMinus = (ImageButton)getView().findViewById(R.id.sound_settings_rateminus);
		rateMinus.setOnClickListener(rateEvent);
		
		ratePlus = (ImageButton)getView().findViewById(R.id.sound_settings_rateplus);
		ratePlus.setOnClickListener(rateEvent);
		
		blockMinus = (ImageButton)getView().findViewById(R.id.sound_settings_blockminus);
		blockMinus.setOnClickListener(rateEvent);		
		
		blockPlus = (ImageButton)getView().findViewById(R.id.sound_settings_blockplus);
		blockPlus.setOnClickListener(rateEvent);		
		
		preBufferMinus = (ImageButton)getView().findViewById(R.id.sound_settings_prebufferminus);
		preBufferMinus.setOnClickListener(rateEvent);		
		
		preBufferPlus = (ImageButton)getView().findViewById(R.id.sound_settings_prebufferplus);
		preBufferPlus.setOnClickListener(rateEvent);		
		
		rateInfo = (TextView)getView().findViewById(R.id.sound_settings_ratinfo);
		blockInfo = (TextView)getView().findViewById(R.id.sound_settings_blockinfo);
		preBufferInfo = (TextView)getView().findViewById(R.id.sound_settings_prebufferinfo);
		
		if (!automaticAdjustement)
		{
			TextView warning = (TextView)getView().findViewById(R.id.sound_settings_warning);
			warning.setVisibility(View.GONE);
		}
		
		confirm = (ImageButton)getView().findViewById(R.id.sound_settings_confirm);
		confirm.setOnClickListener(getConfirmEvent());
	}
	
	private View.OnClickListener getConfirmEvent()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if (event != null)
				{
					event.onPick(rate, blockSize, preBuffer);
				}
				
				dismiss();
			}
		};
	}
	
	private View.OnClickListener getRateEvent()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.sound_settings_rateminus:
					{
						switch (rate)
						{
							case 48000:{rate = 44100;break;}
							case 44100:{rate = 32000;break;}
							case 32000:{rate = 22050;break;}
							case 22050:{rate = 16000;break;}
							case 16000:{rate = 11025;break;}
							case 11025:{rate = 8000;break;}
						}
						
						break;
					}
					case R.id.sound_settings_rateplus:
					{
						switch (rate)
						{
							case  8000:{rate = 11025;break;}
							case 11025:{rate = 16000;break;}
							case 16000:{rate = 22050;break;}
							case 22050:{rate = 32000;break;}
							case 32000:{rate = 44100;break;}
							case 44100:{rate = 48000;break;}
						}						
						break;
					}
					case R.id.sound_settings_blockminus:
					{
						switch(blockSize)
						{
							case 4096:{blockSize = 3072;break;}
							case 3072:{blockSize = 2048;break;}
							case 2048:{blockSize = 1024;break;}
							case 1024:{blockSize = 512;break;}
						}
						break;
					}
					case R.id.sound_settings_blockplus:
					{
						switch(blockSize)
						{
							case  512:{blockSize = 1024;break;}
							case 1024:{blockSize = 2048;break;}
							case 2048:{blockSize = 3072;break;}
							case 3072:{blockSize = 4096;break;}
						}
						break;
					}	
					case R.id.sound_settings_prebufferminus:
					{
						preBuffer--;
						
						if (preBuffer < 15)
							preBuffer = 15;
						
						break;
					}					
					case R.id.sound_settings_prebufferplus:
					{
						preBuffer++;
						
						if (preBuffer > 100)
							preBuffer = 100;
						
						break;
					}					
				}
				
				rateInfo.setText("" + rate);
				blockInfo.setText("" + blockSize);
				preBufferInfo.setText("" + preBuffer);
			}
		};
	}

	public void setOnSoundSpeakerEventListener(SoundSettingsEventListener event)
	{
		this.event = event;
	}

	public void setRate(int rate)
	{
		this.rate = rate;
		
		rateInfo.setText("" + rate);
	}

	public int getBlockSize()
	{
		return blockSize;
	}

	public void setBlockSize(int blockSize)
	{
		this.blockSize = blockSize;
		
		blockInfo.setText("" + blockSize);
	}

	public int getPreBuffer()
	{
		return preBuffer;
	}

	public void setPreBuffer(int preBuffer)
	{
		this.preBuffer = preBuffer;
		
		preBufferInfo.setText("" + preBuffer);
	}
}

class GUSSettings extends Dialog
{
	public abstract interface OnGUSEventListener
	{
		public abstract void onSave(boolean enabled, String path);
	}

	@Override
	public void onSetLocalizedLayout()
	{
		localize(R.id.gus_info, "gus_info");
		localize(R.id.gus_on, "common_enabled");
		localize(R.id.gus_choose_path, "common_choose");
	}

	private OnGUSEventListener event;
	private Button pickDir;
	private EditText dir;
	private CheckBox isEnabled;
	private View.OnClickListener onClick;
	private File rootPath;

	public GUSSettings(Context context, File rootPath, boolean enabled, String path) {
		super(context);

		this.rootPath = rootPath;

		setContentView(R.layout.gus);
		setCaption("gus_caption");

		pickDir = (Button)findViewById(R.id.gus_choose_path);
		pickDir.setOnClickListener(getOnClick());

		dir = (EditText)findViewById(R.id.gus_path);

		if (path == null || path.equals(""))
		{
			File p = new File(rootPath, "ULTRASND");
			p.mkdirs();

			dir.setText("C:\\ULTRASND");
		}
		else
			dir.setText(path);

		isEnabled = (CheckBox)findViewById(R.id.gus_on);
		isEnabled.setChecked(enabled);

		findViewById(R.id.gus_confirm).setOnClickListener(getOnClick());
	}

	private View.OnClickListener getOnClick()
	{
		if (onClick != null)
			return onClick;

		onClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch(v.getId()) {
					case R.id.gus_confirm:
					{
						if (event != null) {
							event.onSave(isEnabled.isChecked(), dir.getText().toString());
						}
						dismiss();
						break;
					}
					case R.id.gus_choose_path:
					{
						FileBrowser fb = new FileBrowser(getContext(), rootPath.getAbsolutePath(), null, true);
						fb.setCaption("fb_caption_choose_folder");
						fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
						{
							@Override
							public void onPick(String selected)
							{
								String d = rootPath.getAbsolutePath();
								String dirPath = selected.substring(d.length());

								dirPath = dirPath.replace("/", "\\");
								if (dirPath.startsWith("\\"))
								{
									dirPath = "C:" + dirPath;
								}
								else
								{
									dirPath = "C:\\" + dirPath;
								}

								dir.setText(dirPath);
							}
						});

						fb.show();

						break;
					}
				}
			}
		};

		return onClick;
	}

	public void setOnGUSEventListener(OnGUSEventListener event)
	{
		this.event = event;
	}
}

class MidiMT32Settings extends Dialog
{
	public abstract interface OnMidiMT32EventListener
	{
		public abstract void onSave(String mt32RomDir,
									boolean mt32RunInThread,
									int mt32Analog,
									int mt32Dac,
									int mt32Prebuffer);
	}

	private Button pickROM;
	private EditText mt32RomDir;
	private CheckBox mt32RunMidiInThread;
	private View.OnClickListener onClick;
	private TextView mt32AnalogInfo;
	private TextView mt32DacInfo;
	private TextView mt32PrebufferInfo;
	private OnMidiMT32EventListener event;

	private int mt32AnalogValue;
	private int mt32DacValue;
	private int mt32PrebufferValue;

	@Override
	public void onSetLocalizedLayout()
	{
		localize(R.id.midi_roms_title, "midi_rom_location");
		localize(R.id.midi_roms_description, "midi_req_warning");
		localize(R.id.midi_mt32inthread, "midi_inthread");
		localize(R.id.midi_performance_warning, "aus_msg_warning");
		localize(R.id.midi_mt32analog, "midi_analog");
		localize(R.id.midi_mt32dac, "midi_dac");
		localize(R.id.midi_mt32prebuffer, "midi_prebuffer");
		localize(R.id.midi_choose_mt32roms, "common_choose");
	}

	public MidiMT32Settings(Context context,
							String mt32RomDir,
							boolean mt32RunInThread,
							int mt32Analog,
							int mt32Dac,
							int mt32Prebuffer,
							boolean automaticPerformance) {
		super(context);

		setContentView(R.layout.midi_mt32);
		setCaption("MT-32");

		mt32AnalogValue = mt32Analog;
		mt32DacValue = mt32Dac;
		mt32PrebufferValue = mt32Prebuffer;

		pickROM = (Button)findViewById(R.id.midi_choose_mt32roms);
		pickROM.setOnClickListener(getOnClick());

		this.mt32RomDir = (EditText)findViewById(R.id.midi_mt32roms);

		File p = new File(AppGlobal.appPath + "MidiROM");
		p.mkdirs();

		if (mt32RomDir == null || mt32RomDir.equals(""))
		{
			this.mt32RomDir.setText(p.getAbsolutePath());
		}
		else
			this.mt32RomDir.setText(mt32RomDir);

		mt32RunMidiInThread = (CheckBox)findViewById(R.id.midi_mt32inthread);
		mt32RunMidiInThread.setChecked(mt32RunInThread);

		if (!automaticPerformance)
			findViewById(R.id.midi_performance_warning).setVisibility(View.GONE);

		findViewById(R.id.midi_mt32analog_minus).setOnClickListener(getOnClick());
		findViewById(R.id.midi_mt32analog_plus).setOnClickListener(getOnClick());

		findViewById(R.id.midi_mt32dac_minus).setOnClickListener(getOnClick());
		findViewById(R.id.midi_mt32dac_plus).setOnClickListener(getOnClick());

		findViewById(R.id.midi_mt32prebuffer_minus).setOnClickListener(getOnClick());
		findViewById(R.id.midi_mt32prebuffer_plus).setOnClickListener(getOnClick());

		findViewById(R.id.midi_confirm).setOnClickListener(getOnClick());

		mt32AnalogInfo = (TextView)findViewById(R.id.midi_mt32analog_info);
		setAnalogInfo();

		mt32DacInfo = (TextView)findViewById(R.id.midi_mt32dac_info);
		setDacInfo();

		mt32PrebufferInfo = (TextView)findViewById(R.id.midi_mt32prebuffer_info);
		mt32PrebufferInfo.setText("" + mt32PrebufferValue);
	}

	private View.OnClickListener getOnClick()
	{
		if (onClick != null)
			return onClick;

		onClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.midi_choose_mt32roms: {
						Storages.onDrivePick(getContext(), false, new Storages.onDrivePickListener() {
							@Override
							public void onPick(String drive) {
								FileBrowser fb;

								fb = new FileBrowser(getContext(), drive, null, true);

								fb.setCaption("fb_caption_choose_folder");
								fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
									@Override
									public void onPick(String selected) {
										mt32RomDir.setText(selected);
									}
								});

								fb.show();
							}
						});
						break;
					}
					case R.id.midi_mt32analog_minus: {
						if (mt32AnalogValue == 0)
							break;

						mt32AnalogValue--;
						setAnalogInfo();
						break;
					}
					case R.id.midi_mt32analog_plus: {
						if (mt32AnalogValue == 3)
							break;

						mt32AnalogValue++;
						setAnalogInfo();
						break;
					}
					case R.id.midi_mt32dac_minus: {
						if (mt32DacValue == 0)
							break;

						mt32DacValue--;
						setDacInfo();
						break;
					}
					case R.id.midi_mt32dac_plus: {
						if (mt32DacValue == 3)
							break;

						mt32DacValue++;
						setDacInfo();
						break;
					}
					case R.id.midi_mt32prebuffer_minus: {
						if (mt32PrebufferValue == 3)
							break;

						switch (mt32PrebufferValue)
						{
							case 200: {
								mt32PrebufferValue = 199;
								break;
							}
							case 199: {
								mt32PrebufferValue = 32;
								break;
							}
							case 32: {
								mt32PrebufferValue = 4;
								break;
							}
							case 4: {
								mt32PrebufferValue = 3;
								break;
							}
						}

						mt32PrebufferInfo.setText("" + mt32PrebufferValue);
						break;
					}
					case R.id.midi_mt32prebuffer_plus: {
						if (mt32PrebufferValue == 200)
							break;

						switch (mt32PrebufferValue)
						{
							case 3: {
								mt32PrebufferValue = 4;
								break;
							}
							case 4: {
								mt32PrebufferValue = 32;
								break;
							}
							case 32: {
								mt32PrebufferValue = 199;
								break;
							}
							case 199: {
								mt32PrebufferValue = 200;
								break;
							}
						}

						mt32PrebufferInfo.setText("" + mt32PrebufferValue);
						break;
					}
					case R.id.midi_confirm:
					{
						if (event != null) {
							event.onSave(mt32RomDir.getText().toString(),
										 mt32RunMidiInThread.isChecked(),
										 mt32AnalogValue,
										 mt32DacValue,
										 mt32PrebufferValue);
						}
						dismiss();
						break;
					}
				}
			}
		};

		return onClick;
	}

	private void setAnalogInfo()
	{
		switch (mt32AnalogValue)
		{
			case 0: {
				mt32AnalogInfo.setText(Localization.getString("midi_analog_digital"));
				break;
			}
			case 1: {
				mt32AnalogInfo.setText(Localization.getString("midi_analog_coarse"));
				break;
			}
			case 2: {
				mt32AnalogInfo.setText(Localization.getString("midi_analog_accurate"));
				break;
			}
			case 3: {
				mt32AnalogInfo.setText(Localization.getString("midi_analog_oversampled"));
				break;
			}
		}
	}

	private void setDacInfo()
	{
		switch (mt32DacValue)
		{
			case 0: {
				mt32DacInfo.setText(Localization.getString("midi_dac_nice"));
				break;
			}
			case 1: {
				mt32DacInfo.setText(Localization.getString("midi_dac_pure"));
				break;
			}
			case 2: {
				mt32DacInfo.setText(Localization.getString("midi_dac_gen1"));
				break;
			}
			case 3: {
				mt32DacInfo.setText(Localization.getString("midi_dac_gen2"));
				break;
			}
		}
	}

	public void setOnMidiMT32EventListener(OnMidiMT32EventListener event)
	{
		this.event = event;
	}
}

class MidiSynthSettings extends Dialog
{
	public abstract interface OnMidiSynthEventListener
	{
		public abstract void onSave(String synthRomPath);
	}

	@Override
	public void onSetLocalizedLayout()
	{
		localize(R.id.midi_synth_description, "midi_sf");
		localize(R.id.midi_choose_soundfont, "common_choose");
	}

	private Button pickROM;
	private View.OnClickListener onClick;
	private OnMidiSynthEventListener event;
	private CheckBox synthon;
	private EditText synthRomPath;

	public MidiSynthSettings(Context context, String synthROMPath) {
		super(context);

		setContentView(R.layout.midi_synth);
		setCaption("Synth");

		pickROM = (Button)findViewById(R.id.midi_choose_soundfont);
		pickROM.setOnClickListener(getOnClick());

		this.synthRomPath = (EditText)findViewById(R.id.midi_soundfont);

		if (synthROMPath == null)
		{
			synthROMPath = "";
		}

		this.synthRomPath.setText(synthROMPath);

		findViewById(R.id.midi_confirm).setOnClickListener(getOnClick());
	}

	private View.OnClickListener getOnClick()
	{
		if (onClick != null)
			return onClick;

		onClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.midi_choose_soundfont: {
						Storages.onDrivePick(getContext(), false, new Storages.onDrivePickListener() {
							@Override
							public void onPick(String drive) {
								FileBrowser fb;

								fb = new FileBrowser(getContext(), drive, new String[] {".sf2"});

								fb.setCaption("Pick sound font *.sf2");
								fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
									@Override
									public void onPick(String selected) {
										synthRomPath.setText(selected);
									}
								});

								fb.show();
							}
						});
						break;
					}
					case R.id.midi_confirm:
					{
						if (synthRomPath.getText().toString().trim().equals("")) {
							MessageInfo.info("midi_sf");
							break;
						}

						if (event != null) {
							event.onSave(synthRomPath.getText().toString());
						}
						dismiss();
						break;
					}
				}
			}
		};

		return onClick;
	}

	public void setOnMidiSynthEventListener(OnMidiSynthEventListener event)
	{
		this.event = event;
	}
}