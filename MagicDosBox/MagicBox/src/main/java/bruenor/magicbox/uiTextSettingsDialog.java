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
import magiclib.core.Align;
import magiclib.fonts.ExternalFontItem;
import magiclib.fonts.ExternalFonts;
import magiclib.locales.Localization;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.ArrayList;
import java.util.List;

abstract interface TextSettingsChangeListener
{
	public abstract void onChange(String text, Align align, int opacity, boolean antialiasing, String fontCode);
}

class uiTextSettingsDialog extends Dialog
{
	private EditText editText;
	private Align align;
	private CheckBox cbxxAntialiasing;
	private int opacity;
	private String fontCode;
	private int fontIndex = -1;
	private int maxFontIndex;
	private TextView fntValue;
	private TextView alignmentValue;
	private TextSettingsChangeListener textSettingsChangeListener = null;
	List<String> fnts = new ArrayList<String>();
	
	public void onSetLocalizedLayout() 
	{
		localize(R.id.button_menu_textdialog_text_caption,  "textset_text_title");
		localize(R.id.button_menu_textdialog_alignmenttext, "common_alignment");
		
		//localize(R.id.button_menu_textdialog_alignmenttext_left,   "textset_alignment_left_title");
		//localize(R.id.button_menu_textdialog_alignmenttext_middle, "textset_alignment_middle_title");
		//localize(R.id.button_menu_textdialog_alignmenttext_right,  "textset_alignment_right_title");
		
		localize(R.id.button_menu_textdialog_opacitytext,  "textset_opacity_title");

		localize(R.id.button_menu_textdialog_font_title, "common_font");
		localize(R.id.button_menu_textdialog_antialiasing, "textset_antialiasing_on");
	};
	
	public uiTextSettingsDialog(Context context, String text, 
			                    Align textAlign,
			                    int textOpacity,
								boolean antialiasing,
								String fontCode)
	{
		super(context);

		setContentView(R.layout.button_menu_text_dialog);
		setCaption("textset_caption");
		
		this.align = textAlign;
		this.opacity = textOpacity;
		this.fontCode = fontCode;

		editText = (EditText)findViewById(R.id.button_menu_textdialog_text);
		editText.setText(text);

		cbxxAntialiasing = (CheckBox)findViewById(R.id.button_menu_textdialog_antialiasing);
		cbxxAntialiasing.setChecked(antialiasing);

		final TextView opacityValue = (TextView)findViewById(R.id.button_menu_textdialog_opacity_value);
		opacityValue.setText(""+opacity);		
		
		final SeekBar seek = (SeekBar)findViewById(R.id.button_menu_textdialog_opacity);
		seek.setMax(255);
		seek.setProgress(opacity);		
		
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
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
				opacity = progress;
				opacityValue.setText(""+opacity);
			}
		});			
		
		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.button_menu_opacity_minus:
					{
						if (opacity == 0)
							return;
						
						opacity --;
						seek.setProgress(opacity);
						opacityValue.setText(""+opacity);
						break;
					}
					case R.id.button_menu_opacity_plus:
					{
						if (opacity == 255)
							return;
						
						opacity ++;
						seek.setProgress(opacity);
						opacityValue.setText(""+opacity);
						break;
					}
					case R.id.button_menu_textdialog_font_minus:
					{
						minusFont();
						break;
					}
					case R.id.button_menu_textdialog_font_plus:
					{
						plusFont();
						break;
					}
					case R.id.button_menu_textdialog_alignmenttext_minus:
					{
						minusAlignment();
						break;
					}
					case R.id.button_menu_textdialog_alignmenttext_plus:
					{
						plusAlignment();
						break;
					}
				}
			}			
		};
		
		ImageButton zoom = (ImageButton)findViewById(R.id.button_menu_opacity_minus);
		zoom.setOnClickListener(buttonClick);
		
		zoom = (ImageButton)findViewById(R.id.button_menu_opacity_plus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_textdialog_font_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_textdialog_font_plus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_textdialog_alignmenttext_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_textdialog_alignmenttext_plus);
		zoom.setOnClickListener(buttonClick);

		fnts.add(Localization.getString("common_default"));
		fntValue = (TextView)findViewById(R.id.button_menu_textdialog_font_value);
		alignmentValue = (TextView)findViewById(R.id.button_menu_textdialog_alignmenttext_value);

		if (ExternalFonts.fonts != null)
		{
			int i = 1;

			for (ExternalFontItem fnt : ExternalFonts.fonts)
			{
				fnts.add(fnt.title);

				if (fontCode != null)
				{
					if (fnt.fileName.equals(fontCode)) {
						fntValue.setText(fnt.title);
						fontIndex = i;
					}
					i++;
				}
			}
		}

		if (fontIndex == -1)
		{
			fntValue.setText(fnts.get(0));
			fontIndex = 0;
		}

		maxFontIndex = fnts.size() - 1;

		ImageButton confirm = (ImageButton)findViewById(R.id.button_menu_textdialog_confirm);
		confirm.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if (textSettingsChangeListener != null)
				{
					String fc = (fontIndex == 0) ? null : ExternalFonts.fonts.get(fontIndex - 1).fileName;

					textSettingsChangeListener.onChange(editText.getText().toString(), align, opacity, cbxxAntialiasing.isChecked(), fc);
				}
				
				dismiss();
			}
		});

		setAlignmentTitle();
	}

	public void setOnTextSettingsChangeListener(TextSettingsChangeListener event)
	{
		this.textSettingsChangeListener = event;
	}

	private void minusFont()
	{
		if (fontIndex == 0)
			return;

		fntValue.setText(fnts.get(--fontIndex));
	}

	private void plusFont()
	{
		if (fontIndex == maxFontIndex)
			return;

		fntValue.setText(fnts.get(++fontIndex));
	}

	private void minusAlignment()
	{
		if (align == Align.left)
			return;

		if (align == Align.right) {
			align = Align.middle;
		} else {
			align = Align.left;
		}

		setAlignmentTitle();
	}

	private void plusAlignment()
	{
		if (align == Align.right)
			return;

		if (align == Align.left) {
			align = Align.middle;
		} else {
			align = Align.right;
		}

		setAlignmentTitle();
	}

	private void setAlignmentTitle() {
		switch (align) {
			case left: {
				alignmentValue.setText(Localization.getString("textset_alignment_left_title"));
				break;
			}
			case middle: {
				alignmentValue.setText(Localization.getString("textset_alignment_middle_title"));
				break;
			}
			case right: {
				alignmentValue.setText(Localization.getString("textset_alignment_right_title"));
				break;
			}
		}
	}
}
