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
import java.util.UUID;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;

import magiclib.IO.FileBrowser;
import magiclib.IO.Storages;
import magiclib.core.CDROMItem;
import magiclib.controls.Dialog;
import magiclib.keyboard.KeyCodeInfo;
import magiclib.logging.MessageInfo;

abstract interface CDROMEventListener
{
	public abstract void onPick(CDROMItem selected);
}

public class uiCDROM extends Dialog
{
	private CDROMItem cdrom;
	
	private EditText label;
	private Button driveLetter;
	private CheckBox imageSource;
	private CheckBox fldSource;
	private EditText sourcePath;

	//events
	private CDROMEventListener CDROMEvent = null;
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.activity_game_starter_edit_cdrom_label_caption, "cdrom_cdlabel");
		localize(R.id.activity_game_starter_edit_cdrom_driveletter_caption, "cdrom_driveletter");
		localize(R.id.activity_game_starter_edit_cdrom_map_caption, "cdrom_map");
		
		localize(R.id.activity_game_starter_edit_cdrom_image, "cdrom_image");
		localize(R.id.activity_game_starter_edit_cdrom_folder, "cdrom_folder");
		
		localize(R.id.activity_game_starter_edit_cdrom_sourcepath_caption, "cdrom_sourcepath");
		localize(R.id.activity_game_starter_edit_cdrom_choosesource, "common_choose");
	}
	
	public uiCDROM(CDROMItem cdrom)
	{
		super(AppGlobal.context);
				
		setContentView(R.layout.activity_game_starter_edit_cdrom);
		setCaption("cdrom_caption");
		
		ImageButton confirm = (ImageButton)findViewById(R.id.activity_game_starter_edit_cdrom_confirm);
		label = (EditText)findViewById(R.id.activity_game_starter_edit_cdrom_label);
		driveLetter = (Button)findViewById(R.id.activity_game_starter_edit_cdrom_driveletter);		
		imageSource = (CheckBox)findViewById(R.id.activity_game_starter_edit_cdrom_image);
		fldSource = (CheckBox)findViewById(R.id.activity_game_starter_edit_cdrom_folder);
		Button chooseSource = (Button)findViewById(R.id.activity_game_starter_edit_cdrom_choosesource);
		sourcePath = (EditText)findViewById(R.id.activity_game_starter_edit_cdrom_source);
		
		confirm.setOnClickListener(confirm());
		driveLetter.setOnClickListener(chooseDriveLetter());		
		imageSource.setOnCheckedChangeListener(imgOrFld());
		fldSource.setOnCheckedChangeListener(imgOrFld());
		chooseSource.setOnClickListener(chooseSource());
		
		if (cdrom == null)
		{			
			cdrom =  new CDROMItem();
			cdrom.setId(UUID.randomUUID().toString());
		}
			
		//temp fix
		if (cdrom.getId().equals(""))
			cdrom.setId(UUID.randomUUID().toString());
		
		this.cdrom = cdrom;

		label.setText(cdrom.getLabel());
		driveLetter.setText(cdrom.getDriveLetter());

		if (cdrom.isMappedImage())
		{
			imageSource.setChecked(true);
		} else
		{
			fldSource.setChecked(true);
		}

		sourcePath.setText(cdrom.getSourcePath());	
	}
	
	private View.OnClickListener confirm()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{			
				//validation
				if (label.getText().toString().trim().equals(""))
				{
					MessageInfo.info("cdrom_msg_missinglabel");
					return;
				}
				
				if (label.getText().toString().trim().equals(""))
				{
					MessageInfo.info("cdrom_msg_missingdrive");
					return;
				}
				
				String p = sourcePath.getText().toString().trim();
				
				if (imageSource.isChecked())
				{					
					if (p.equals(""))
					{
						MessageInfo.info("cdrom_msg_missingimage");
						return;
					}
										
					File f = new File(p);
					
					if (!f.isFile())
					{
						MessageInfo.info("cdrom_msg_wrongimgsource");
						return;
					}
				}
				else
				{
					if (p.equals(""))
					{
						MessageInfo.info("cdrom_msg_emptysource");
						return;						
					}
					
					File f = new File(p);
					
					if (f.isFile())
					{
						MessageInfo.info("cdrom_msg_wrongfldsource");
						return;						
					}					
				}
				
				//validation is successful
				cdrom.setLabel(label.getText().toString());
				cdrom.setDriveLetter(driveLetter.getText().toString());
				cdrom.setMapImage(imageSource.isChecked());
				cdrom.setSourcePath(sourcePath.getText().toString());
				
				if (CDROMEvent != null)
				{
					CDROMEvent.onPick(cdrom);
				}
				
				dismiss();
			}
		};
	}
	
	private View.OnClickListener chooseDriveLetter()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{				
				uiKeyCodesDialog dlg = new uiKeyCodesDialog(getContext(), false);
				dlg.setCaption("fb_caption_choose_drive");
				dlg.setOnKeyCodeListener(new KeyCodeListener()
				{							
					@Override
					public void onPick(KeyCodeItem selected)
					{
						driveLetter.setText(KeyCodeInfo.getDosboxKeyInfo(selected.getKeyCode(), true)+":");
					}
				});				
								
				dlg.show();
			}
		};
	}
	
	private boolean inCheck = false;
	
	private CompoundButton.OnCheckedChangeListener imgOrFld()
	{
		return new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton v,
					boolean isChecked)
			{
				if (inCheck)
					return;
				
				inCheck = true;
				
				switch (v.getId())
				{
					case R.id.activity_game_starter_edit_cdrom_image:
					{						
						imageSource.setChecked(true);
						fldSource.setChecked(false);						
						break;
					}
					case R.id.activity_game_starter_edit_cdrom_folder:
					{
						imageSource.setChecked(false);
						fldSource.setChecked(true);						
						break;
					}					
				}
				
				inCheck = false;
			}
		};
	}
		
	private View.OnClickListener chooseSource()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{							
				Storages.onDrivePick(getContext(), new Storages.onDrivePickListener() {
					@Override
					public void onPick(String drive) {
						FileBrowser fb = null;

						if (imageSource.isChecked()) {
							fb = new FileBrowser(getContext(), drive, new String[]{".iso", ".ISO", ".bin", ".BIN", ".cue", ".CUE",
									//gog files : gog = iso/bin, inst = cue
									".gog", ".GOG", ".inst", ".INST"}, false);
							fb.setCaption("fb_caption_choose_iso_bin_cue");
						} else {
							fb = new FileBrowser(getContext(), drive, null, true);
							fb.setCaption("fb_caption_choose_folder");
						}

						fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
							@Override
							public void onPick(String selected) {
								sourcePath.setText(selected);
							}
						});

						fb.show();
					}
				});
			}
		};
	}

	public void setOnCDROMEventListener(CDROMEventListener event)
	{
		this.CDROMEvent = event;
	}
	
	@Override
	protected void onStop()
	{
		if (CDROMEvent != null)
		{
			CDROMEvent = null;
		}
		
		super.onStop();
	}
}