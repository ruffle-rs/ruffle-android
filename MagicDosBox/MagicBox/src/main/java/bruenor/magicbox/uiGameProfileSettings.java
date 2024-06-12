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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import magiclib.CrossSettings;
import magiclib.Global;
import magiclib.IO.FileBrowser;
import magiclib.IO.FileBrowserItem;
import magiclib.IO.Files;
import magiclib.IO.Storages;
import magiclib.collection.CollectionItem;
import magiclib.controls.Dialog;
import magiclib.controls.HelpViewer;
import magiclib.controls.ImageSize;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.CDROMItem;
import magiclib.core.Screen;
import magiclib.dosbox.DosboxConfig;
import magiclib.dosbox.MidiType;
import magiclib.dosbox.PerformanceType;
import magiclib.dosbox.SerialPort;
import magiclib.locales.Localization;
import magiclib.logging.Log;
import magiclib.logging.MessageInfo;

abstract interface GameProfileSettingsEventListener
{
	public abstract void onSave(DosboxConfig gameConfig, CollectionItem game, String imageFile, String imageName, String description);
	public abstract void onSetupRun(DosboxConfig gameConfig, CollectionItem game, String imageFile, String imageName, String description);
}

class uiGameProfileSettings extends Dialog
{
	DosboxConfig cfg = null;
	
	private EditText name;
	private ImageButton avatar;
	private CheckBox soundOn;
	private String imageFile = null;
	private CollectionItem game;
	private String imageName = null;
	private int memorySize;
	private TextView memoryText = null;
	private int cycles;
	private int frameSkip;
	private CheckBox pcspeaker = null;	
	private CheckBox xms = null;
	private CheckBox ems = null;
	private CheckBox umb = null;
	private Button soundblaster = null;
	private String soundBlasterType;
	private Button cpuCore = null;
	private LinearLayout mounts = null;
	private CheckBox cdrom_enabled;
	private ImageButton cdrom_add;	
	private MountsAdapter mountsAdapter = null;	
	private Button chooseDriveC = null;
	private EditText driveC = null;
	private Button chooseMainProgram = null;
	private EditText mainProgram = null;
	private CheckBox enableExpertCommands = null;
	private Button chooseSetupProgram = null; 
	private EditText setupProgram = null;
	private String setupMountID = null;	
	private CheckBox automaticPerformance = null;
	private CheckBox cyclesMax = null;
	private CheckBox cyclesCustom = null;
	private ImageButton soundSpeakerSettings = null;
	private ImageButton butSave;
	private ImageButton butHelp;
	private Button butSetupRun;
	private ImageButton butDosboxConfig = null;
	private String dosboxConfig;
	private TextView mainProgramTitle = null;
	private CheckBox aspectOn;
	private Button midi;
	private Button gus;
	private Button mainParam;
	private Button setupParam;
	private Button IPX;
	private Button serialPort;
	private ImageButton joystick;
	private CheckBox fasterAndroidBuffer;
	
	private int audioRate;
	private int audioBlockSize;
	private int audioPreBuffer;

	//midi
	private boolean midiEnabled;
	private MidiType midiType;
	//midi mt32
	private String midiMT32RomDir;
	private boolean midiMT32RunInThread;
	private int midiMT32Analog;
	private int midiMT32Dac;
	private int midiMT32Prebuffer;
	//midi synth
	private String midiSynthROMPath;

	//gus
	private boolean gusEnabled;
	private String gusPath;

	//params
	String mainProgramParams;
	String setupProgramParams;

	//ipx
	private boolean ipxEnabled;
	private boolean ipxAskAtStart;
	private boolean ipxClientOn;
	private int ipxServerPort;
	private String ipxClientToIP;
	private int ipxClientToPort;

	//serial port (support modem and disabled currently)
	private SerialPort serialPort1;
	private int serialPort1ModemPort;

	//native joystick support
	private boolean joystickEnabled;
	private boolean joystickTimed;

	private GameProfileSettingsEventListener eventListener = null;
	
	private long saveStateUnlockTime = 0;
	private int saveStateUnlockTapCount = 0;
	
	public class MountsAdapter 
	{
		public ImageSize imageSize = ImageSize.small_medium;
		public List<CDROMItem> items = new ArrayList<CDROMItem>();
		
		private LinearLayout root;
		private View.OnLongClickListener onItemLongClick = null;
		private View.OnClickListener onItemClick = null;
		private View.OnClickListener onCheckBoxClick = null;
		private View.OnClickListener onReorderClick = null;
		
		public MountsAdapter(LinearLayout root) 
		{
			this.root = root;
		}

		public void updateItem(CDROMItem item) 
		{
			int position = items.indexOf(item);
			View v = root.getChildAt(position);
		
			if (item != null)
			{
				CheckBox cbx = (CheckBox)v.findViewById(R.id.cdromimage_selected);
				cbx.setChecked(item.isChecked());
				
				Button label = (Button)v.findViewById(R.id.cdromimage_label);
				label.setText(" " + item.getDriveLetter() + "   " + item.getLabel());
			}
		}
		
		public void removeItem(CDROMItem item)
		{
			int position = items.indexOf(item);
												
			root.removeViewAt(position);			
			items.remove(position);			
		}

		public void addItem(CDROMItem item) 
		{
			RelativeLayout view = (RelativeLayout)getLayoutInflater().inflate(R.layout.cdromitem, null);
			
			items.add(item);			
			root.addView(view);
						
			Button label = (Button)view.findViewById(R.id.cdromimage_label);
			label.setOnClickListener(getOnItemClick());
			label.setOnLongClickListener(getOnItemLongClick());
			
			CheckBox cbx = (CheckBox)view.findViewById(R.id.cdromimage_selected);
			cbx.setOnClickListener(getOnCheckBoxClick());
			
			ImageButton button = (ImageButton)view.findViewById(R.id.cdromimage_movedown);
			button.setOnClickListener(getOnReorderClick());
			
			button = (ImageButton)view.findViewById(R.id.cdromimage_moveup);
			button.setOnClickListener(getOnReorderClick());			
			
			updateItem(item);
		}
		
		private View.OnClickListener getOnReorderClick()
		{
			if (onReorderClick == null)
			{
				onReorderClick = new View.OnClickListener()
				{					
					@Override
					public void onClick(View v)
					{						
						RelativeLayout view = (RelativeLayout)v.getParent();
	
						int position = root.indexOfChild(view);
						int newPosition = position + ((v.getId() == R.id.cdromimage_moveup)?(-1):(1)); 
											
						CDROMItem item = items.get(position);
						
						items.remove(item);
						items.add(newPosition, item);
													
						UpdateItems(newPosition);
					}
				};
			}
			
			return onReorderClick;
		}
			
