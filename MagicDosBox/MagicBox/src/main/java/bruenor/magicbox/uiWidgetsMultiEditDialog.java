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

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import magiclib.controls.Dialog;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.ColorPicker;
import magiclib.core.ColorPickerItem;
import magiclib.gui_modes.DesignMode;
import magiclib.gui_modes.WidgetConfigurationDialog;
import magiclib.layout.widgets.Folder;
import magiclib.layout.widgets.FolderDialog;
import magiclib.layout.widgets.Widget;
import magiclib.locales.Localization;

class WidgetsMultiEditDialog extends WidgetConfigurationDialog
{
	private ImageView imagePreview;
	private ImageView backgroundColorPreview;	
	private GradientDrawable gradBgrColor;
	private GradientDrawable gradTextColor;
	private ImageView backgroundImage;
	private ImageView backgroundImagePreview;
	private SeekBar imageOpacity;
	private SeekBar backgroundOpacity;
	private SeekBar backgroundImageOpacity;
	private TextView imageOpacityValue;
	private TextView backgroundOpacityValue;
	private TextView backgroundImageOpacityValue;
	private TextView textPreview;
	
	//image
	private boolean temp_bitmapEnabled;
	private int temp_bitmapOpacity = 255;
	
	//background
	private boolean temp_bgrColorEnabled;
	private int temp_backgroundColor;
	private int temp_backgroundOpacity;
		
	//background image
	private boolean temp_bgrBitmapEnabled;
	private String temp_bgrBitmapName;
	private int temp_bgrBitmapOpacity;
	
	//text	
	private boolean temp_textEnabled;
	private int temp_textColor;

	//FLAGS
	//image
	private CheckBox changedBitmapEnabled;
	private CheckBox changedBitmapOpacity;

	//background
	private CheckBox changedBgrColorEnabled;
	private CheckBox changedBackgroundColor;
	private CheckBox changedBackgroundOpacity;
	
	//background image
	private CheckBox changedBgrBitmapEnabled;
	private CheckBox changedBgrBitmapName;
	private CheckBox changedBgrBitmapOpacity;
	
