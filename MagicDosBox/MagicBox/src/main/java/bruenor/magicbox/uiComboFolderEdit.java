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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import magiclib.controls.Dialog;
import magiclib.layout.widgets.Widget;

abstract interface ComboFolderEditEventListener
{
	public abstract void onPick(Widget widget, int actionIndex);
}

class uiComboFolderEdit extends Dialog
{
	private Widget widget;
	private ComboFolderEditEventListener eventListener;
	
	public uiComboFolderEdit(Context context, Widget widget, String actionTextCode)
	{
		super(context);
		
		this.widget = widget;
		
		setContentView(R.layout.combo_folder_edit);
		
		TextView actionText = (TextView)getView().findViewById(R.id.combo_folder_edit_actiontext);
		
/*		if (bag)
		{
			actionText.setText(getLocaleString("widget_edit_combo_bag_action"));
		}
		else if (mouseToggle)
		{
			
			actionText.setText(getLocaleString("widget_edit_combo_mbt_action"));
		}
		else
		{
			actionText.setText(getLocaleString("widget_edit_combo_gp_action"));
		}*/

		actionText.setText(getLocaleString(actionTextCode));

		ImageButton butConfirm = (ImageButton)getView().findViewById(R.id.combo_folder_edit_confirm);
		butConfirm.setOnClickListener(getConfirmEvent());
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
					RadioGroup rg = (RadioGroup) findViewById(R.id.combo_folder_edit_radiogroup);
					 
					int actionIndex = 0;
					
					switch (rg.getCheckedRadioButtonId())
					{
						case R.id.combo_folder_edit_radiogroup_item1:
						{
							actionIndex = 0;
							break;
						}
						case R.id.combo_folder_edit_radiogroup_item2:
						{
							actionIndex = 1;
							break;
						}
						case R.id.combo_folder_edit_radiogroup_item3:
						{
							actionIndex = 2;
							break;
						}						
					}
					
					eventListener.onPick(widget, actionIndex);
				}
				
				dismiss();
			}
		};
	}
	
	public void setRadioItemLabel(int index, String text)
	{
		switch (index)
		{
			case 0:
			{
				((RadioButton)getView().findViewById(R.id.combo_folder_edit_radiogroup_item1)).setText(text);
				break;
			}
			case 1:
			{
				((RadioButton)getView().findViewById(R.id.combo_folder_edit_radiogroup_item2)).setText(text);
				break;
			}
			case 2:
			{
				((RadioButton)getView().findViewById(R.id.combo_folder_edit_radiogroup_item3)).setText(text);
				break;
			}			
		}
	}

	public void setSelectedRadioItem(int index)
	{
		RadioGroup rg = (RadioGroup) findViewById(R.id.combo_folder_edit_radiogroup);
		
		switch (index)
		{
			case 0:
			{
				rg.check(R.id.combo_folder_edit_radiogroup_item1);
				break;
			}
			case 1:
			{
				rg.check(R.id.combo_folder_edit_radiogroup_item2);
				break;
			}
			case 2:
			{
				rg.check(R.id.combo_folder_edit_radiogroup_item3);
				break;
			}			
		}
	}	
	
	public void setOnComboFolderEditEventListener(ComboFolderEditEventListener event)
	{
		this.eventListener = event;
	}	
}