		private void UpdateItems(int current)
		{
			ImageButton bUp = null;
			ImageButton bDown = null;
			View view;
			int size = root.getChildCount();

			for (int i=0; i < size; i++)
			{
				view = root.getChildAt(i);				
				
				CDROMItem item = items.get(i);
				
				CheckBox cbx = (CheckBox)view.findViewById(R.id.cdromimage_selected);
				cbx.setChecked(item.isChecked());
				
				Button b = (Button)view.findViewById(R.id.cdromimage_label);
				b.setText(" " + item.getDriveLetter() + "   " + item.getLabel());
				
				bUp = (ImageButton)view.findViewById(R.id.cdromimage_moveup);
				bDown = (ImageButton)view.findViewById(R.id.cdromimage_movedown);
				
				if (i == current)
				{
					if (i == 0)
					{
						bUp.setVisibility(View.INVISIBLE);
						bDown.setVisibility(View.VISIBLE);
					}					
					else if (i == size - 1)
					{
						bUp.setVisibility(View.VISIBLE);
						bDown.setVisibility(View.INVISIBLE);						
					}
					else
					{
						bUp.setVisibility(View.VISIBLE);
						bDown.setVisibility(View.VISIBLE);
					}
					
					b.setTextColor(Color.GREEN);
				}
				else
				{
					bUp.setVisibility(View.INVISIBLE);
					bDown.setVisibility(View.INVISIBLE);
					b.setTextColor(Color.WHITE);
				}
			}
		}
		
		private void hideOrderButtons()
		{
			hideOrderButtons(-1);
		}
		
		private void hideOrderButtons(int excludePosition)
		{
			int size = root.getChildCount();
			ImageButton bUp = null;
			ImageButton bDown = null;
			Button b = null;
			
			for (int i=0; i < size; i++)
			{
				if (((excludePosition > -1) && (excludePosition != i)) || (excludePosition == -1))
				{
					bUp = (ImageButton)root.getChildAt(i).findViewById(R.id.cdromimage_moveup);					
					bUp.setVisibility(View.INVISIBLE);
					
					bDown = (ImageButton)root.getChildAt(i).findViewById(R.id.cdromimage_movedown);
					bDown.setVisibility(View.INVISIBLE);
					
					b = (Button)root.getChildAt(i).findViewById(R.id.cdromimage_label);
					b.setTextColor(Color.WHITE);
				}
			}	
		}
		
		private View.OnClickListener getOnCheckBoxClick()
		{
			if (onCheckBoxClick == null)
			{
				onCheckBoxClick = new View.OnClickListener()
				{				
					@Override
					public void onClick(View v)
					{
						View view = (View)v.getParent();
						
						int position = root.indexOfChild(view);
						
						CDROMItem item = items.get(position);
						
						CheckBox cbx = (CheckBox)view.findViewById(R.id.cdromimage_selected);
						cbx.setChecked(true);
						
						hideOrderButtons();
						
						if (item.isChecked())
						{
							return;
						}
						
						item.setChecked(true);					

						for (int i=0; i<items.size(); i++)
						{
							if (i != position)
							{
								items.get(i).setChecked(false);
							}
																
							if (view != root.getChildAt(i))
							{
								cbx = (CheckBox)root.getChildAt(i).findViewById(R.id.cdromimage_selected);
								cbx.setChecked(false);								
							}						
						}
					}
				};
			}
			
			return onCheckBoxClick;
		}
		
		private View.OnClickListener getOnItemClick()
		{
			if (onItemClick == null)
			{
				onItemClick = new View.OnClickListener()
				{				
					@Override
					public void onClick(View v)
					{
						int position = root.indexOfChild((RelativeLayout)v.getParent());
						int size = root.getChildCount();	
						
						if (size < 2)
							return;
						
						Button b = (Button)v;
						b.setTextColor(Color.GREEN);
						
						ImageButton bUp = (ImageButton)root.getChildAt(position).findViewById(R.id.cdromimage_moveup);
						ImageButton bDown = (ImageButton)root.getChildAt(position).findViewById(R.id.cdromimage_movedown);

						if (position == 0)
						{
							if (bUp.getVisibility() == View.VISIBLE)
								bUp.setVisibility(View.INVISIBLE);						
						}
						else
						{
							if (bUp.getVisibility() == View.INVISIBLE)
								bUp.setVisibility(View.VISIBLE);
						}
											
						if (position == size - 1)
						{
							if (bDown.getVisibility() == View.VISIBLE)
								bDown.setVisibility(View.INVISIBLE);						
						}
						else
						{
							if (bDown.getVisibility() == View.INVISIBLE)
								bDown.setVisibility(View.VISIBLE);						
						}						

						hideOrderButtons(position);
					}
				};
			}
			
			return onItemClick;
		}
		
		private View.OnLongClickListener getOnItemLongClick()
		{
			if (onItemLongClick == null)
			{
				onItemLongClick = new View.OnLongClickListener()
				{				
					@Override
					public boolean onLongClick(View v)
					{
						/*int position = root.indexOfChild((RelativeLayout)v.getParent());
						
						uiCDROM cdrom = new uiCDROM(getContext(), items.get(position), cfg);
						
						cdrom.setOnCDROMEventListener(new CDROMEventListener()
						{					
							@Override
							public void onPick(CDROMItem selected)
							{						
								mountsAdapter.updateItem(selected);						
							}

							@Override
							public void onDelete(CDROMItem selected)
							{
								mountsAdapter.removeItem(selected);
								
								if (setupMountID == null)
									return;
								
								if (selected.getId().equals(setupMountID))
								{
									setupMountID = null;
									setupProgram.setText("");								
								}
							}
						});
						
						cdrom.setOnDismissListener(new OnDismissListener()
						{							
							@Override
							public void onDismiss(DialogInterface dialog)
							{
								hideOrderButtons();
							}
						});
						
						cdrom.show();*/

						final int position = root.indexOfChild((RelativeLayout)v.getParent());

						uiImageViewer viewer = new uiImageViewer(getContext());
						viewer.setCaption("cdrom_caption");

						viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
							@Override
							public boolean onSet(List images) {
								images.add(new ImageViewerItem(R.drawable.icon_edit, "edit", "common_edit"));
								images.add(new ImageViewerItem(R.drawable.icon_disabled, "delete", "common_delete"));

								return true;
							}
						});

						viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
							@Override
							public void onPick(ImageViewerItem selected) {
								CDROMItem item = items.get(position);

								if (selected.getName().equals("edit")) {
									uiCDROM cdrom = new uiCDROM(item);

									cdrom.setOnCDROMEventListener(new CDROMEventListener()
									{
										@Override
										public void onPick(CDROMItem selected)
										{
											mountsAdapter.updateItem(selected);
										}
									});

									cdrom.setOnDismissListener(new OnDismissListener()
									{
										@Override
										public void onDismiss(DialogInterface dialog)
										{
											hideOrderButtons();
										}
									});

									cdrom.show();
								} else {
									mountsAdapter.removeItem(item);

									if (setupMountID == null)
										return;

									if (item.getId().equals(setupMountID))
									{
										setupMountID = null;
										setupProgram.setText("");
									}
								}
							}
						});

						viewer.show();