	//text	
	private CheckBox changedTextEnabled;
	private CheckBox changedTextColor;	
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.widgets_multiedit_lookandfeel_title,                 "common_lookandfeel");
		localize(R.id.widgets_multiedit_graphics_title,                    "widget_edit_header_lookandfeel_image_caption");
		localize(R.id.widgets_multiedit_preview_image_text,                "widget_edit_header_lookandfeel_image_preview");
		//localize(R.id.widgets_multiedit_preview_text,                      "common_textcolor");
		localize(R.id.widgets_multiedit_background_caption,                "widget_edit_header_lookandfeel_imgbackgroud_caption");
		localize(R.id.widgets_multiedit_backgroundimg_caption,             "widget_edit_header_lookandfeel_imgbackgroudimg_caption");
		localize(R.id.widgets_multiedit_opacity_title,                     "common_opacity");
		localize(R.id.widgets_multiedit_imageopacity_title,                "widget_edit_header_lookandfeel_opacity_image");
		localize(R.id.widgets_multiedit_backgropacity_title,               "widget_edit_header_lookandfeel_opacity_background");
		localize(R.id.widgets_multiedit_backgrimageopacity_title,          "widget_edit_header_lookandfeel_opacity_backgroundimg");
		localize(R.id.widgets_multiedit_text_title,                        "widget_edit_header_lookandfeel_text_caption");
		localize(R.id.widgets_multiedit_text_enabled,                      "common_enabled");
		localize(R.id.widgets_multiedit_textcolor_title,                   "common_textcolor");
		localize(R.id.widgets_multiedit_changes_title,                     "widgets_multiedit_changes_title");
		localize(R.id.widgets_multiedit_changes_image_title,               "common_image");
		localize(R.id.widgets_multiedit_changes_image_enabled_title,       "common_enabled");
		localize(R.id.widgets_multiedit_changes_image_opacity_title,       "common_opacity");
		localize(R.id.widgets_multiedit_changes_backgrimage_title,         "widget_edit_header_lookandfeel_opacity_backgroundimg");
		localize(R.id.widgets_multiedit_changes_backgrimage_enabled_title, "common_enabled");
		localize(R.id.widgets_multiedit_changes_backgrimage_image_title,   "common_image");
		localize(R.id.widgets_multiedit_changes_backgrimage_opacity_title, "common_opacity");
		localize(R.id.widgets_multiedit_changes_background_title,          "widget_edit_header_lookandfeel_opacity_background");
		localize(R.id.widgets_multiedit_changes_background_enabled_title,  "common_enabled");
		localize(R.id.widgets_multiedit_changes_background_color_title,    "common_color");
		localize(R.id.widgets_multiedit_changes_background_opacity_title,  "common_opacity");
		localize(R.id.widgets_multiedit_changes_text_title,                "widget_edit_header_lookandfeel_text_caption");
		localize(R.id.widgets_multiedit_changes_text_enabled_title,        "common_enabled");
		localize(R.id.widgets_multiedit_changes_text_color_title,          "common_color");
	}
	
	public WidgetsMultiEditDialog(Context context, Widget selected)
	{
		super(AppGlobal.context, selected);
		
		setContentView(R.layout.widgets_multiedit);
		setCaption("widgets_multiedit_caption");
		
		this.button = selected;
		
		//image
		temp_bitmapEnabled = selected.isBitmapEnabled();
		
		if (!selected.getBitmapName().equals(""))
		{
			temp_bitmapOpacity = selected.getBitmap().getTransparency();
		}
		
		setImageSettings();
		
		//background
		temp_bgrColorEnabled = selected.isBackgroundColorEnabled();
		temp_backgroundColor = selected.backgroundColor;
		temp_backgroundOpacity = selected.getTransparency();
		
		setBackgroundSettings();
		
		//background image
		temp_bgrBitmapEnabled = selected.isBackgroundBitmapEnabled();
		temp_bgrBitmapName = selected.getBackgroundBitmap();
		temp_bgrBitmapOpacity = selected.getBackgroundBitmapTransparency();
		
		setBackgroundImageSettings();
		
		//text	
		temp_textEnabled = selected.isTextEnabled();
		temp_textColor = selected.getTextData().getTextColor();
		
		setTextSettings();
		
		CompoundButton.OnCheckedChangeListener cbxListener = new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton v, boolean isChecked)
			{
				switch (v.getId())
				{				
					case R.id.widgets_multiedit_backgroundcolor_enabled:
					{
						temp_bgrColorEnabled = isChecked;
						backgroundColorPreview.setVisibility(isChecked?View.VISIBLE:View.INVISIBLE);
						changedBgrColorEnabled.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_image_enabled:
					{
						temp_bitmapEnabled = isChecked;
						
						if (!temp_bitmapEnabled)
						{
							imagePreview.setVisibility(View.INVISIBLE);
						}
						else
						{
							imagePreview.setVisibility(View.VISIBLE);
						}
						
						changedBitmapEnabled.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_backgroundtexture_enabled:
					{
						temp_bgrBitmapEnabled = isChecked;	
						
						backgroundImagePreview.setVisibility((temp_bgrBitmapEnabled)?View.VISIBLE:View.INVISIBLE);
						changedBgrBitmapEnabled.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_text_enabled:
					{
						temp_textEnabled = isChecked;	
						changedTextEnabled.setChecked(true);
						break;
					}
				}				
			}
		};

		CheckBox cbx = (CheckBox)findViewById(R.id.widgets_multiedit_image_enabled);
		cbx.setChecked(temp_bitmapEnabled);
		cbx.setOnCheckedChangeListener(cbxListener);		
						
		cbx = (CheckBox)findViewById(R.id.widgets_multiedit_backgroundcolor_enabled);
		cbx.setChecked(temp_bgrColorEnabled);
		cbx.setOnCheckedChangeListener(cbxListener);
		
		cbx = (CheckBox)findViewById(R.id.widgets_multiedit_backgroundtexture_enabled);
		cbx.setChecked(temp_bgrBitmapEnabled);
		cbx.setOnCheckedChangeListener(cbxListener);
		
		cbx = (CheckBox)findViewById(R.id.widgets_multiedit_text_enabled);
		cbx.setChecked(temp_textEnabled);
		cbx.setOnCheckedChangeListener(cbxListener);
		
		setOpacitySettings();
		setFlags();
		
		ImageButton confirm = (ImageButton)findViewById(R.id.widgets_multiedit_confirm);
		confirm.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				applyMultiChanges();
				dismiss();
			}
		});
	}
	
	private void setTextSettings()
	{
		textPreview = (TextView)findViewById(R.id.widgets_multiedit_preview_text);
		
		if (!button.getText().trim().equals(""))
		{
			textPreview.setText(button.getText());
		}
		else
			textPreview.setText(Localization.getString("common_textcolor"));
		
		gradTextColor = new GradientDrawable();
		gradTextColor.setShape(GradientDrawable.RECTANGLE);	
		gradTextColor.setColor(temp_textColor);
		gradTextColor.setStroke(5, Color.LTGRAY);
		
		final Button textColor = (Button)findViewById(R.id.widgets_multiedit_textcolor);
		textColor.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				ColorPicker dlg = new ColorPicker(getContext(), temp_textColor);
				dlg.setCaption("common_textcolor");
				dlg.setOnColorPickListener(new ColorPicker.ColorPickListener()
				{					
					@Override
					public void onPick(ColorPickerItem selected)
					{
						temp_textColor = selected.getColor();
												
						gradTextColor.setColor(temp_textColor);
						AppGlobal.setBackgroundDrawable(textColor, gradTextColor);
						textPreview.setTextColor(temp_textColor);
						changedTextColor.setChecked(true);
					}
				});
				
				dlg.show();				
			}
		});
		
		AppGlobal.setBackgroundDrawable(textColor, gradTextColor);
		textPreview.setTextColor(temp_textColor);
	}
	
	private void setImageSettings()
	{
		imagePreview = (ImageView) findViewById(R.id.widgets_multiedit_preview_image);
		imagePreview.setImageBitmap(AppGlobal.getBitmapFromWidget(button, true));
		
		AppGlobal.setAlpha(imagePreview, temp_bitmapOpacity);
	}
	
	private void setBackgroundSettings()
	{
		gradBgrColor = new GradientDrawable();
		gradBgrColor.setShape(GradientDrawable.RECTANGLE);
		gradBgrColor.setAlpha(temp_backgroundOpacity);
		gradBgrColor.setColor(temp_backgroundColor);

		backgroundColorPreview = (ImageView) findViewById(R.id.widgets_multiedit_preview_backgroundcolor);
		AppGlobal.setBackgroundDrawable(backgroundColorPreview, gradBgrColor);
		backgroundColorPreview.setVisibility(temp_bgrColorEnabled?View.VISIBLE:View.INVISIBLE);
		
		final GradientDrawable drawable = new GradientDrawable();
		drawable.setShape(GradientDrawable.RECTANGLE);
		drawable.setColor(temp_backgroundColor);
		drawable.setStroke(2, Color.LTGRAY);
		
		Button background = (Button) findViewById(R.id.widgets_multiedit_backgroundcolor);
		AppGlobal.setBackgroundDrawable(background, drawable);
		
		background.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ColorPicker dlg = new ColorPicker(getContext(), temp_backgroundColor);
				dlg.setCaption("common_bckcolor");
				dlg.setOnColorPickListener(new ColorPicker.ColorPickListener()
				{
					@Override
					public void onPick(ColorPickerItem selected)
					{
						temp_backgroundColor = selected.getColor();
						drawable.setColor(temp_backgroundColor);
						gradBgrColor.setColor(temp_backgroundColor);
						changedBackgroundColor.setChecked(true);
					}
				});

				dlg.show();
			}
		});
	}
	
	private void setBackgroundImageSettings()
	{
		backgroundImage = (ImageView) findViewById(R.id.widgets_multiedit_backgroundtexture);
		backgroundImagePreview = (ImageView) findViewById(R.id.widgets_multiedit_preview_backgroundtexture);
		
		backgroundImage.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				uiImageViewer viewer = new uiImageViewer(getContext());
				viewer.setCaption("imgview_caption_choose_backgrimage");
				viewer.loadBackgrounds();

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener()
				{
					@Override
					public void onPick(ImageViewerItem selected)
					{
						Bitmap bitmap = AppGlobal.getBitmapFromImageViewerItem(selected);
						temp_bgrBitmapName = selected.getName();

						backgroundImage.setImageBitmap(bitmap);
						backgroundImagePreview.setImageBitmap(bitmap);
						changedBgrBitmapName.setChecked(true);
					}
				});

				viewer.show();
			}
		});
		
		temp_bgrBitmapOpacity = button.getBackgroundBitmapTransparency();
		temp_bgrBitmapName = button.getBackgroundBitmap();
		temp_bgrBitmapEnabled = button.isBackgroundBitmapEnabled();
		
		Bitmap bitmap = AppGlobal.getBgrBitmapFromWidget(button, true);
		
		backgroundImage.setImageBitmap(bitmap);
		backgroundImagePreview.setImageBitmap(bitmap);
		backgroundImagePreview.setVisibility((temp_bgrBitmapEnabled)?View.VISIBLE:View.INVISIBLE);
		
		AppGlobal.setAlpha(backgroundImagePreview, temp_bgrBitmapOpacity);
	}
	
	private void setOpacitySettings()
	{
		imageOpacityValue = (TextView)findViewById(R.id.widgets_multiedit_imageopacity_value);
		backgroundOpacityValue = (TextView)findViewById(R.id.widgets_multiedit_backgropacity_value);
		backgroundImageOpacityValue = (TextView)findViewById(R.id.widgets_multiedit_backgrimageopacity_value);
		
		OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener()
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
				// TODO Auto-generated method stub				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
				// TODO Auto-generated method stub				
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				switch (seekBar.getId())
				{
					case R.id.widgets_multiedit_imageopacity_seek:
					{
						temp_bitmapOpacity = progress;

						AppGlobal.setAlpha(imagePreview, temp_bitmapOpacity);

						imageOpacityValue.setText("" + temp_bitmapOpacity);	
						changedBitmapOpacity.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_backgropacity_seek:
					{
						temp_backgroundOpacity = progress;

						gradBgrColor.setAlpha(temp_backgroundOpacity);
						
						backgroundOpacityValue.setText("" + temp_backgroundOpacity);
						changedBackgroundOpacity.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_backgrimageopacity_seek:
					{
						temp_bgrBitmapOpacity = progress;

						AppGlobal.setAlpha(backgroundImagePreview, temp_bgrBitmapOpacity);

						backgroundImageOpacityValue.setText("" + temp_bgrBitmapOpacity);
						changedBgrBitmapOpacity.setChecked(true);
						break;
					}					
				}
			}
		};
		
		imageOpacity = (SeekBar) findViewById(R.id.widgets_multiedit_imageopacity_seek);
		imageOpacity.setMax(255);
		imageOpacity.setProgress(temp_bitmapOpacity);
		imageOpacity.setOnSeekBarChangeListener(seekListener);
		
	 	backgroundOpacity = (SeekBar) findViewById(R.id.widgets_multiedit_backgropacity_seek);
	 	backgroundOpacity.setMax(255);
	 	backgroundOpacity.setProgress(temp_backgroundOpacity);
	 	backgroundOpacity.setOnSeekBarChangeListener(seekListener);	 	
	 	
	 	backgroundImageOpacity = (SeekBar) findViewById(R.id.widgets_multiedit_backgrimageopacity_seek);
	 	backgroundImageOpacity.setMax(255);
	 	backgroundImageOpacity.setProgress(temp_bgrBitmapOpacity);
	 	backgroundImageOpacity.setOnSeekBarChangeListener(seekListener);	 	

		imageOpacityValue.setText("" + temp_bitmapOpacity);
		backgroundOpacityValue.setText("" + temp_backgroundOpacity);
		backgroundImageOpacityValue.setText("" + temp_bgrBitmapOpacity);	 	
	 	
		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					//image
					case R.id.widgets_multiedit_imageopacity_minus:
					{
						if (temp_bitmapOpacity == 0)
							return;

						temp_bitmapOpacity--;
						
						changedBitmapOpacity.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_imageopacity_plus:
					{
						if (temp_bitmapOpacity == 255)
							return;

						temp_bitmapOpacity++;
						changedBitmapOpacity.setChecked(true);
						break;
					}					
					//background
					case R.id.widgets_multiedit_backgropacity_minus:
					{
						if (temp_backgroundOpacity == 0)
							return;

						temp_backgroundOpacity--;
						changedBackgroundOpacity.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_backgropacity_plus:
					{
						if (temp_backgroundOpacity == 255)
							return;

						temp_backgroundOpacity++;
						changedBackgroundOpacity.setChecked(true);
						break;
					}				
					//background image
					case R.id.widgets_multiedit_backgrimageopacity_minus:
					{
						if (temp_bgrBitmapOpacity == 0)
							return;

						temp_bgrBitmapOpacity--;
						changedBgrBitmapOpacity.setChecked(true);
						break;
					}
					case R.id.widgets_multiedit_backgrimageopacity_plus:
					{
						if (temp_bgrBitmapOpacity == 255)
							return;

						temp_bgrBitmapOpacity++;
						changedBgrBitmapOpacity.setChecked(true);
						break;
					}
				}
			}
		};
		
		//image
		ImageButton zoom = (ImageButton) findViewById(R.id.widgets_multiedit_imageopacity_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.widgets_multiedit_imageopacity_plus);
		zoom.setOnClickListener(buttonClick);
		
		//background
		zoom = (ImageButton) findViewById(R.id.widgets_multiedit_backgropacity_minus);
		zoom.setOnClickListener(buttonClick);
		
		zoom = (ImageButton) findViewById(R.id.widgets_multiedit_backgropacity_plus);
		zoom.setOnClickListener(buttonClick);
		
		//background image
		zoom = (ImageButton) findViewById(R.id.widgets_multiedit_backgrimageopacity_minus);
		zoom.setOnClickListener(buttonClick);
		
		zoom = (ImageButton) findViewById(R.id.widgets_multiedit_backgrimageopacity_plus);
		zoom.setOnClickListener(buttonClick);		
	}
	
	private void setFlags()
	{
		//image
		changedBitmapEnabled = (CheckBox)findViewById(R.id.widgets_multiedit_changes_image_enabled_title);
		changedBitmapOpacity = (CheckBox)findViewById(R.id.widgets_multiedit_changes_image_opacity_title);

		//background
		changedBgrColorEnabled = (CheckBox)findViewById(R.id.widgets_multiedit_changes_background_enabled_title);
		changedBackgroundColor = (CheckBox)findViewById(R.id.widgets_multiedit_changes_background_color_title);
		changedBackgroundOpacity = (CheckBox)findViewById(R.id.widgets_multiedit_changes_background_opacity_title);		
		
		//background image
		changedBgrBitmapEnabled = (CheckBox)findViewById(R.id.widgets_multiedit_changes_backgrimage_enabled_title);
		changedBgrBitmapName = (CheckBox)findViewById(R.id.widgets_multiedit_changes_backgrimage_image_title);
		changedBgrBitmapOpacity = (CheckBox)findViewById(R.id.widgets_multiedit_changes_backgrimage_opacity_title);		
		
		//text	
		changedTextEnabled = (CheckBox)findViewById(R.id.widgets_multiedit_changes_text_enabled_title);
		changedTextColor = (CheckBox)findViewById(R.id.widgets_multiedit_changes_text_color_title);
		
		//set defaults
		changedBitmapEnabled.setChecked(false);
		changedBitmapOpacity.setChecked(false);

		//background image
		changedBgrBitmapEnabled.setChecked(false);
		changedBgrBitmapName.setChecked(false);
		changedBgrBitmapOpacity.setChecked(false);		
		
		//background
		changedBgrColorEnabled.setChecked(false);
		changedBackgroundColor.setChecked(false);
		changedBackgroundOpacity.setChecked(false);
		
		//text	
		changedTextEnabled.setChecked(false);
		changedTextColor.setChecked(false);		
	}
