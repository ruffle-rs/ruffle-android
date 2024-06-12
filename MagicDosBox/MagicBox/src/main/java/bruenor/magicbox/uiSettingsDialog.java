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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import magiclib.CrossSettings;
import magiclib.Global;
import magiclib.controls.Dialog;
import magiclib.controls.HelpViewer;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.Backup;

import magiclib.core.EmuConfig;
import magiclib.core.EmuManager;
import magiclib.core.NativeOption;
import magiclib.core.Screen;

import magiclib.dosbox.CyclesDialog;
import magiclib.dosbox.DosboxConfig;
import magiclib.dosbox.FrameSkipDialog;
import magiclib.dosbox.PerformanceType;
import magiclib.graphics.EmuVideo;
import magiclib.graphics.ScreenDesign;
import magiclib.graphics.opengl.ShaderProgramType;
import magiclib.core.EmuManagerMode;
import magiclib.gui_modes.DesignMode;
import magiclib.gui_modes.ModeToolbar;
import magiclib.locales.Localization;
import magiclib.logging.Log;
import magiclib.logging.MessageBox;
import magiclib.logging.MessageInfo;
import magiclib.mapper.Mapper;

public class uiSettingsDialog extends Dialog
{
	private static FrameSkipDialog frame_skip = null;
	private boolean isDesignMode;
	
	public List<SettingsItem> main_list = new ArrayList<SettingsItem>();
		
	class SettingsAdapter extends ArrayAdapter<SettingsItem> 
	{
		private List<SettingsItem> items;

	    public SettingsAdapter(Context context, int textViewResourceId, List<SettingsItem> items)
	    {
	        super(context, textViewResourceId, items);
	        
	        this.items = items;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) 
	    {
	    	View v = convertView;
	    	
	    	ImageView avatar = null;
	    	TextView description = null;
	    	
	        if (v == null) 
	        {
	            v = getLayoutInflater().inflate(R.layout.settings_item, null);

	            avatar = (ImageView) v.findViewById(R.id.settings_item_imageview);
	            description = (TextView) v.findViewById(R.id.settings_item_text);
	            
            	description.setTextSize(TypedValue.COMPLEX_UNIT_PX, ImageViewer.textSize);
            	description.getLayoutParams().height = ImageViewer.textViewHeight;
            	description.getLayoutParams().width = ImageViewer.imageSizeinPx;

            	avatar.getLayoutParams().height = ImageViewer.imageSizeinPx;
            	avatar.getLayoutParams().width = ImageViewer.imageSizeinPx;
	        }	    	
	        
	        SettingsItem item = items.get(position);

	        if (item != null) 
	        {	            
	        	if (avatar == null)
	        		avatar = (ImageView) v.findViewById(R.id.settings_item_imageview);
	        	
	        	if (description == null)
	        		description = (TextView) v.findViewById(R.id.settings_item_text);

	            description.setText(item.getDescription());	            
	            avatar.setImageResource(item.getImageID());
	        }
	        
            return v;
	    }	    
	}
	
	public void onSetLocalizedLayout() 
	{
		localize(R.id.settings_dialog_butsavelayout_title, "genset_menu_savelayout_title");
		localize(R.id.settings_dialog_butquit_title, "genset_menu_quit_title");
	};
	
	public uiSettingsDialog()
	{
		super(AppGlobal.context);
		
		setContentView(R.layout.settings_dialog);
		//setTitleBarVisible(false);
		//setPaddingInDP(0, 0, 0, 0);
		setCaption("genset_caption");
		
		setQuitButton();
		//setPlayModeButton();
		setSaveLayoutButton();
		setKeyboardButton();
			
		loadMain();
	}
	
	@Override
	protected void onStop()
	{
		if (Log.DEBUG) Log.log("stopping");
		
		if (main_list != null)
		{
			main_list.clear();
		}
		
		super.onStop();
	}
	