						return true;
					}
				};
			}
			
			return onItemLongClick;
		}
	}	
	/*
    public static void setListViewHeightBasedOnChildren(ListView listView) 
    {
        ListAdapter listAdapter = listView.getAdapter(); 
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }*/	
	
	private void setupFromHDD()
	{
		if (driveC.getText().toString().equals(""))
		{
			MessageInfo.info("gamec_msg_emptydrivec");
			return;
		}
		
		FileBrowser fb = new FileBrowser(getContext(), driveC.getText().toString(), new String[] {".exe", ".EXE", ".com", ".COM", ".bat", ".BAT"});
		fb.setCaption("fb_caption_choose_exe_bat_com");
		fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
		{					
			@Override
			public void onPick(String selected)
			{
				/*String exePath = AppGlobal.getPathWithoutSourceDrive(selected);
				String cPath = AppGlobal.getPathWithoutSourceDrive(driveC.getText().toString());
										
				setupProgram.setText(exePath.substring(cPath.length()));
				
				butSetupRun.setVisibility(View.VISIBLE);
				setupMountID = null;*/
				
				String dir = new File(driveC.getText().toString()).getAbsolutePath();						
				String exePath = selected.substring(dir.length());
				
				if (!exePath.startsWith("/"))
					exePath = "/" + exePath; 
				
				setupProgram.setText(exePath);	
				butSetupRun.setVisibility(View.VISIBLE);
				setupMountID = null;								
			}
		});
		
		fb.show();		
	}
	
	private void selectSourceDirectory(String drive)
	{
		FileBrowser fb;
		
		fb = new FileBrowser(getContext(), drive, null, true);
		
		fb.setCaption("fb_caption_choose_folder");
		fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
		{					
			@Override
			public void onPick(String selected)
			{			
				driveC.setText(selected);
			}
		});
		
		fb.show();
	}
	
	private boolean inPerformanceCheck = false;
	private CompoundButton.OnCheckedChangeListener performanceListener()
	{
		return new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton v,
					boolean isChecked)
			{
				if (inPerformanceCheck)
					return;
				
				inPerformanceCheck = true;
				
				switch (v.getId())
				{
					case R.id.activity_game_starter_edit_autoperformance:
					{						
						automaticPerformance.setChecked(true);
						cyclesMax.setChecked(false);
						cyclesCustom.setChecked(false);
						break;
					}
					case R.id.activity_game_starter_edit_cyclesmax:
					{						
						automaticPerformance.setChecked(false);
						cyclesMax.setChecked(true);
						cyclesCustom.setChecked(false);
						break;
					}
					case R.id.activity_game_starter_edit_cyclescustom:
					{						
						automaticPerformance.setChecked(false);
						cyclesMax.setChecked(false);
						cyclesCustom.setChecked(true);
						break;
					}					
				}
				
				inPerformanceCheck = false;
			}
		};
	}
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.activity_game_starter_edit_application,       "gamec_application");
		localize(R.id.activity_game_starter_edit_application_title, "gamec_application_title");
		
		localize(R.id.activity_game_starter_edit_hardware,          "gamec_hardware");		
		localize(R.id.activity_game_starter_edit_hardware_sound,    "gamec_hardware_sound");
		localize(R.id.activity_game_starter_edit_soundon,           "gamec_hardware_soundon");
		localize(R.id.activity_game_starter_edit_soundpcspeakeron,  "gamec_hardware_sound_pcspeaker");
		localize(R.id.activity_game_starter_edit_faster_android_buffer,  "gamec_hardware_faster_buffer");
		
		localize(R.id.activity_game_starter_edit_hardware_memory,   "gamec_hardware_memory");
		
		localize(R.id.activity_game_starter_edit_hardware_dos, "gamec_hardware_dos");
		localize(R.id.activity_game_starter_edit_xms,          "gamec_hardware_xms");
		localize(R.id.activity_game_starter_edit_ems,          "gamec_hardware_ems");
		localize(R.id.activity_game_starter_edit_umb,          "gamec_hardware_umb");

		localize(R.id.activity_game_starter_edit_hardware_video, "gamec_hardware_video");
		localize(R.id.activity_game_starter_edit_aspect, "gamec_hardware_aspecton");

		localize(R.id.activity_game_starter_edit_hardware_cpu, "gamec_hardware_cpu");
		
		localize(R.id.activity_game_starter_edit_hardware_performance, "gamec_hardware_performance");
		localize(R.id.activity_game_starter_edit_autoperformance,      "gamec_hardware_performance_auto");
		localize(R.id.activity_game_starter_edit_cyclesmax,            "gamec_hardware_performance_cmax");
		localize(R.id.activity_game_starter_edit_cyclescustom,         "gamec_hardware_performance_ccustom");
		
		localize(R.id.activity_game_starter_edit_hardware_drivec, "gamec_hardware_drivec");
		localize(R.id.activity_game_starter_edit_choosedrivec,    "gamec_hardware_drivec_choose");
		localize(R.id.activity_game_starter_edit_hardware_joystick_caption, "gamec_joystick");
		
		localize(R.id.activity_game_starter_edit_hardware_cdrom,        "gamec_hardware_cdrom");
		localize(R.id.activity_game_starter_edit_cdrom_enabled,         "common_enabled");
		localize(R.id.activity_game_starter_edit_software,              "gamec_software");
		localize(R.id.activity_game_starter_edit_mainprogramtitle,      "gamec_software_mainprogram");
		localize(R.id.activity_game_starter_edit_choosemainprogram,     "common_choose");
		localize(R.id.activity_game_starter_edit_mainprogramclipboard,  "common_get");
		localize(R.id.activity_game_starter_edit_software_setup,        "gamec_software_setup");
		localize(R.id.activity_game_starter_edit_choosesetup,           "common_choose");
		localize(R.id.activity_game_starter_edit_setupprogramclipboard, "common_get");
		localize(R.id.activity_game_starter_edit_expert,                "gamec_expert");
		
		localize(R.id.activity_game_starter_edit_expert_commands,       "gamec_expert_commands");
		localize(R.id.activity_game_starter_edit_runsetup,              "common_setup");

		localize(R.id.activity_game_starter_edit_hardware_network,  "common_network");
		localize(R.id.activity_game_starter_edit_serialport,              "serial_modem");
	}
	
	public uiGameProfileSettings(final Context context, CollectionItem item, String defaultDriveC)
	{
		super(context);
		
		this.game = item; 

		if (game != null)
		{
			imageName = game.getAvatar();
		}
		
		setContentView(R.layout.activity_game_starter_edit);
		setCaption("gamec_caption");
		
		butSave = (ImageButton)getView().findViewById(R.id.activity_game_starter_edit_confirm);
		butSetupRun  = (Button)getView().findViewById(R.id.activity_game_starter_edit_runsetup);
		butHelp  = (ImageButton)getView().findViewById(R.id.activity_game_starter_edit_help);

		View.OnClickListener mainButtonsListener = new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				DosboxConfig tmp = getConfig();
				
				if (tmp == null)
					return;
				
				if (eventListener != null)
				{
					switch (v.getId())
					{
						case R.id.activity_game_starter_edit_confirm:
						{
							eventListener.onSave(tmp, game, imageFile, imageName, name.getText().toString());
							dismiss();
							break;
						}
						case R.id.activity_game_starter_edit_runsetup:
						{
							eventListener.onSetupRun(tmp, game, imageFile, imageName, name.getText().toString());
							break;
						}
						case R.id.activity_game_starter_edit_help:{
							HelpViewer hlp = new HelpViewer("common_help", "help/tips/collection/index.html", "help/tips/collection/game-profile/sections.html", CrossSettings.showCollectionToolTip, false, true);
							hlp.setOnHelpEventListener(new HelpViewer.HelpEventListener() {
								@Override
								public void onStartEnable(boolean enabled) {
									CrossSettings.showCollectionToolTip = enabled;
									CrossSettings.save();
								}
							});
							hlp.show();
							break;
						}
					}					
				}
			}
		};
		
		butSave.setOnClickListener(mainButtonsListener);
		butSetupRun.setOnClickListener(mainButtonsListener);
		butHelp.setOnClickListener(mainButtonsListener);
		
		//game settings
		
		name = (EditText)getView().findViewById(R.id.activity_game_starter_edit_name);
		avatar = (ImageButton)getView().findViewById(R.id.activity_game_starter_edit_image);
		soundOn = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_soundon);
		aspectOn = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_aspect);
		memoryText = (TextView)getView().findViewById(R.id.activity_game_starter_edit_memorytext);
		pcspeaker = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_soundpcspeakeron);		
		xms = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_xms);
		ems = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_ems);
		umb = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_umb);
		soundblaster = (Button)getView().findViewById(R.id.activity_game_starter_edit_soundsblaster);
		cpuCore = (Button)getView().findViewById(R.id.activity_game_starter_edit_cpucore);
		cdrom_enabled = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_cdrom_enabled);
		cdrom_add = (ImageButton)getView().findViewById(R.id.activity_game_starter_edit_cdrom_add);
		mounts = (LinearLayout)getView().findViewById(R.id.activity_game_starter_edit_cdrom_mounts);
		chooseDriveC = (Button)getView().findViewById(R.id.activity_game_starter_edit_choosedrivec);
		driveC = (EditText)getView().findViewById(R.id.activity_game_starter_edit_drivec);
		chooseMainProgram = (Button)getView().findViewById(R.id.activity_game_starter_edit_choosemainprogram);
		mainProgram = (EditText)getView().findViewById(R.id.activity_game_starter_edit_mainprogram);
		enableExpertCommands = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_enablecommands);
		chooseSetupProgram = (Button)getView().findViewById(R.id.activity_game_starter_edit_choosesetup);
		setupProgram = (EditText)getView().findViewById(R.id.activity_game_starter_edit_setupprogram);
		Button mainProgramScript = (Button)getView().findViewById(R.id.activity_game_starter_edit_mainprogramclipboard);
		Button setupProgramScript = (Button)getView().findViewById(R.id.activity_game_starter_edit_setupprogramclipboard);
		automaticPerformance = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_autoperformance);
		cyclesMax = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_cyclesmax);
		cyclesCustom = (CheckBox)getView().findViewById(R.id.activity_game_starter_edit_cyclescustom);
		soundSpeakerSettings = (ImageButton)getView().findViewById(R.id.activity_game_starter_edit_soundpcspeaker_settings);
		butDosboxConfig = (ImageButton)getView().findViewById(R.id.activity_game_starter_edit_dosboxconfigsettings);
		mainProgramTitle = (TextView)getView().findViewById(R.id.activity_game_starter_edit_mainprogramtitle);
		midi = (Button)findViewById(R.id.activity_game_starter_edit_midi);
		gus = (Button)findViewById(R.id.activity_game_starter_edit_gus);
		mainParam = (Button)findViewById(R.id.activity_game_starter_edit_programparam);
		setupParam = (Button)findViewById(R.id.activity_game_starter_edit_choosesetupparam);
		IPX = (Button)findViewById(R.id.activity_game_starter_edit_ipx);
		serialPort = (Button)findViewById(R.id.activity_game_starter_edit_serialport);
		joystick = (ImageButton)findViewById(R.id.activity_game_starter_edit_hardware_joystick_settings);
		fasterAndroidBuffer = (CheckBox)findViewById(R.id.activity_game_starter_edit_faster_android_buffer);

		View.OnClickListener joystickListener = new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				JoystickEdit je = new JoystickEdit(getContext(), joystickEnabled, joystickTimed);
				je.setOnJoystickListener(new JoystickEdit.OnJoystickListener() {
					@Override
					public void onPick(boolean enabled, boolean timed) {
						joystickEnabled = enabled;
						joystickTimed = timed;
					}
				});
				je.show();
			}
		};

		joystick.setOnClickListener(joystickListener);

		View.OnClickListener networkListener = new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (v.getId() == R.id.activity_game_starter_edit_ipx) {
					IPXSettings d = new IPXSettings(false, ipxEnabled, ipxAskAtStart, ipxClientOn, ipxServerPort, ipxClientToIP, ipxClientToPort);
					d.setOnIPXEditEventListener(new IPXSettings.OnIPXEditEventListener() {
						@Override
						public void onSave(boolean enabled, boolean ask, boolean clientOn, int serverPort, String clientToIp, int clientToPort) {
							ipxEnabled = enabled;
							ipxAskAtStart = ask;
							ipxClientOn = clientOn;
							ipxServerPort = serverPort;
							ipxClientToIP = clientToIp;
							ipxClientToPort = clientToPort;
						}
					});
					d.show();
				} else {
					SerialPortSettings d = new SerialPortSettings(getContext(), serialPort1, serialPort1ModemPort);
					d.setOnSerialPortEventListener(new SerialPortSettings.OnSerialPortEventListener() {
						@Override
						public void onSave(SerialPort serial, int port) {
							serialPort1 = serial;
							serialPort1ModemPort = port;
						}
					});
					d.show();
				}
			}
		};

		IPX.setOnClickListener(networkListener);
		serialPort.setOnClickListener(networkListener);

		View.OnClickListener paramListener = new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				ParamatersEdit d =  new ParamatersEdit(context, v.getId()==R.id.activity_game_starter_edit_programparam?mainProgramParams:setupProgramParams);
				d.setOnParamatersListener(new ParamatersEdit.OnParametersListener()
				{
					@Override
					public void onPick(String params) {
						if (v.getId()==R.id.activity_game_starter_edit_programparam) {
							mainProgramParams = params;
						} else {
							setupProgramParams = params;
						}
					}
				});

				d.show();
			}
		};

		mainParam.setOnClickListener(paramListener);
		setupParam.setOnClickListener(paramListener);

		gus.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) {
				if (driveC.getText().toString().equals("")) {
					MessageInfo.info("gamec_msg_emptydrivec");
					return;
				}

				GUSSettings d = new GUSSettings(context, new File(driveC.getText().toString()), gusEnabled, gusPath);

				d.setOnGUSEventListener(new GUSSettings.OnGUSEventListener() {
					@Override
					public void onSave(boolean enabled, String path) {
						gusEnabled = enabled;
						gusPath = path;
					}
				});

				d.show();
			}
		});

		midi.setText("Midi : " + getLocaleString("common_off"));
		midi.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) {
				/*MidiSettings d = new MidiSettings(context,
						midiEnabled,
						midiType,
						//mt32
						midiMT32RomDir,
						midiMT32RunInThread,
						midiMT32Analog,
						midiMT32Dac,
						midiMT32Prebuffer,
						//synth
						midiSynthROMPath,
						automaticPerformance.isChecked());

				d.setOnMidiEventListener(new MidiSettings.OnMidiEventListener() {
					@Override
					public void onSave(boolean enabled,
									   MidiType type,
									   String mt32RomDir, boolean mt32RunInThread, int mt32Analog, int mt32Dac, int mt32Prebuffer,
									   String synthRomPath) {
						midiEnabled = enabled;
						midiType = type;
						midiMT32RomDir = mt32RomDir;
						midiMT32RunInThread = mt32RunInThread;
						midiMT32Analog = mt32Analog;
						midiMT32Dac = mt32Dac;
						midiMT32Prebuffer = mt32Prebuffer;
						midiSynthROMPath = synthRomPath;
					}
				});

				d.show();*/
				uiImageViewer viewer = new uiImageViewer(context);
				viewer.setCaption("midi_caption");

				viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
					@Override
					public boolean onSet(List images) {
						images.add(new ImageViewerItem(R.drawable.icon_disabled, "disabled", getLocaleString("common_disabled")));
						images.add(new ImageViewerItem(R.drawable.icon_mt32, "mt32", "MT-32"));
						images.add(new ImageViewerItem(R.drawable.icon_synth, "synth", "Synth"));

						return true;
					}
				});

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
					@Override
					public void onPick(ImageViewerItem selected) {
						if (selected.getName().equals("disabled")) {
							midiEnabled = false;
							midi.setText("Midi : " + getLocaleString("common_off"));
						} else if (selected.getName().equals("mt32")) {
							MidiMT32Settings d = new MidiMT32Settings(getContext(),
									midiMT32RomDir,
									midiMT32RunInThread,
									midiMT32Analog,
									midiMT32Dac,
									midiMT32Prebuffer,
									automaticPerformance.isChecked());
							d.setOnMidiMT32EventListener(new MidiMT32Settings.OnMidiMT32EventListener() {
								@Override
								public void onSave(String mt32RomDir, boolean mt32RunInThread, int mt32Analog, int mt32Dac, int mt32Prebuffer) {
									midiEnabled = true;
									midiType = MidiType.mt32;
									midiMT32RomDir = mt32RomDir;
									midiMT32RunInThread = mt32RunInThread;
									midiMT32Analog = mt32Analog;
									midiMT32Dac = mt32Dac;
									midiMT32Prebuffer = mt32Prebuffer;

									midi.setText("Midi : MT-32");
								}
							});
							d.show();
						} else if (selected.getName().equals("synth")) {
							MidiSynthSettings d = new MidiSynthSettings(getContext(), midiSynthROMPath);
							d.setOnMidiSynthEventListener(new MidiSynthSettings.OnMidiSynthEventListener() {
								@Override
								public void onSave(String synthRomPath) {
									midiEnabled = true;
									midiType = MidiType.synth;
									midiSynthROMPath = synthRomPath;

									midi.setText("Midi : Synth");
								}
							});
							d.show();
						}
					}
				});

				viewer.show();
			}
		});

		automaticPerformance.setOnCheckedChangeListener(performanceListener());
		cyclesMax.setOnCheckedChangeListener(performanceListener());
		cyclesCustom.setOnCheckedChangeListener(performanceListener());
			
		mainProgramTitle.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{				
				if ((System.currentTimeMillis() - saveStateUnlockTime) < 500)
				{
					saveStateUnlockTapCount++;
				}
				else
					saveStateUnlockTapCount = 0;

				
				saveStateUnlockTime = System.currentTimeMillis();
				
				if (saveStateUnlockTapCount == 5) {
					cfg.unlockSaveState(!cfg.isSaveStateUnlocked());

					if (cfg.isSaveStateUnlocked()) {
						MessageInfo.infoEx("Save states are unlocked");
					}
					else
						MessageInfo.infoEx("Save states are locked");
				}
			}
		});
		
		butDosboxConfig.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				uiEditDosboxConfig d = new uiEditDosboxConfig(context);
				d.setConfig(dosboxConfig);
				d.setOnEditDosBoxConfigListener(new EditDosBoxConfigListener()
				{					
					@Override
					public void onSave(String config)
					{
						dosboxConfig = config;						
					}
				});
				d.show();
			}
		});
				
		soundSpeakerSettings.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				uiSoundSettings d = new uiSoundSettings(context, automaticPerformance.isChecked());
				d.setRate(audioRate);
				d.setBlockSize(audioBlockSize);
				d.setPreBuffer(audioPreBuffer);
				d.setOnSoundSpeakerEventListener(new SoundSettingsEventListener()
				{					
					@Override
					public void onPick(int rate, int blockSize, int preBuffer)
					{
						audioRate = rate;
						audioBlockSize = blockSize;
						audioPreBuffer = preBuffer;
					}
				});
				d.show();
			}
		});
		
		View.OnClickListener clipBoardScript = new View.OnClickListener()
		{			
			@Override
			public void onClick(final View v)
			{
                ClipBoardDialog clp = new ClipBoardDialog();
                clp.setOnClipboardEventListener(new ClipboardEventListener()
                {
                    @Override
                    public void onPick(boolean autoexec)
                    {
                        DosboxConfig cfg =  getConfig();

                        if (cfg == null)
                            return;

                        String script;

                        if (autoexec)
                        {
                            if (v.getId() == R.id.activity_game_starter_edit_mainprogramclipboard)
                            {
                                script = DosboxConfig.generateMainCommand(cfg);
                            }
                            else
                                script = DosboxConfig.generateSetupCommand(cfg);

                            AppGlobal.textToClipboard(context, script);

                            MessageInfo.info("gamec_msg_autoexecinclipboard");
                        }
                        else
                        {
                            try
                            {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                PrintStream out = new PrintStream(os, true, "utf-8");

								DosboxConfig.generateDosboxConfig(out, cfg);

                                AppGlobal.textToClipboard(context, os.toString());
                                out.close();
                                os.close();

                                MessageInfo.info("gamec_msg_autoexecinclipboard");
                            } catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                clp.show();
			}
		};
		
		mainProgramScript.setOnClickListener(clipBoardScript);
		setupProgramScript.setOnClickListener(clipBoardScript);
				
		chooseSetupProgram.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if ((mountsAdapter== null) || (mountsAdapter.items == null) || (mountsAdapter.items.size() == 0))
				{
					setupFromHDD();
					return;
				}
				
				uiImageViewer viewer = new uiImageViewer(context);
				//viewer.loadSetupChoice(mountsAdapter.items);
				
				viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter()
				{					
					@Override
					public boolean onSet(List images)
					{
						images.add(new ImageViewerItem(R.drawable.icon_sdcard, "hdd", getLocaleString("fb_drivec")));
						
						for (CDROMItem cd : mountsAdapter.items)
						{
							images.add(new ImageViewerItem(R.drawable.icon_cdimage, cd.getId(), cd.getLabel(), cd));			
						}
						
						return true;
					}
				});
				
				viewer.setCaption("imgview_caption_search");				
				
				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener()
				{
					@Override
					public void onPick(ImageViewerItem selected)
					{
						if (selected.getName().equals("hdd"))
						{
							setupFromHDD();
						}
						else
						{
							final CDROMItem cd = (CDROMItem)selected.getTag();
							
							FileBrowser fb = new FileBrowser(context, cd.getSourcePath(), new String[] {".exe", ".EXE", ".com", ".COM", ".bat", ".BAT"});
							fb.setCaption("fb_caption_choose_setup");
							fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
							{					
								@Override
								public void onPick(String selected)
								{
									if (!cd.isMappedImage())
									{
										selected = selected.replace(cd.getSourcePath(), "");
										
										if (!selected.startsWith("/"))
											selected = "/" + selected;
									}
									
									setupProgram.setText(selected);
									setupMountID = cd.getId();
									butSetupRun.setVisibility(View.VISIBLE);
								}
							});
							
							fb.show();								
						}
					}
				});					
								
				viewer.show();			
			}
		});
				
		chooseDriveC.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{				
				Storages.onDrivePick(context, true, new Storages.onDrivePickListener() {
					@Override
					public void onPick(String drive) {
						selectSourceDirectory(drive);
					}
				});
			}
		});
		
		chooseMainProgram.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if (driveC.getText().toString().equals(""))
				{
					MessageInfo.info("gamec_msg_emptydrivec");
					return;
				}
				
				FileBrowser fb = new FileBrowser(context, driveC.getText().toString(), new String[] {".exe", ".EXE", ".com", ".COM", ".bat", ".BAT"});
				fb.setCaption("fb_caption_choose_exe_bat_com");
				fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
				{					
					@Override
					public void onPick(String selected)
					{
						String dir = new File(driveC.getText().toString()).getAbsolutePath();						
						String exePath = selected.substring(dir.length());
						
						if (!exePath.startsWith("/"))
							exePath = "/" + exePath; 
						
						mainProgram.setText(exePath);						
					}
				});
				
				fb.show();			
			}
		});
		
		cdrom_add.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				uiCDROM cdrom = new uiCDROM(null);
				cdrom.setOnCDROMEventListener(new CDROMEventListener() {
					@Override
					public void onPick(CDROMItem selected) {
						mountsAdapter.addItem(selected);
					}
				});
				
				cdrom.show();
			}
		});
		
		cdrom_enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				cdrom_enabled.setChecked(isChecked);
			}
		});
		
		cpuCore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uiImageViewer viewer = new uiImageViewer(context);
				viewer.setCaption("imgview_caption_cpucore");

				viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
					@Override
					public boolean onSet(List images) {
						images.add(new ImageViewerItem(R.drawable.icon_cpucore_simple, "simple", Localization.getString("cpucore_simple")));
						images.add(new ImageViewerItem(R.drawable.icon_cpucore_normal, "normal", Localization.getString("cpucore_normal")));
						images.add(new ImageViewerItem(R.drawable.icon_cpucore_dynamic, "dynamic", Localization.getString("cpucore_dynamic")));
						images.add(new ImageViewerItem(R.drawable.icon_cpucore_auto, "auto", Localization.getString("cpucore_auto")));

						return true;
					}
				});

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
					@Override
					public void onPick(ImageViewerItem selected) {
						cpuCore.setTag(selected.getName());

						setCpuCoreText();
					}
				});

				viewer.show();
			}
		});		
		
		soundblaster.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				uiImageViewer viewer = new uiImageViewer(context);
				viewer.setCaption("imgview_caption_soundblaster");
				
				viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter()
				{					
					@Override
					public boolean onSet(List images)
					{
						images.add(new ImageViewerItem(R.drawable.icon_disabled, "none", "common_disabled"));
						images.add(new ImageViewerItem(R.drawable.icon_soundblaster1_0, "sb1", "SB 1.0"));
						images.add(new ImageViewerItem(R.drawable.icon_soundblaster2_0, "sb2", "SB 2.0"));
						images.add(new ImageViewerItem(R.drawable.icon_soundblaster_pro1_0, "sbpro1", "SB Pro 1.0"));
						images.add(new ImageViewerItem(R.drawable.icon_soundblaster_pro2_0, "sbpro2", "SB Pro 2.0"));
						images.add(new ImageViewerItem(R.drawable.icon_soundblaster16, "sb16", "SB 16"));
						
						return true;
					}
				});
				
				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener()
				{
					@Override
					public void onPick(ImageViewerItem selected)
					{
						soundBlasterType = selected.getName();
						soundblaster.setText(getSoundBlasterDescription());
					}
				});				
				
				
				viewer.show();
			}
		});
		
		avatar.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{		
				Storages.onDrivePick(context, new Storages.onDrivePickListener() {
					@Override
					public void onPick(String drive) {
						final FileBrowser fb = new FileBrowser(context, drive, new String[]{".png", ".jpg", ".jpeg", ".bmp", ".PNG", ".JPG", ".JPEG", ".BMP"});
						fb.setCaption("fb_caption_find_image");
						fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
							@Override
							public void onPick(String selected) {
								imageFile = selected;

								Bitmap bmp = BitmapFactory.decodeFile(imageFile);

								avatar.setImageBitmap(bmp);
								avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

								try {
									imageName = "avatar." + Files.getFileExtension(selected);
								} catch (Exception exc) {

								}
							}
						});

						fb.setOnFileValidationEvent(new FileBrowser.OnFileValidationListener() {
							@Override
							public FileBrowserItem onValidation(File file) {
								try {
									BitmapFactory.decodeFile(file.getAbsolutePath(), AppGlobal.imageHeaderOptions);

									if (AppGlobal.imageHeaderOptions.outWidth < 0 || AppGlobal.imageHeaderOptions.outHeight < 0) {
										return null;
									}

									FileBrowserItem item = new FileBrowserItem(file);

									if ((AppGlobal.imageHeaderOptions.outWidth > 0) &&
											(AppGlobal.imageHeaderOptions.outHeight > 0) &&
											(AppGlobal.imageHeaderOptions.outWidth <= 1024) &&
											(AppGlobal.imageHeaderOptions.outHeight <= 1024)) {
										item.isPictureFile = true;
									} else {
										item.isPictureFile = false;//will not show preview
									}

									return item;
								} catch (Exception exc) {
								}

								return null;
							}
						});

						fb.show();
					}
				});
			}
		});
		
		soundOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				soundOn.setChecked(isChecked);
			}
		});

		aspectOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				aspectOn.setChecked(isChecked);
			}
		});
					
		View.OnClickListener memSizeClick = new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.activity_game_starter_edit_memoryplus:
					{		
						plusMemSize();
						break;
					}
					case R.id.activity_game_starter_edit_memoryminus:
					{
						minusMemSize();
						break;
					}					
				}
				
				memoryText.setText(memorySize + "MB");
			}
		};
		
		Button memSizePlus = (Button)getView().findViewById(R.id.activity_game_starter_edit_memoryplus);
		Button memSizeMinus = (Button)getView().findViewById(R.id.activity_game_starter_edit_memoryminus);
		
		memSizePlus.setOnClickListener(memSizeClick);
		memSizeMinus.setOnClickListener(memSizeClick);

		if (item != null)
		{
			if (!item.getAvatar().equals(""))
			{
            	File imgFile = new File(AppGlobal.gamesDataPath + item.getID() + "/" + item.getAvatar());
            	
            	if(imgFile.exists())
            	{
            	   Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            	   avatar.setImageBitmap(bmp);
            	   avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);  
            	}				
			}
			
			name.setText(item.getDescription());
									
			File source = new File(AppGlobal.gamesDataPath + item.getID() + "/config.xml");
			Serializer serializer = new Persister();
			
			try
			{
				cfg = serializer.read(DosboxConfig.class, source);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}
		else
		{
			cfg = new DosboxConfig();
			cfg.setCommand(DosboxConfig.generateDefaultDosboxConfig());
		}
		
		if (cfg != null)
		{
			memorySize = cfg.getMemorySize();
			cycles = cfg.getCycles();
			frameSkip = cfg.getFrameskip();

			//midi
			midiEnabled = cfg.midiEnabled;
			midiType = cfg.midiType;
			midiMT32RomDir = cfg.midiMT32ROMPath;
			midiMT32RunInThread = cfg.midiMT32RunInThread;
			midiMT32Analog = cfg.midiMT32Analog;
			midiMT32Dac = cfg.midiMT32DAC;
			midiMT32Prebuffer = cfg.midiMT32Prebuffer;
			midiSynthROMPath = cfg.midiSynthROMPath;

			if (!midiEnabled) {
				midi.setText("Midi : " + getLocaleString("common_off"));
			} else {
				switch (midiType) {
					case mt32: {
						midi.setText("Midi : MT-32");
						break;
					}
					case synth: {
						midi.setText("Midi : Synth");
						break;
					}
				}
			}

			//gus
			gusEnabled = cfg.gusEnabled;
			gusPath = cfg.gusPath;

			//params
			mainProgramParams = cfg.mainProgramParams;
			setupProgramParams = cfg.setupProgramParams;

			//ipx
			ipxEnabled = cfg.ipxEnabled;
			ipxAskAtStart = cfg.ipxAskAtStart;
			ipxClientOn = cfg.ipxClientOn;
			ipxServerPort = cfg.ipxServerPort;
			ipxClientToIP = cfg.ipxClientToIP;
			ipxClientToPort = cfg.ipxClientToPort;

			joystickEnabled = cfg.nativeJoystickEnabled;
			joystickTimed = cfg.nativeJoystickTimed;

			//serial port
			serialPort1 = cfg.serialPort1;
			serialPort1ModemPort = cfg.serialPort1ModemPort;

			dosboxConfig = cfg.getCommand();
			soundOn.setChecked(cfg.isSoundOn());
			aspectOn.setChecked(cfg.isAspectOn());
			audioRate = cfg.getAudioRate();
			audioBlockSize = cfg.getAudioBlockSize();
			audioPreBuffer = cfg.getAudioPreBuffer();
			memoryText.setText(memorySize + "MB");
			pcspeaker.setChecked(cfg.isPCspeakerOn());
			fasterAndroidBuffer.setChecked(cfg.useAndroidAudioHack);
			xms.setChecked(cfg.isXmsOn());
			ems.setChecked(cfg.isEmsOn());
			umb.setChecked(cfg.isUmbOn());
						
			automaticPerformance.setChecked(cfg.getPerformance() == PerformanceType.automatic);
			cyclesMax.setChecked(cfg.getPerformance() == PerformanceType.cycles_max);
			cyclesCustom.setChecked(cfg.getPerformance() == PerformanceType.cycles_custom);

			soundBlasterType = cfg.getSoundBlaster();
			soundblaster.setText(getSoundBlasterDescription());
			
			cpuCore.setTag(cfg.getCpuCore());
			setCpuCoreText();
			
			cdrom_enabled.setChecked(cfg.isCdromEnabled());
			
			mountsAdapter = new MountsAdapter(mounts);
			
			for (CDROMItem cd : cfg.cdromlist)
			{
				mountsAdapter.addItem(cd);
			}
			
			if (cfg.getDriveC().trim().equals(""))
			{
				driveC.setText(defaultDriveC);
			}
			else
				driveC.setText(cfg.getDriveC());
			
			mainProgram.setText(cfg.getMainProgram());
			enableExpertCommands.setChecked(cfg.isExpertEnabled());						
			setupProgram.setText(cfg.getSetupProgram());
			setupMountID = cfg.getSetupMountID();
			
			if (setupProgram.getText().toString().equals(""))
			{
				butSetupRun.setVisibility(View.GONE);
			}
		} 
	}
	
	private String getSoundBlasterDescription()
	{		
		if (soundBlasterType.equals("none"))
			return getLocaleString("gamec_hardware_sound_sbdisabled");
		
		if (soundBlasterType.equals("sb1"))
			return "Sound Blaster 1.0";		
		
		if (soundBlasterType.equals("sb2"))
			return "Sound Blaster 2.0";
		
		if (soundBlasterType.equals("sbpro1"))
			return "Sound Blaster Pro 1.0";		
		
		if (soundBlasterType.equals("sbpro2"))
			return "Sound Blaster Pro 2.0";		
		
		if (soundBlasterType.equals("sb16"))
			return "Sound Blaster 16";	
		
		return "???";
	}
	
	private boolean plusMemSize()
	{
		if (memorySize == 64)
			return false;
		
		switch (memorySize)
		{
			case 1: {memorySize = 2; break;} 
			case 2: {memorySize = 4; break;}
			case 4: {memorySize = 8; break;}
			case 8: {memorySize = 16; break;}
			case 16: {memorySize = 24; break;}
			case 24: {memorySize = 32; break;}
			case 32: {memorySize = 48; break;}
			case 48: {memorySize = 64; break;}
			//case 64: {memorySize = 96; break;}
			//case 96: {memorySize = 128; break;}			
			//case 128: {memorySize = 256; break;}
			//case 256: {memorySize = 512; break;}
			default:
			{
				break;
			}
		}
		
		return true;
	}
	
	private boolean minusMemSize()
	{
		if (memorySize == 1)
			return false;
		
		switch (memorySize)
		{
		    //case 512: {memorySize = 256; break;}
		    //case 256: {memorySize = 128; break;}
		    //case 128: {memorySize = 96; break;}
		    //case 96: {memorySize = 64; break;}
			case 64: {memorySize = 48; break;} 
			case 48: {memorySize = 32; break;}
			case 32: {memorySize = 24; break;}
			case 24: {memorySize = 16; break;}
			case 16: {memorySize = 8; break;}
			case 8: {memorySize = 4; break;}
			case 4: {memorySize = 2; break;}
			case 2: {memorySize = 1; break;}
			default:
			{
				break;
			}
		}
		
		return true;
	}	
		
	public int getImageResourceID()
	{		
		if (imageName == null)
			return -1;
		
		if (!imageName.equals(""))
		{
			return AppGlobal.getImageID(imageName);
		}
		
		return -1;
	}
	
	
	private void setCpuCoreText() 
	{
		if (cpuCore.getTag().toString().equals("simple")) 
		{
			cpuCore.setText(Localization.getString("cpucore_simple"));
		} 
		else if (cpuCore.getTag().toString().equals("normal")) 
		{
			cpuCore.setText(Localization.getString("cpucore_normal"));
		}
		else if (cpuCore.getTag().toString().equals("dynamic"))
		{
			cpuCore.setText(Localization.getString("cpucore_dynamic"));
		} 
		else if (cpuCore.getTag().toString().equals("auto"))
		{
			cpuCore.setText(Localization.getString("cpucore_auto"));
		}
	}
	
	private DosboxConfig getConfig()
	{
		//------------------validation------------------
		
		//check if drive C:\ can be mounted
		if (driveC.getText().toString().trim().equals(""))
		{
			MessageInfo.info("gamec_msg_emptydrivec");
			return null;
		}
		
		if (name.getText().toString().trim().equals(""))
		{
			MessageInfo.info("msg_title_required");
			return null;
		}
		
		//-------------validation is successful-------------		
		DosboxConfig config = new DosboxConfig();

		config.setCommand(dosboxConfig);
		config.setSoundOn(soundOn.isChecked());
		config.setAspectOn(aspectOn.isChecked());
		config.setAudioRate(audioRate);
		config.setAudioBlockSize(audioBlockSize);
		config.setAudioPreBuffer(audioPreBuffer);
		config.setMemorySize(memorySize);
		config.setCycles(cycles);
		config.setFrameskip(frameSkip);
		config.setPCspeakerOn(pcspeaker.isChecked());
		config.setXmsOn(xms.isChecked());
		config.setEmsOn(ems.isChecked());
		config.setUmbOn(umb.isChecked());
		config.setSoundBlaster(soundBlasterType);
		config.setCpuCore(cpuCore.getTag().toString());
		config.setCdromEnabled(cdrom_enabled.isChecked());
		config.setDriveC(driveC.getText().toString());
		config.setMainProgram(mainProgram.getText().toString());
		config.setExpertEnabled(enableExpertCommands.isChecked());

		if (automaticPerformance.isChecked())
		{
			config.setPerformance(PerformanceType.automatic);
		}
		else if (cyclesMax.isChecked())
		{
			config.setPerformance(PerformanceType.cycles_max);
		} else
			config.setPerformance(PerformanceType.cycles_custom);
		
		config.unlockSaveState(cfg.isSaveStateUnlocked());
		
		config.cdromlist.clear();
		
		for(CDROMItem item : mountsAdapter.items) {
			config.cdromlist.add(item);
		}

		config.setSetupProgram(setupProgram.getText().toString());
		config.setSetupMountID(setupMountID);

		config.useAndroidAudioHack = fasterAndroidBuffer.isChecked();

		//midi
		config.midiEnabled = midiEnabled;
		config.midiType = midiType;
		config.midiMT32ROMPath = midiMT32RomDir;
		config.midiMT32RunInThread = midiMT32RunInThread;
		config.midiMT32Analog = midiMT32Analog;
		config.midiMT32DAC = midiMT32Dac;
		config.midiMT32Prebuffer = midiMT32Prebuffer;
		config.midiSynthROMPath = midiSynthROMPath;

		config.gusEnabled = gusEnabled;
		config.gusPath = gusPath;

		config.mainProgramParams = mainProgramParams;
		config.setupProgramParams = setupProgramParams;

		config.ipxEnabled = ipxEnabled;
		config.ipxAskAtStart = ipxAskAtStart;
		config.ipxClientOn = ipxClientOn;
		config.ipxServerPort = ipxServerPort;
		config.ipxClientToIP = ipxClientToIP;
		config.ipxClientToPort = ipxClientToPort;

		config.nativeJoystickEnabled = joystickEnabled;
		config.nativeJoystickTimed = joystickTimed;

		//serial port
		config.serialPort1 = serialPort1;
		config.serialPort1ModemPort = serialPort1ModemPort;

		return config;
	}
	
	public void setOnGameSettingsEventListener(GameProfileSettingsEventListener event)
	{
		this.eventListener = event;
	}
	
	@Override
	protected void onStop()
	{
		if (Log.DEBUG) Log.log("stopping");
		
		if ((mountsAdapter != null) && (mountsAdapter.items != null))
		{
			mountsAdapter.items.clear();
		}

		super.onStop();
	}
}