/*
	private void applyMultiChanges()
	{
		List<Widget> list = DesignMode.getSelectedList();
		
		for (Widget widget : list)
		{
			//damn, this is hardcoded. Should be much better to store selected widgets on uiFolderDialog side, not uiFolder. Then I can iterate through selected children. 
			//But I don't care much now. must rewrite it in the future
			if (widget instanceof FolderDialog)
			{
				Folder fld = ((FolderDialog)widget).folder;
				
				if (fld.getSelectedList() != null && fld.getSelectedList().size() > 0)
				{	
					for(Widget child : fld.getSelectedList())
					{
						updateWidget(child);
					} 
				}
			}
			else
			{
				updateWidget(widget);
			}
		}	
	}*/

	private void applyMultiChanges()
	{
		List<Widget> list = DesignMode.getSelectedList();

		for (Widget widget : list)
		{
			if (widget.hasSelectedChildren())
			{
				for(Widget child : widget.getSelectedChildren()) {
					updateWidget(child);
				}
			}
			else
			{
				updateWidget(widget);
			}
		}
	}

	private void updateWidget(Widget widget)
	{
		boolean wasChange = false;
		
		//image
		if (changedBitmapEnabled.isChecked() && widget.isBitmapEnabled() != temp_bitmapEnabled)
		{
			widget.setBitmapEnabled(temp_bitmapEnabled);
			wasChange = true;
		}
		
		if (changedBitmapOpacity.isChecked() && widget.getBitmap() != null)
		{
			widget.getBitmap().setTransparency(temp_bitmapOpacity);
			
			if (!temp_bitmapEnabled)
			{
				widget.getBitmap().clear();
			}
			
			wasChange = true;
		}
		
		//background		
		if (changedBgrColorEnabled.isChecked())
		{
			widget.setBgrColorEnabled(temp_bgrColorEnabled);
			wasChange = true;
		}
		
		if (changedBackgroundColor.isChecked())
		{
			widget.setBackgroundColor(temp_backgroundColor);
			wasChange = true;
		}
		
		if (changedBackgroundOpacity.isChecked())
		{		
			widget.setTransparency(temp_backgroundOpacity);
			wasChange = true;
		}
		
		//background image
		if (changedBgrBitmapEnabled.isChecked())
		{
			widget.setBackgroundBitmapEnabled(temp_bgrBitmapEnabled);
			wasChange = true;
		}
		
		if (changedBgrBitmapName.isChecked())
		{
			widget.setBackgroundBitmap(temp_bgrBitmapName);
			wasChange = true;
		}
		
		if (changedBgrBitmapOpacity.isChecked())
		{
			widget.setBackgroundBitmapTransparency(temp_bgrBitmapOpacity);
			wasChange = true;
		}
		
		if (changedTextEnabled.isChecked())
		{
			widget.setTextEnabled(temp_textEnabled);
			wasChange = true;
		}
		
		if (changedTextColor.isChecked())
		{
			widget.getTextData().setTextColor(temp_textColor);
			wasChange = true;
		}
		
		if (wasChange)
		{
			widget.update();
		}
	}
}