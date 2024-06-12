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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import magiclib.IO.FileBrowser;
import magiclib.IO.Files;
import magiclib.IO.SAFSupport;
import magiclib.IO.Storages;
import magiclib.IO.UserStorage;
import magiclib.controls.Dialog;
import magiclib.controls.HelpViewer;
import magiclib.controls.ImageSize;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.NavigationCursor;
import magiclib.keyboard.KeyCodeInfo;
import magiclib.locales.Language;
import magiclib.locales.Localization;
import magiclib.logging.Log;
import magiclib.logging.MessageInfo;

class GlobalSettings extends Dialog
{
	public abstract interface OnConfirmEventListener
	{
		public abstract void onSave(String defaultDriveC,
									Language language,
									String storage1title, String storagePath1,
									String storage2title, String storagePath2,
									boolean debug,
									boolean navigationCursor,
									int navigationReservedButton,
									boolean navigationLeftJoy,
									ImageSize fbItemSize,
									ImageSize imgViewerItemSize,
									String dataDir);
	}

	//storage backup
	private boolean returnBackup;
	private UserStorage bckpStorage1;
	private UserStorage bckpStorage2;
	private ImageSize bckpFbItemSize;
	private ImageSize bckpImgViewerItemSize;
	private int bckpNavigationReservedButton;
	private int newNavigationReservedButton;

	private int selectedLanguageIndex;
	private Language selectedLanguage;

	private EditText driveC;
	private EditText dataDirectory;
	private EditText storage1;
	private EditText storage2;	
	private TextView storage1Title;
	private TextView storage2Title;
	private TextView sdcardUriRealPath;
	private CheckBox debugging;
	private CheckBox navigationCursor;
	private CheckBox navigationLeftJoy;

	private OnConfirmEventListener event;

	TextView filebrowserSizeText;
	TextView imgViewerSizeText;

	@Override
	public void onSetLocalizedLayout()
	{
		localize(R.id.gls_size_caption,   "common_size");
		localize(R.id.gls_filebrowser_caption,   "gls_filebrowser");
		localize(R.id.gls_imgviewer_caption, "gls_imgviewer");

		localize(R.id.gls_directories_caption,     "gls_directories");
		localize(R.id.gls_directories_mbxdatadir,  "gls_directories_mbxdatadir");
		localize(R.id.gls_settings_choosedatadir,  "gls_directories_mbxdatadir_choose");
		localize(R.id.gls_directories_ddcdir,      "gls_directories_ddcdir");
		localize(R.id.gls_settings_choosedrivec,   "gls_directories_ddcdir_choose");
								
		localize(R.id.gls_sdcard_caption, "gls_sdcard");
		localize(R.id.gls_sdcard_addstorages, "gls_sdcard_addstorages");
		localize(R.id.gls_settings_showstorages, "gls_sdcard_show_detected");
		
		localize(R.id.gls_sdcard_request_permissions_title, "gls_sdcard_request_permissions_title");
		localize(R.id.gls_sdcard_request_permissions_hint, "gls_sdcard_request_permissions_hint");
		localize(R.id.gls_sdcard_request_permissions, "gls_sdcard_request_permissions");

		localize(R.id.gls_language_caption, "gls_language");
		
		localize(R.id.gls_settings_languagepacks_title, "gls_language_translator_title");
		localize(R.id.gls_settings_languagepacks_hint, "gls_language_translator_hint");
		localize(R.id.gls_settings_exportlanguagepack, "gls_language_translator_export");
		localize(R.id.gls_settings_importlanguagepack, "gls_language_translator_import");

		localize(R.id.gls_nontouchcontrol_caption, "navcursor_label");
		localize(R.id.gls_settings_navcursor, "common_enabled");
		localize(R.id.gls_navcursor_key_hint, "navcursor_reservedbutton");
		localize(R.id.gls_navcursor_leftstick, "navcursor_leftjoy");

		localize(R.id.gls_other_caption, "gls_other");
		localize(R.id.gls_settings_debugging, "gls_other_debug");
		localize(R.id.gls_settings_credits, "common_credits");
	}

	@Override
	public void dismiss()
	{
		Storages.reset();
		
		if (returnBackup)
		{
			Storages.userStorage1 = bckpStorage1;
			Storages.userStorage2 = bckpStorage2;
			FileBrowser.setItemsSize(bckpFbItemSize);
			ImageViewer.setImageSize(bckpImgViewerItemSize);
			NavigationCursor.reservedButton = bckpNavigationReservedButton;
		}

		super.dismiss();
	}
	