abstract interface ClipboardEventListener
{
    public abstract void onPick(boolean autoexec);
}

class ClipBoardDialog extends Dialog
{
    private ClipboardEventListener event;

	public ClipBoardDialog() {
		super(AppGlobal.context);
        setContentView(R.layout.cfg_to_clp_dialog);
		setCaption("common_message");

		setSize(260, ViewGroup.LayoutParams.WRAP_CONTENT);

        View.OnClickListener clickEvent = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.clpdlg_autoexec:
                    case R.id.clpdlg_full: {
                        if (event!=null) {
                            event.onPick((v.getId() == R.id.clpdlg_autoexec)?true:false);
                        }
                        break;
                    }
                }

                dismiss();
            }
        };

        Button b = (Button)findViewById(R.id.clpdlg_autoexec);
		b.setOnClickListener(clickEvent);
		b.setText(Localization.getString("autoexec_short"));

        b = (Button)findViewById(R.id.clpdlg_full);
		b.setOnClickListener(clickEvent);
		b.setText(Localization.getString("common_all"));

		TextView txt = (TextView)findViewById(R.id.clpdlg_html);
		txt.setText(Localization.getString("msg_clipboard_get"));
    }

    public void setOnClipboardEventListener(ClipboardEventListener event) {
        this.event = event;
    }
}

