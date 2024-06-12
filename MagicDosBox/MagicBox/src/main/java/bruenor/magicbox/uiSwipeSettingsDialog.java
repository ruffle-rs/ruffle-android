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
import magiclib.gestures.SwipeItem;
import magiclib.gestures.Swipes;
import magiclib.layout.widgets.WidgetType;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class uiSwipeSettingsDialog extends Dialog
{
	SwipeItem swipeItem;
	
	
	public void onSetLocalizedLayout() 
	{
		localize(R.id.swipes_radio_afterswipe_title,           "swipesset_swipes_settings_allow");
		localize(R.id.swipesettings_radioallow_mousemoveonly,  "swipesset_swipes_settings_allow_move_cursor");
		localize(R.id.swipesettings_radioallow_mousemoveclick, "swipesset_swipes_settings_allow_moveclick_cursor");
	};
	
	public uiSwipeSettingsDialog(Context context, SwipeItem item)
	{
		super(context);
				
		setContentView(R.layout.swipe_settings_dialog);
		setCaption("swipesset_swipes_caption");		
		
		this.swipeItem = item;
		
		OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				switch(checkedId)
				{
					case R.id.swipesettings_radioallow_mousemoveonly:
					{
						swipeItem.setAfterSwipe(Swipes.AfteSwipeAction.move_mouse);
						break;
					}
					case R.id.swipesettings_radioallow_mousemoveclick:
					{
						swipeItem.setAfterSwipe(Swipes.AfteSwipeAction.move_mouse_and_click);
						break;
					}					
				}				
			}			
		};
		
		RadioGroup grp = (RadioGroup)getView().findViewById(R.id.swipes_radio_afterswipe);
		grp.setOnCheckedChangeListener(onRadioChange);
		
		switch (swipeItem.getAfterSwipe())
		{
			case move_mouse:
			{
				grp.check(R.id.swipesettings_radioallow_mousemoveonly);
				break;
			}
			case move_mouse_and_click:
			{
				grp.check(R.id.swipesettings_radioallow_mousemoveclick);
				break;
			}			
		}
		
		if (item.getWidget().getType() == WidgetType.key)
		{	
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			LinearLayout layout = (LinearLayout)getView().findViewById(R.id.swipesettings_custom_view);
			layout.addView((LinearLayout)inflater.inflate(R.layout.swipe_settings_keys, null));
			
			CheckBox cbx = (CheckBox)getView().findViewById(R.id.swipe_settings_key_keyup_onnexttouch);
			localize(R.id.swipe_settings_key_keyup_onnexttouch, "swipesset_swipes_settings_keys_kupnext");
			
			cbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
			{				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					swipeItem.setKeyUpOnNextTouch(isChecked);
				}
			});
			
			cbx.setChecked(swipeItem.isKeyUpOnNextTouch());
		}		
	}
}