	public GlobalSettings(String defaultDriveC, Language language)
	{
		super(AppGlobal.context);
		
		setContentView(R.layout.global_settings);
		//setIcon(R.drawable.icon_settings);
		setCaption("gls_caption");
		
		Button credits = (Button)findViewById(R.id.gls_settings_credits);
		credits.setOnClickListener(new View.OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				HelpViewer hlp = new HelpViewer("common_credits", "credits.html", null);
                hlp.show();
			}
		});
		
		View.OnClickListener sizeEvent = new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.gls_filebrowser_itemsize_minus:
					{
						decreaseFileBrowserSize();
						break;
					}
					case R.id.gls_filebrowser_itemsize_plus:
					{
						increaseFileBrowserSize();
						break;
					}
					case R.id.gls_imgviewer_itemsize_minus:
					{
						decreaseImageViewerSize();
						break;
					}
					case R.id.gls_imgviewer_itemsize_plus:
					{
						increaseImageViewerSize();
						break;
					}
				}
			}
		};
		
		ImageButton sizePlus;
		ImageButton sizeMinus;

		//file browser items size
		filebrowserSizeText = (TextView)findViewById(R.id.gls_filebrowser_itemsize_value);
		filebrowserSizeText.setTag(FileBrowser.itemsSize);
		
		sizeMinus = (ImageButton)getView().findViewById(R.id.gls_filebrowser_itemsize_minus);
		sizeMinus.setOnClickListener(sizeEvent);
		
		sizePlus = (ImageButton)getView().findViewById(R.id.gls_filebrowser_itemsize_plus);
		sizePlus.setOnClickListener(sizeEvent);


		setFileBrowserSizeDescription(FileBrowser.itemsSize);
		
		//menu and image viewer items size
		imgViewerSizeText = (TextView)findViewById(R.id.gls_imgviewer_itemsize_value);
		imgViewerSizeText.setTag(ImageViewer.imageSize);
		
		sizeMinus = (ImageButton)getView().findViewById(R.id.gls_imgviewer_itemsize_minus);
		sizeMinus.setOnClickListener(sizeEvent);
		
		sizePlus = (ImageButton)getView().findViewById(R.id.gls_imgviewer_itemsize_plus);
		sizePlus.setOnClickListener(sizeEvent);		

		setImageViewerSizeDescription(ImageViewer.imageSize);
		
		//backup
		bckpFbItemSize = FileBrowser.itemsSize;
		bckpImgViewerItemSize = ImageViewer.imageSize;

		//mount drive c
		driveC = (EditText)findViewById(R.id.gls_settings_drivec);
		driveC.setText(defaultDriveC);
		
		Button chooseDriveC = (Button)findViewById(R.id.gls_settings_choosedrivec);
		chooseDriveC.setOnClickListener(new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{						
				Storages.onDrivePick(getContext(), true, new Storages.onDrivePickListener()
				{					
					@Override
					public void onPick(String drive)
					{
						selectSourceDirectory(drive);
					}
				});							
			}
		});		
		
		dataDirectory = (EditText)findViewById(R.id.gls_settings_datadir);
		dataDirectory.setText(AppGlobal.getSharedString(getContext(), "datadirectory"));
		
		Button dataDir = (Button)findViewById(R.id.gls_settings_choosedatadir);
		
		dataDir.setOnClickListener(new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				Storages.onDrivePick(getContext(), true, new Storages.onDrivePickListener()
				{					
					@Override
					public void onPick(String drive)
					{
						/*File f = new File(drive, "MagicBox");
						dataDirectory.setText(f.getAbsolutePath() + "/");*/
						FileBrowser fb;

						fb = new FileBrowser(getContext(), drive, null, true);

						fb.setCaption("fb_caption_choose_folder");
						fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
						{
							@Override
							public void onPick(String selected)
							{
								//File f = new File(selected, "MagicBox");
								File f = new File(selected);
								if (!f.getName().equals("MagicBox")) {
									f = new File(selected, "MagicBox");
								}
								dataDirectory.setText(f.getAbsolutePath() + "/");
							}
						});

						fb.show();
					}
				});						
			}
		});
		
		Button showStorages = (Button)findViewById(R.id.gls_settings_showstorages);
		showStorages.setOnClickListener(new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				StoragesPreview preview = new StoragesPreview(getContext());
				preview.show();
			}
		});
		
		View.OnClickListener setStorageEvent = new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.gls_settings_setstorage1:
					{
						uiAddStorage addStorage = new uiAddStorage(getContext(), storage1Title.getText().toString(),
																			     storage1.getText().toString());
						addStorage.setOnAddStorageEventListener(new AddStorageEventListener()
						{							
							@Override
							public void onPick(String title, String path)
							{
								storage1Title.setText(title);
								storage1.setText(path);

								Storages.reset();
								Storages.userStorage1 = new UserStorage(title, path);
							}
						});

						addStorage.show();
						break;
					}
					case R.id.gls_settings_setstorage2:
					{
						uiAddStorage addStorage = new uiAddStorage(getContext(), storage2Title.getText().toString(),
																			     storage2.getText().toString());
						addStorage.setOnAddStorageEventListener(new AddStorageEventListener()
						{							
							@Override
							public void onPick(String title, String path)
							{
								storage2Title.setText(title);
								storage2.setText(path);

								Storages.reset();
								Storages.userStorage2 = new UserStorage(title, path);
							}
						});

						addStorage.show();
						break;
					}							
				}
			}
		}; 
		
		ImageButton setStorage = (ImageButton)findViewById(R.id.gls_settings_setstorage1);
		setStorage.setOnClickListener(setStorageEvent);
		
		setStorage = (ImageButton)findViewById(R.id.gls_settings_setstorage2);
		setStorage.setOnClickListener(setStorageEvent);				
		
		storage1Title = (TextView)findViewById(R.id.gls_settings_storage1Title);
		storage1 = (EditText)findViewById(R.id.gls_settings_storage1);

		UserStorage s1 = Storages.userStorage1;
		
		if (s1 != null)
		{
			storage1Title.setText(s1.title);
			storage1.setText(s1.path);
		}
		
		storage2Title = (TextView)findViewById(R.id.gls_settings_storage2Title);
		storage2 = (EditText)findViewById(R.id.gls_settings_storage2);
		
		UserStorage s2 = Storages.userStorage2;
		
		if (s2 != null)
		{
			storage2Title.setText(s2.title);
			storage2.setText(s2.path);
		}
		
		View.OnClickListener clearStorageEvent = new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.gls_settings_clearstorage1:
					{
						storage1Title.setText("");
						storage1.setText("");
						
						Storages.reset();
						Storages.userStorage1 = null;
						break;
					}
					case R.id.gls_settings_clearstorage2:
					{
						storage2Title.setText("");
						storage2.setText("");
						
						Storages.reset();
						Storages.userStorage2 = null;
						break;
					}							
				}
			}
		}; 
		
		ImageButton clearStorage = (ImageButton)findViewById(R.id.gls_settings_clearstorage1);
		clearStorage.setOnClickListener(clearStorageEvent);
		
		clearStorage = (ImageButton)findViewById(R.id.gls_settings_clearstorage2);
		clearStorage.setOnClickListener(clearStorageEvent);
		
		returnBackup = true;
		bckpStorage1 = null;
		bckpStorage2 = null;
		
		if (Storages.userStorage1 != null)
		{
			bckpStorage1 = new UserStorage(Storages.userStorage1.title, 
					                       Storages.userStorage1.path);
		}
		
		if (Storages.userStorage2 != null)
		{
			bckpStorage2 = new UserStorage(Storages.userStorage2.title, 
					                       Storages.userStorage2.path);
		}
		
		if (Build.VERSION.SDK_INT < 21)
		{		
			LinearLayout permissionPanel = (LinearLayout)findViewById(R.id.gls_sdcard_request_permissions_panel);
			permissionPanel.setVisibility(View.GONE);
		}
		else
		{
			sdcardUriRealPath = (TextView)findViewById(R.id.gls_sdcard_request_permissions_path);
			sdcardUriRealPath.setText(SAFSupport.sdcardUriRealPath);

			Button permisionRequest = (Button)findViewById(R.id.gls_sdcard_request_permissions);
			permisionRequest.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					((uiGameStarterActivity) (AppGlobal.context)).startActivityForResult(intent, 1);
				}
			});
		}
		
		final List<Language> languagesList = Localization.getSupportedLanguages();

		selectedLanguageIndex = languagesList.indexOf(language);
		selectedLanguage = languagesList.get(selectedLanguageIndex);
		
		final TextView languageInfo = (TextView)getView().findViewById(R.id.gls_language);
		languageInfo.setText(selectedLanguage.displayName);
					
		View.OnClickListener languageEvent = new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.gls_language_minus:
					{
						if (selectedLanguageIndex == 0)
							return;
						
						selectedLanguageIndex--;
						
						break;
					}
					case R.id.gls_language_plus:
					{
						if (selectedLanguageIndex == languagesList.size() - 1)
							return;
						
						selectedLanguageIndex++;

						break;
					}
				}
				
				selectedLanguage = languagesList.get(selectedLanguageIndex);
				languageInfo.setText(selectedLanguage.displayName);
			}
		};
		
		ImageButton languagePlus;
		ImageButton languageMinus;
		
		languageMinus = (ImageButton)getView().findViewById(R.id.gls_language_minus);
		languageMinus.setOnClickListener(languageEvent);
		
		languagePlus = (ImageButton)getView().findViewById(R.id.gls_language_plus);
		languagePlus.setOnClickListener(languageEvent);
		
		View.OnClickListener onTestLanguagePackClick = new View.OnClickListener() 
		{					
			@Override
			public void onClick(View v) 
			{
				switch (v.getId())
				{
					case R.id.gls_settings_exportlanguagepack:
					{
						exportLanguagePacks();
						break;
					}
					case R.id.gls_settings_importlanguagepack:
					{
						importLanguagePack();
						break;
					}
				}
			}
		};
		
		Button testLanguagePack = (Button)getView().findViewById(R.id.gls_settings_exportlanguagepack);
		testLanguagePack.setOnClickListener(onTestLanguagePackClick);
		
		testLanguagePack = (Button)getView().findViewById(R.id.gls_settings_importlanguagepack);
		testLanguagePack.setOnClickListener(onTestLanguagePackClick);

		debugging = (CheckBox)findViewById(R.id.gls_settings_debugging);
		debugging.setChecked(Log.DEBUG);

		navigationCursor = (CheckBox)findViewById(R.id.gls_settings_navcursor);
		navigationLeftJoy = (CheckBox)findViewById(R.id.gls_navcursor_leftstick);
		navigationLeftJoy.setChecked(NavigationCursor.useLeftStick);

		bckpNavigationReservedButton = NavigationCursor.reservedButton;
		newNavigationReservedButton = bckpNavigationReservedButton;

		if (Build.VERSION.SDK_INT < 21) {
			findViewById(R.id.gls_nontouchcontrol_caption).setVisibility(View.GONE);
			navigationCursor.setVisibility(View.GONE);
			navigationCursor.setChecked(false);
			navigationLeftJoy.setVisibility(View.GONE);
			findViewById(R.id.gls_settings_navcursor_butpanel).setVisibility(View.GONE);
		} else {
			navigationCursor.setChecked(NavigationCursor.enabled);

			final Button button = (Button)findViewById(R.id.gls_navcursor_button);
			button.setText(KeyCodeInfo.getAndroidKeyInfo(newNavigationReservedButton, false));
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapperDetector detector = new MapperDetector(getContext(), null, false);
					detector.setOnMapperEventListener(new MapperEventListener() {
						@Override
						public void onPick(int keyCode) {
							button.setText(KeyCodeInfo.getAndroidKeyInfo(keyCode, false));
							newNavigationReservedButton = keyCode;
						}
					});
					detector.show();
				}
			});
		}

		ImageButton b = (ImageButton)findViewById(R.id.gls_settings_confirm);
		b.setOnClickListener(confirmEvent());
	}

	public void sdcardUriChanged()
	{
		sdcardUriRealPath.setText(SAFSupport.sdcardUriRealPath);
	}

	private View.OnClickListener confirmEvent() 
	{
		return new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				returnBackup = false;

				if (event != null)
				{
					event.onSave(driveC.getText().toString(),
							selectedLanguage,
							storage1Title.getText().toString(), storage1.getText().toString(),
							storage2Title.getText().toString(), storage2.getText().toString(),
							debugging.isChecked(),
							navigationCursor.isChecked(),
							newNavigationReservedButton,
							navigationLeftJoy.isChecked(),
							(ImageSize)filebrowserSizeText.getTag(),
							(ImageSize)imgViewerSizeText.getTag(),
							dataDirectory.getText().toString());
				}

				dismiss();
			}
		};
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
	
	private void exportLanguagePacks()
	{
		try
		{
			File dataDir = new File(AppGlobal.appPath + "LanguagesExport");
			
			if (!dataDir.exists())
				dataDir.mkdirs();
			
		    AssetManager assetManager = getContext().getAssets();
		    
		    String[] files = null;
		    
		    files = assetManager.list("translation");
		    
		    for(String filename : files)
			{
		        InputStream in = null;
		        OutputStream out = null;

		        try {
		          	in = assetManager.open("translation/" + filename);
		          	File outFile = new File(dataDir, filename);
		          	out = new FileOutputStream(outFile);
					Files.fileCopy(in, out);
		          	in.close();
		          	in = null;
		          	out.flush();
		          	out.close();
		          	out = null;
		        }
				catch(Exception e)
				{
		            Log.log("Failed to copy asset file: " + filename);
		        }       
		    }
		    
		    MessageInfo.infoEx(Localization.getString("gls_msg_language_pack_exported") + dataDir.getAbsolutePath());
		}
		catch(Exception exc)
		{
			MessageInfo.info("gls_msg_language_pack_failed_export");
		}
	}
	
	private void importLanguagePack()
	{
		try
		{
			Storages.onDrivePick(getContext(), false, new Storages.onDrivePickListener()
			{					
				@Override
				public void onPick(String drive)
				{
					FileBrowser fb;
					
					fb = new FileBrowser(getContext(), drive, new String[] {".xml"});
					
					fb.setCaption("fb_language_packs");
					fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
					{					
						@Override
						public void onPick(String selected)
						{			
							Localization.loadLanguageTestFile(selected);
						}
					});
					
					fb.show();
				}
			});	
		}
		catch(Exception exc)
		{
			MessageInfo.info("gls_msg_language_pack_failed_load");
		}
	}
	
	//filebrowser size
	private void setFileBrowserSizeDescription(ImageSize size)
	{
		switch (size)
		{
			case predefined:
			{
				filebrowserSizeText.setText(Localization.getString("common_default"));
				break;
			}
			case very_small:
			{
				filebrowserSizeText.setText(Localization.getString("size_small"));
				break;
			}
			case small:
			{
				filebrowserSizeText.setText(Localization.getString("size_medium"));
				break;
			}
			case small_medium:
			{
				filebrowserSizeText.setText(Localization.getString("size_large"));
				break;
			}
		default:
			break;								    	
		}
		
		filebrowserSizeText.setTag(size);
		FileBrowser.setItemsSize(size);
	}
	
	private void increaseFileBrowserSize()
	{
		ImageSize size = (ImageSize)filebrowserSizeText.getTag();
		
		if (size == ImageSize.small_medium)
		{
			return;
		}

		if (size == ImageSize.predefined)
		{
			size = ImageSize.very_small;
		}
		else if (size == ImageSize.very_small)
		{
			size = ImageSize.small;
		} 
		else if (size == ImageSize.small)
		{
			size = ImageSize.small_medium;
		}
		
		setFileBrowserSizeDescription(size);
	}
	
	private void decreaseFileBrowserSize()
	{
		ImageSize size = (ImageSize)filebrowserSizeText.getTag();
		
		if (size == ImageSize.predefined)
		{
			return;
		}

		if (size == ImageSize.small_medium)
		{
			size = ImageSize.small;
		} 
		else if (size == ImageSize.small)
		{
			size = ImageSize.very_small;
		}
		else if (size == ImageSize.very_small)
		{
			size = ImageSize.predefined;
		}

		setFileBrowserSizeDescription(size);
	}
	
	//image viewer size
	private void setImageViewerSizeDescription(ImageSize size)
	{
		switch (size)
		{
			case predefined:
			{
				imgViewerSizeText.setText(Localization.getString("common_default"));
				break;
			}
			case small:
			{
				imgViewerSizeText.setText(Localization.getString("size_small"));
				break;
			}
			case small_medium:
			{
				imgViewerSizeText.setText(Localization.getString("size_medium"));
				break;
			}
			case medium:
			{
				imgViewerSizeText.setText(Localization.getString("size_large"));
				break;
			}
		default:
			break;								    	
		}
		
		imgViewerSizeText.setTag(size);
		ImageViewer.setImageSize(size);
	}
	
	private void increaseImageViewerSize()
	{
		ImageSize size = (ImageSize)imgViewerSizeText.getTag();
		
		if (size == ImageSize.medium)
		{
			return;
		}

		if (size == ImageSize.predefined)
		{
			size = ImageSize.small;
		}
		else if (size == ImageSize.small)
		{
			size = ImageSize.small_medium;
		} 
		else if (size == ImageSize.small_medium)
		{
			size = ImageSize.medium;
		}
		
		setImageViewerSizeDescription(size);
	}
	
	private void decreaseImageViewerSize()
	{
		ImageSize size = (ImageSize)imgViewerSizeText.getTag();
		
		if (size == ImageSize.predefined)
		{
			return;
		}

		if (size == ImageSize.medium)
		{
			size = ImageSize.small_medium;
		} 
		else if (size == ImageSize.small_medium)
		{
			size = ImageSize.small;
		}
		else if (size == ImageSize.small)
		{
			size = ImageSize.predefined;
		}
		
		setImageViewerSizeDescription(size);
	}

	public void setOnConfirmEventListener(OnConfirmEventListener event)
	{
		this.event = event;
	}
}