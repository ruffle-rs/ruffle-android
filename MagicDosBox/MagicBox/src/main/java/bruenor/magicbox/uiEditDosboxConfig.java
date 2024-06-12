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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import magiclib.controls.Dialog;
import magiclib.dosbox.DosboxConfig;

abstract interface EditDosBoxConfigListener
{
	public abstract void onSave(String config);
}

public class uiEditDosboxConfig extends Dialog
{
	EditText config = null;
	private EditDosBoxConfigListener eventListener = null;
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.edit_dosbox_config_default, "common_default");
	}
	
	public uiEditDosboxConfig(Context context)
	{
		super(context);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		setContentView(R.layout.edit_dosbox_config);
		setCaption("editconfig_caption");
		
		config = (EditText)getView().findViewById(R.id.edit_dosbox_config_text);
		
		ImageButton button = (ImageButton)getView().findViewById(R.id.edit_dosbox_config_confirm);
		button.setOnClickListener(getConfirmEvent());
		
		Button butDefault = (Button)getView().findViewById(R.id.edit_dosbox_config_default);
		butDefault.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				setConfig(DosboxConfig.generateDefaultDosboxConfig());
			}
		});
	}

	private View.OnClickListener getConfirmEvent()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if (eventListener != null)
				{
					eventListener.onSave(config.getText().toString());
				}
				
				dismiss();
			}
		};
	}	
	
	public void setConfig(String config)
	{
		this.config.setText(config);
	}

	public void setOnEditDosBoxConfigListener(EditDosBoxConfigListener event)
	{
		this.eventListener = event;
	}
}
