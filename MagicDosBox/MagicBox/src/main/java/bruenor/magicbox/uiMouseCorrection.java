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

import magiclib.controls.Dialog;
import magiclib.controls.ZoomButton;
import magiclib.core.EmuConfig;
import magiclib.core.NativeControl;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

abstract interface MouseCorrectionEventListener
{
	public abstract void onChange(boolean enabled, int width, int height);
}

class uiMouseCorrection extends Dialog
{
	private MouseCorrectionEventListener event;
	
	//resolution
	private CheckBox mouse_max_enabled;
	private Button mouse_res_width;
	private ZoomButton mouse_res_width_plus;
	private ZoomButton mouse_res_width_minus;
	private TextView mouseVideoRes;
	
	private Button mouse_res_height;
	private ZoomButton mouse_res_height_plus;
	private ZoomButton mouse_res_height_minus;	
			
	//temporary values
	private int mouse_max_width;
	private int mouse_max_height;
		
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.mouse_sensitivity_mouseres_enabled,      "common_enabled");
		localize(R.id.mouse_sensitivity_mousevideores_title, "mousecorr_mousevideores_title");		
		localize(R.id.mouse_sensitivity_mouseres_width,      "mousecorr_mouseres_width");
		localize(R.id.mouse_sensitivity_mouseres_height,     "mousecorr_mouseres_height");
	};	
	
	public uiMouseCorrection(Context context, boolean resolutionEnabled, int resolutionWidth, int resolutionHeight)
	{
		super(context);
		
		setContentView(R.layout.mouse_sensitivity);
		setCaption("mousecorr_mouseres_title");
		
		mouse_max_width = resolutionWidth;
		mouse_max_height = resolutionHeight;

		if (mouse_max_width < 1)
			mouse_max_width = NativeControl.nativeGetMouseMaxX();
		
		if (mouse_max_height < 1)
			mouse_max_height = NativeControl.nativeGetMouseMaxY();
				
		mouse_max_width++;
		mouse_max_height++;
		
		View.OnClickListener defaultClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{				
				switch (v.getId())
				{					
					case R.id.mouse_sensitivity_mouseres_widthdefault:
					{
						mouse_max_width = NativeControl.nativeGetMouseMaxX() + 1;
						mouse_res_width.setText("" +  mouse_max_width);
						break;
					}
					case R.id.mouse_sensitivity_mouseres_heightdefault:
					{
						mouse_max_height = NativeControl.nativeGetMouseMaxY() + 1;
						mouse_res_height.setText("" + mouse_max_height);
						break;
					}					
					
				}
			}			
		};
		
		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{				
				switch (v.getId())
				{
					//mouse resolution
					case R.id.mouse_sensitivity_mouseres_widthplus:
					{
						mouse_max_width++;
																		
						mouse_res_width.setText("" + mouse_max_width);
						break;
					}
					case R.id.mouse_sensitivity_mouseres_widthminus:
					{
						mouse_max_width--;
						
						if (mouse_max_width < 0)
							mouse_max_width = 0;
												
						mouse_res_width.setText("" + mouse_max_width);
						break;
					}
					case R.id.mouse_sensitivity_mouseres_heightplus:
					{
						mouse_max_height++;
																		
						mouse_res_height.setText("" + mouse_max_height);
						break;
					}
					case R.id.mouse_sensitivity_mouseres_heightminus:
					{
						mouse_max_height--;
						
						if (mouse_max_height < 0)
							mouse_max_height = 0;
																		
						mouse_res_height.setText("" + mouse_max_height);
						break;
					}					
				}
			}			
		};
				
		//resolution
		mouseVideoRes = (TextView)findViewById(R.id.mouse_sensitivity_mouseresvideo);
		mouseVideoRes.setText("(" + MagicLauncher.nativeGetMouseVideoWidth() + "," + MagicLauncher.nativeGetMouseVideoHeight() + ")");
		
		mouse_max_enabled = (CheckBox)findViewById(R.id.mouse_sensitivity_mouseres_enabled);		
		//mouse_max_enabled.setChecked(uiGameItemDetail.mouse_max_enabled);
		mouse_max_enabled.setChecked(resolutionEnabled);
		
		mouse_res_width = (Button)findViewById(R.id.mouse_sensitivity_mouseres_widthdefault);
		mouse_res_width_plus = (ZoomButton)findViewById(R.id.mouse_sensitivity_mouseres_widthplus);
		mouse_res_width_minus = (ZoomButton)findViewById(R.id.mouse_sensitivity_mouseres_widthminus);
		
		mouse_res_height = (Button)findViewById(R.id.mouse_sensitivity_mouseres_heightdefault);
		mouse_res_height_plus = (ZoomButton)findViewById(R.id.mouse_sensitivity_mouseres_heightplus);
		mouse_res_height_minus = (ZoomButton)findViewById(R.id.mouse_sensitivity_mouseres_heightminus);
		
		mouse_res_width_plus.setZoomSpeed(5);
		mouse_res_width_minus.setZoomSpeed(5);
		mouse_res_height_plus.setZoomSpeed(5);
		mouse_res_height_minus.setZoomSpeed(5);
		
		//default click				
		mouse_res_width.setOnClickListener(defaultClick);
		mouse_res_height.setOnClickListener(defaultClick);
		
		//+/-	
		//resolution scale
		mouse_res_width_plus.setOnClickListener(buttonClick);
		mouse_res_width_minus.setOnClickListener(buttonClick);
		
		mouse_res_height_plus.setOnClickListener(buttonClick);
		mouse_res_height_minus.setOnClickListener(buttonClick);
				
		//init	
		mouse_res_width.setText("" + mouse_max_width);
		mouse_res_height.setText("" + mouse_max_height);
										
		ImageButton bSave = (ImageButton)findViewById(R.id.mouse_sensitivity_confirm);
		bSave.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{					
				if (event != null)
				{
					if ((EmuConfig.mouse_max_enabled != mouse_max_enabled.isChecked()) ||
						((EmuConfig.mouse_max_width + 1) != mouse_max_width)||
						((EmuConfig.mouse_max_height + 1) != mouse_max_height))
					{
						
						event.onChange(mouse_max_enabled.isChecked(), mouse_max_width - 1, mouse_max_height - 1);
					}
				}
				
				dismiss();
			}
		});
	}

	public void setOnMouseCorrectionEventListener(MouseCorrectionEventListener event)
	{
		this.event = event;
	}
}
