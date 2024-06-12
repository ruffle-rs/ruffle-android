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
import android.widget.ImageButton;
import android.widget.TextView;

import magiclib.controls.Dialog;

abstract interface DelayEventListener
{
	public abstract void onPick(int value);
}

class uiComboDelayEdit extends Dialog
{
	private int value;
	private TextView edValue;
	private View.OnClickListener counterEvent = null;
	private DelayEventListener delayEventListener;
	
	private int minValue = 0;
	private int maxValue = 10000;
	
	public uiComboDelayEdit(Context context, int value)
	{
		super(context);
		
		this.value = value;
		
		setContentView(R.layout.combo_delay_edit);
		setCaption("widget_edit_combo_rest_caption");
		
		edValue = (TextView)getView().findViewById(R.id.combo_delay_edit_number);
		setDisplayValue();
		
		ImageButton button;
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_1_plus);
		button.setOnClickListener(getCounterEvent());
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_1_minus);
		button.setOnClickListener(getCounterEvent());
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_10_plus);
		button.setOnClickListener(getCounterEvent());
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_10_minus);
		button.setOnClickListener(getCounterEvent());		
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_100_plus);
		button.setOnClickListener(getCounterEvent());		
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_100_minus);
		button.setOnClickListener(getCounterEvent());
		
		button = (ImageButton)getView().findViewById(R.id.combo_delay_edit_confirm);
		button.setOnClickListener(getConfirmEvent());
		
		/*View v = findViewById(R.id.combo_delay_rootview);
		
		if (v != null)
		{			
			getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}*/
	}
	
	private void setDisplayValue()
	{
		edValue.setText(value + " [ms]");
	}
	
	private View.OnClickListener getConfirmEvent()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if (delayEventListener != null)
				{
					delayEventListener.onPick(value);
				}
				
				dismiss();
			}
		};
	}
	
	private View.OnClickListener getCounterEvent()
	{
		if (counterEvent == null)
		{
			counterEvent = new View.OnClickListener()
			{			
				@Override
				public void onClick(View v)
				{
					switch (v.getId())
					{
						case R.id.combo_delay_edit_1_plus:
						{
							value++;							
							break;
						}
						case R.id.combo_delay_edit_1_minus:
						{
							value--;
							break;
						}					
						case R.id.combo_delay_edit_10_plus:
						{
							value+=10;
							break;
						}
						case R.id.combo_delay_edit_10_minus:
						{
							value-=10;
							break;
						}					
						case R.id.combo_delay_edit_100_plus:
						{
							value+=100;
							break;
						}
						case R.id.combo_delay_edit_100_minus:
						{
							value-=100;
							break;
						}										
					}
					
					if (value > maxValue)
						value = maxValue;
					
					if (value < minValue)
						value = minValue;
					
					setDisplayValue();
				}
			};
		}
		
		return counterEvent;
	}

	public void setOnDelayEventListener(DelayEventListener event)
	{
		this.delayEventListener = event;
	}
}