class ParamatersEdit extends Dialog
{
	public abstract interface OnParametersListener
	{
		public abstract void onPick(String params);
	}

	private OnParametersListener event;
	private EditText paramEdit;

	public ParamatersEdit(Context context, String params)
	{
		super(context);

		setContentView(R.layout.params_edit);
		setCaption("common_parameters");

		if (params == null)
			params = "";

		paramEdit = (EditText)findViewById(R.id.params_value);
		paramEdit.setText(params);

		ImageButton confirm = (ImageButton)findViewById(R.id.params_confirm);
		confirm.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (event != null) {
					event.onPick(paramEdit.getText().toString());
				}
				dismiss();
			}
		});
	}

	public void setOnParamatersListener(OnParametersListener event)
	{
		this.event = event;
	}
}

class JoystickEdit extends Dialog
{
	private OnJoystickListener event;
	private CheckBox cbxEnabled;
	private CheckBox cbxTimed;

	@Override
	public void onSetLocalizedLayout() {
		localize(R.id.joystick_edit_enabled, "common_enabled");
	}

	public abstract interface OnJoystickListener
	{
		public abstract void onPick(boolean enabled, boolean timed);
	}

	public JoystickEdit(Context context, boolean enabled, boolean timed) {
		super(context);

		setContentView(R.layout.joystick_edit);
		setCaption("common_joystick");

		cbxEnabled = (CheckBox)findViewById(R.id.joystick_edit_enabled);
		cbxEnabled.setChecked(enabled);

		cbxTimed = (CheckBox)findViewById(R.id.joystick_edit_timed);
		cbxTimed.setChecked(timed);

		ImageButton confirm = (ImageButton)findViewById(R.id.joystick_edit_confirm);
		confirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (event != null) {
					event.onPick(cbxEnabled.isChecked(), cbxTimed.isChecked());
				}
				dismiss();
			}
		});
	}

	public void setOnJoystickListener(OnJoystickListener event)
	{
		this.event = event;
	}
}