	private void setQuitButton()
	{
		//Quit button
		LinearLayout button = (LinearLayout)findViewById(R.id.settings_dialog_butquit);		
		button.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				dismiss();
				
				EmuManager.quit();
			}
		});
	}

	private void setSaveLayoutButton()
	{
		LinearLayout button = (LinearLayout)findViewById(R.id.settings_dialog_butsavelayout);		
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//save layout
				Serializer serializer = new Persister();
				File layout = new File(AppGlobal.currentLayoutFile);

				try {
					serializer.write(EmuManager.emuConfig, layout);
				} catch (Exception e) {
					e.printStackTrace();
				}

				//save global settings
				if (DosboxConfig.isModified()) {
					DosboxConfig.save();
				}

				//save mapper
				if (Mapper.edited) {
					Mapper.save();
				}

				boolean redraw = EmuManager.clearDeletedResources();
				EmuManager.clearNewWidgets();

				if (CrossSettings.edited)
					CrossSettings.save();

				new Backup().confirmChanges();

				dismiss();

				if (redraw) {
					EmuVideo.redraw();
				}
			}
		});	
	}		

	private void showAndroidKeyboard() {
		dismiss();

		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		}

		EmuVideo.surface.requestFocus();
	}

	private void showDosboxKeyboard() {
		dismiss();

		EmuManager.showSystemKeyboard();
	}

	private void setKeyboardButton()
	{
		View.OnClickListener onClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.settings_dialog_butleftkeyboard: {
						showAndroidKeyboard();
						break;
					}
					case R.id.settings_dialog_butrightkeyboard: {
						showDosboxKeyboard();
						break;
					}
				}
			}
		};

		((ImageButton)findViewById(R.id.settings_dialog_butleftkeyboard)).setOnClickListener(onClick);
		((ImageButton)findViewById(R.id.settings_dialog_butrightkeyboard)).setOnClickListener(onClick);
	}	
		
	public static void showCycles()
	{
		  CyclesDialog dlg =  new CyclesDialog();
		  
		  dlg.showAtLocation(Screen.screenWidth >> 1 ,
				             Screen.screenHeight >> 1);
	}	
	
	public static void screenshotGallery()
	{			
		File dir = new File(AppGlobal.currentGameScreenShotsPath);
		
		File[] files = null;
		
		if(!dir.exists())
		{
			dir.mkdirs();
		}
		else
		{
			files = dir.listFiles();
		}
		
		if ((files == null) || (files.length == 0))
		{
			MessageInfo.info("msg_no_screenshots");
			return;
		}
		
		uiScreenshotGallery gallery = new uiScreenshotGallery(Global.context);
		gallery.setFiles(files);
		gallery.show();
	}
	
	public static void frameskip()
	{	
		if (frame_skip == null)
		{
			frame_skip =  new FrameSkipDialog();
		}

		frame_skip.showAtLocation(Screen.screenWidth >> 1, Screen.screenHeight >> 1);
	}
	
	private void addEditMode()
	{
		if (isDesignMode) {
			main_list.add(new SettingsItem(SettingsItemType.edit_mode, R.drawable.icon_play, "editmode_finish"));
			return;
		}

		main_list.add(new SettingsItem(SettingsItemType.edit_mode, R.drawable.icon_pause, "editmode_start"));
	}

	private void addSpecialKeys()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.special_keys, R.drawable.icon_specialkeys, "genset_menu_speckeys"));
	}

	private void addScale()
	{

		main_list.add(new SettingsItem(SettingsItemType.screen_adjust, R.drawable.icon_resize_size, "genset_menu_resize"));
	}
	
	private void addFilter()
	{
		if (isDesignMode)
			return;
		
		String filterInfo = "";
		
		switch (EmuConfig.graphic_filter)
		{
			case ShaderProgramType.normal:
			{
				filterInfo = Localization.getString("grfilter_normal");
				break;
			}
			case ShaderProgramType.linear:
			{
				filterInfo = Localization.getString("grfilter_linear");
				break;
			}			
			case ShaderProgramType.hq2x:
			{
				filterInfo = Localization.getString("grfilter_hq2x");
				break;
			}
			case ShaderProgramType.hq4x:
			{
				filterInfo = Localization.getString("grfilter_hq4x");
				break;
			}
			case ShaderProgramType._2xSaI:
			{
				filterInfo = Localization.getString("grfilter_2xSaI");
				break;
			}
			case ShaderProgramType.superEagle:
			{
				filterInfo = Localization.getString("grfilter_superEagle");
				break;
			}
			case ShaderProgramType._5xBR:
			{
				filterInfo = Localization.getString("grfilter_5xBR");
				break;
			}
			case ShaderProgramType.mcgreen:
			{
				filterInfo = Localization.getString("grfilter_mcgreen");
				break;
			}
			case ShaderProgramType.mcamber:
			{
				filterInfo = Localization.getString("grfilter_mcamber");
				break;
			}			
			case ShaderProgramType.grayscale:
			{
				filterInfo = Localization.getString("grfilter_grayscale");
				break;
			}			
			case ShaderProgramType.crt:
			{
				filterInfo = Localization.getString("grfilter_crt");
				break;
			}
			case ShaderProgramType.scanline:
			{
				filterInfo = Localization.getString("grfilter_scanline");
				break;
			}
		}
		
		main_list.add(new SettingsItem(SettingsItemType.graphic_filter, R.drawable.icon_smooth, 
				Localization.getString("genset_menu_graphic_filter") + " (" + filterInfo + ")", true));
	}	
	
	private void addCycles()
	{
		if (isDesignMode)
			return;
		
		if (DosboxConfig.config.getPerformance() == PerformanceType.cycles_custom)
			main_list.add(new SettingsItem(SettingsItemType.cycles, R.drawable.img_potionofpower, "genset_menu_power"));		
	}
	
	private void addFrameSkip()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.frame_skip, R.drawable.icon_frameskip, "genset_menu_frameskip"));		
	}
	
	private void addSaveStates()
	{
		if (isDesignMode)
			return;
		
		if (DosboxConfig.config.isSaveStateUnlocked())
		{		
			main_list.add(new SettingsItem(SettingsItemType.save_state, R.drawable.icon_timeportal, "Set time portal"));
			main_list.add(new SettingsItem(SettingsItemType.load_state, R.drawable.icon_timetravel, "Travel"));
		}		
	}
	
	private void addGestures()
	{
		main_list.add(new SettingsItem(SettingsItemType.gestures, R.drawable.img_scratch, "genset_menu_gestures"));
	}

	private void addMouse()
	{
		if (isDesignMode)
			return;

		main_list.add(new SettingsItem(SettingsItemType.mouse, R.drawable.icon_mouse, "genset_menu_mouse"));
	}

	private void addScreenshot()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.screenshot, R.drawable.img_lightning, "genset_menu_screenshot"));
	}
	
	private void addGallery()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.screenshot_gallery, R.drawable.icon_gallery, "genset_menu_gallery"));
	}
	
	private void addAdvSettings()
	{
		main_list.add(new SettingsItem(SettingsItemType.advanced_settings, R.drawable.img_flamesword, "genset_menu_advset"));
	}
	
	private void addInputMethod()
	{
		main_list.add(new SettingsItem(SettingsItemType.input_method, R.drawable.img_towngate, "genset_menu_inputmet"));
	}
	
	private void addSwapImage()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.swap_image, R.drawable.icon_swapimage, "genset_menu_swapimage"));
	}
	
	private void addMapper()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.mapper, R.drawable.icon_voodoo, "genset_menu_mapper"));
	}
	
	private void addHideWidgets()
	{
		if (isDesignMode)
			return;
		
		main_list.add(new SettingsItem(SettingsItemType.hide_all_buttons, R.drawable.img_shadow, EmuManager.showAllButtons ? "genset_menu_hide_buttons" : "genset_menu_show_buttons"));
	}

	private void addFonts()
	{
		main_list.add(new SettingsItem(SettingsItemType.fonts, R.drawable.img_rune, "genset_menu_runecrafting"));
	}

    private void addHelp()
    {
        main_list.add(new SettingsItem(SettingsItemType.help, R.drawable.icon_imgnone, "common_help"));
    }

	private void loadMain()
	{
		final GridView grid = (GridView)findViewById(R.id.settings_dialog_gridview);
        grid.setColumnWidth(ImageViewer.imageSizeinPx);
        
        isDesignMode = (EmuManager.mode == EmuManagerMode.design);

		addEditMode();
        addSpecialKeys();
        //addResize();
        addScale();
		addFilter();
		addCycles();
		addFrameSkip();
		addSaveStates();
		addGestures();
		addMouse();
		addScreenshot();
		addGallery();
		addAdvSettings();
		addInputMethod();
		addSwapImage();
		addMapper();		
		addHideWidgets();
		addFonts();
        addHelp();
		
		grid.setAdapter(new SettingsAdapter(getContext(), android.R.layout.simple_list_item_1, main_list));
		
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() 
        {
        	  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) 
        	  {        		  
        		  SettingsItem item = (SettingsItem)grid.getItemAtPosition(position);
        		  
        		  if (item != null)
        		  {
        			  if (item.getType() == SettingsItemType.special_keys)
        			  {
        				  dismiss();
        				          				  
        				  EmuManager.setNativeOption(NativeOption.showSpecialKeys, 0, null);
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.save_state)
        			  {
        				  dismiss();
        				  
        				  uiSaveState state = new uiSaveState(Global.context, SaveStateAction.save);
        				  state.show();  
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.load_state)
        			  {
        				  dismiss();
        				  
        				  uiSaveState state = new uiSaveState(Global.context, SaveStateAction.load);
        				  state.show();  
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.screen_adjust)
        			  {
        				  dismiss();
        				  
        				  ScreenDesign.start();
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.graphic_filter)
        			  {
        				  dismiss();
        				  
        				  uiImageViewer v = new uiImageViewer(AppGlobal.context);
        				  v.setCaption("imgview_caption_grfilter");
        				  v.itemStyle = ImageViewer.ImageViewerItemStyle.style2;
        				  v.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter()
        				  {							
        					  @Override
        					  public boolean onSet(List images)
        					  {
        						  images.add(new ImageViewerItem(0, "normal", "grfilter_normal"));
        						  images.add(new ImageViewerItem(0, "linear", "grfilter_linear"));
        						  //images.add(new ImageViewerItem("test", "test"));
        						  images.add(new ImageViewerItem(CrossSettings.shader_hq2x? 0 : R.drawable.img_padlock, "hq2x", "grfilter_hq2x"));
        						  //images.add(new ImageViewerItem("hq4x", "grfilter_hq4x"));
        						  images.add(new ImageViewerItem(0, "2xSaI", "grfilter_2xSaI"));
        						  //images.add(new ImageViewerItem("superEagle", "grfilter_superEagle"));
        						  //images.add(new ImageViewerItem("5xBR", "grfilter_5xBR"));
        						  images.add(new ImageViewerItem(0, "mcgreen", "grfilter_mcgreen"));
        						  images.add(new ImageViewerItem(0, "mcamber", "grfilter_mcamber"));
        						  images.add(new ImageViewerItem(0, "grayscale", "grfilter_grayscale"));
        						  //images.add(new ImageViewerItem("crt", "grfilter_crt"));
        						  //images.add(new ImageViewerItem("scanline", "grfilter_scanline"));
        						  return true;
        					  }
        				  });
        				  
        				  v.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
							  @Override
							  public void onPick(ImageViewerItem selected) {
								  int filter = -1;
								  if (selected.getName().equals("normal")) {
									  filter = ShaderProgramType.normal;
								  } else if (selected.getName().equals("linear")) {
									  filter = ShaderProgramType.linear;
								  } else if (selected.getName().equals("hq2x")) {
									  if (CrossSettings.shader_hq2x) {
										  filter = ShaderProgramType.hq2x;
									  } else {
										  MessageBox msg = new MessageBox();
										  msg.setHtml("grfilter_hq2x_warning");
										  msg.setOKButton(new MessageBox.MessageBoxClickEventListener() {
											  @Override
											  public void onClick() {
												  CrossSettings.edited = CrossSettings.shader_hq2x = true;
												  EmuConfig.graphic_filter = ShaderProgramType.hq2x;
												  EmuVideo.changeVideoFilter();
												  EmuVideo.redraw();
											  }
										  });
										  msg.show();
										  return;
									  }
								  } else if (selected.getName().equals("hq4x")) {
									  filter = ShaderProgramType.hq4x;
								  } else if (selected.getName().equals("2xSaI")) {
									  filter = ShaderProgramType._2xSaI;
								  } else if (selected.getName().equals("superEagle")) {
									  filter = ShaderProgramType.superEagle;
								  } else if (selected.getName().equals("5xBR")) {
									  filter = ShaderProgramType._5xBR;
								  } else if (selected.getName().equals("mcgreen")) {
									  filter = ShaderProgramType.mcgreen;
								  } else if (selected.getName().equals("mcamber")) {
									  filter = ShaderProgramType.mcamber;
								  } else if (selected.getName().equals("crt")) {
									  filter = ShaderProgramType.crt;
								  } else if (selected.getName().equals("scanline")) {
									  filter = ShaderProgramType.scanline;
								  } else if (selected.getName().equals("grayscale")) {
									  filter = ShaderProgramType.grayscale;
								  } else if (selected.getName().equals("test")) {
									  filter = ShaderProgramType.test;
								  }

								  if (filter > -1) {
									  EmuConfig.graphic_filter = filter;
									  EmuVideo.changeVideoFilter();
									  EmuVideo.redraw();
								  }
							  }
						  });
        				  
        				  v.show();
        				  
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.gestures)
        			  {
        				  dismiss();
        				  
        				  GesturesSettings d = new GesturesSettings();
        				  d.show();
        			  }

					  if (item.getType() == SettingsItemType.mouse)
					  {
						  dismiss();

						  uiMouseDialog d = new uiMouseDialog();
						  d.show();
					  }

        			  if (item.getType() == SettingsItemType.swap_image)
        			  {
        				  dismiss();
        				  
        				  EmuManager.setNativeOption(NativeOption.swapInNextDisk, 1, null);
        				  
        				  MessageInfo.info("common_ok");
        				  
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.cycles)
        			  {
        				  dismiss();
        				  
        				  showCycles();
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.screenshot)
        			  {
        				  dismiss();
        				  
        				  EmuVideo.createScreenshot();
        				  return;
        			  } 
        			  
        			  if (item.getType() == SettingsItemType.screenshot_gallery)
        			  {
        				  dismiss();
        				  
        				  screenshotGallery();
        				  return;
        			  } 
        			  
        			  if (item.getType() == SettingsItemType.frame_skip)
        			  {
        				  dismiss();
        				  
        				  frameskip();
        				  return;
        			  }
        			  
        			  if (item.getType() == SettingsItemType.advanced_settings)
        			  {
        				  dismiss();
        				  
        				  uiGameAdvancedSettings dlg = new uiGameAdvancedSettings(Global.context);
        				  dlg.show();
        				  return;
        			  }        			  

        			  if (item.getType() == SettingsItemType.input_method)
        			  {
        				  dismiss();
        				  
						InputMethodManager imm = (InputMethodManager) Global.context.getSystemService(Context.INPUT_METHOD_SERVICE);

						if (imm != null)
						{
							imm.showInputMethodPicker();							
							//imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
						}

						return;
        			  } 
        			  
        			  if (item.getType() == SettingsItemType.mapper)
        			  {
        				  dismiss();
        				  
        				  MapperSettings d = new MapperSettings();
        				  d.show();
        				  return;
        			  }

        			  if (item.getType() == SettingsItemType.hide_all_buttons)
        			  {
        				  dismiss();

						  EmuManager.hideAllButtons(null);
        				  return;
        			  }

					  if (item.getType() == SettingsItemType.fonts)
					  {
						  dismiss();

						  FontsSettings d = new FontsSettings();
						  d.show();
						  return;
					  }

                      if (item.getType() == SettingsItemType.help)
                      {
                          dismiss();

						  if (isDesignMode) {
							  HelpViewer hlp = new HelpViewer("common_help", "help/tips/design/index.html", null, CrossSettings.showDesignHelp, false, true);
							  hlp.setOnHelpEventListener(new HelpViewer.HelpEventListener()
							  {
								  @Override
								  public void onStartEnable(boolean enabled) {
									  CrossSettings.showDesignHelp = enabled;
									  CrossSettings.save();
								  }
							  });
							  hlp.show();
							  return;
						  }

						  HelpViewer hlp = new HelpViewer("common_help", "help/tips/ingame/index.html", null, CrossSettings.showInGameHelp, false, true);
                          hlp.setOnHelpEventListener(new HelpViewer.HelpEventListener() {
                              @Override
                              public void onStartEnable(boolean enabled) {
                                  CrossSettings.showInGameHelp = enabled;
                                  CrossSettings.save();
                              }
                          });
                          hlp.show();
                          return;
                      }

					  if (item.getType() == SettingsItemType.edit_mode)
					  {
						  dismiss();

						  if (EmuManager.mode == EmuManagerMode.design)
						  {
							  EmuManager.setMode(EmuManagerMode.play);

							  if (EmuManager.isPaused())
							  {
								  EmuManager.unPause();
							  }

							  ModeToolbar.dispose();

							  DesignMode.unselectAll();

							  EmuManager.setLazyDrawing();

							  EmuManager.resetZIndexes();
							  EmuVideo.redraw();
						  }
						  else
						  {
							  EmuManager.setMode(EmuManagerMode.design);

							  if (!EmuManager.isPaused())
							  {
								  EmuManager.pause();
							  }

							  //showMoveWindow();

							  EmuManager.setLazyDrawing();

							  MessageInfo.info("editmode_msg_start", true);

							  if (CrossSettings.showDesignHelp) {
								  HelpViewer hlp = new HelpViewer("common_help", "help/tips/design/index.html", null, CrossSettings.showDesignHelp, false, true);
								  hlp.setOnHelpEventListener(new HelpViewer.HelpEventListener() {
									  @Override
									  public void onStartEnable(boolean enabled) {
										  CrossSettings.showDesignHelp = enabled;
										  CrossSettings.save();
									  }
								  });
								  hlp.show();
							  }
						  }
					  }
                  }
        		  
        	  }
        });			
	}
}

enum SettingsItemType
{
	edit_mode,
	special_keys,
	//graphics
	screen_adjust,
	graphic_filter,
	//dosbox
	cycles,
	frame_skip,
	save_state,
	load_state,
	gestures,
	mouse,
	swap_image,
	//other
	screenshot,
	screenshot_gallery,
	advanced_settings,
	input_method,
	mapper,
	hide_all_buttons,
	fonts,
    help
}

class SettingsItem
{
	private int imageID;
	private String description;
	private SettingsItemType type;

	public SettingsItem(SettingsItemType type, int imageID, String description)
	{
		this(type, imageID, description, true);
	}
	
	public SettingsItem(SettingsItemType type, int imageID, String description, boolean localize)
	{
		this.type = type;
		this.imageID = imageID;
		
		if (localize)
		{
			this.description = Localization.getString(description);
		}
		else
			this.description = description;
	}

	public int getImageID()
	{
		return imageID;
	}
	public void setImageID(int imageID)
	{
		this.imageID = imageID;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public SettingsItemType getType()
	{
		return type;
	}

	public void setType(SettingsItemType type)
	{
		this.type = type;
	}	
}