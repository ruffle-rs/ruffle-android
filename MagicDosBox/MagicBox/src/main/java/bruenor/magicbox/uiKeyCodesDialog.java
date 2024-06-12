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

import java.util.ArrayList;
import java.util.List;

import magiclib.controls.Dialog;
import magiclib.locales.Localization;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

abstract interface KeyCodeListener
{
	public abstract void onPick(KeyCodeItem selected);
}

class uiKeyCodesDialog extends Dialog
{	
	private int viewSize;
	private static List<KeyCodeItem> keys = null;
	private KeyCodesAdapter adapter;
	private KeyCodeListener keyCodeListener = null;
	
	private class KeyCodesAdapter extends ArrayAdapter<KeyCodeItem>
	{
		private List<KeyCodeItem> types;
		private final Context context;
		
		public KeyCodesAdapter(Context context, List<KeyCodeItem> types)
		{
			super(context, android.R.layout.simple_list_item_1, types);
			
			this.types = types;
			this.context = context;
		}
		
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) 
	    {
	    	View v = convertView;
	    	
	        if (v == null) 
	        {
	        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = inflater.inflate(R.layout.keycodes_item, null);
	            v.setLayoutParams(new GridView.LayoutParams(viewSize, viewSize));
	            v.setPadding(8, 8, 8, 8);
	        }	    	
	        
	        KeyCodeItem item = types.get(position);
	        
	        if (item != null)
	        {
	        	TextView description = (TextView) v.findViewById(R.id.keycodes_item_description);

	            if (description != null) 
	            {
	            	//description.setText(item.getText());
	            	//description.setTextColor(item.getTextColor());
					description.setText(item.html);
	            }
	            
	            v.setBackgroundColor(item.getBackgroundColor());
	        }
	        
	    	return v;	        
	    }		
	}
	
	public uiKeyCodesDialog(Context context)
	{
		this(context, true);
	}
	
	public uiKeyCodesDialog(Context context, boolean autofill)
	{
		super(context);
			
		setContentView(R.layout.keycodes_dialog);
		setCaption("common_keys");
				
		keys = new ArrayList<KeyCodeItem>();
		
		if (autofill)
		{
			getKeys();			
		}
		else
			fillDriveLetters();
		
		GridView gridview = (GridView)findViewById(R.id.keycodes_gridView);
		
		if (gridview != null)
		{
			adapter = new KeyCodesAdapter(this.getContext(), keys);
			
			gridview.setAdapter(adapter);
			gridview.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) 
				{
					if (keyCodeListener != null)
					{
						keyCodeListener.onPick(keys.get(position));
					}
					
					dismiss();
				}
			});	
			
			gridview.setColumnWidth(viewSize);
			gridview.setStretchMode(GridView.NO_STRETCH) ;
		}
	}
	
	@Override
	protected void onStop()
	{
		if (keys != null)
		{
			keys.clear();
		}
		
		if (adapter != null)
		{
			adapter.clear();
		}
				
		super.onStop();
	}
	
	public KeyCodeItem findKey(int keyCode)
	{
		getKeys();
		
		for(KeyCodeItem item : keys)
		{
			if (item.getKeyCode() == keyCode)
				return item;
		}
		
		return null;
	}
	
	public void fillDriveLetters()
	{
		if (keys.size() == 0)
		{
			//viewSize = ImageSize.small.getInDPI();
			Resources res = AppGlobal.context.getResources();
			viewSize = (int)res.getDimension(R.dimen.keybrowser_drives_size);

			int textColor = Color.YELLOW;
			int backColor = Color.parseColor("#5246AE");
			
			//a-z
			keys.add(new KeyCodeItem(29, "A:", textColor, backColor));
			keys.add(new KeyCodeItem(30, "B:", textColor, backColor));
			//keys.add(new KeyCodeItem(31, "C:", textColor, backColor));
			keys.add(new KeyCodeItem(32, "D:", textColor, backColor));
			keys.add(new KeyCodeItem(33, "E:", textColor, backColor));
			keys.add(new KeyCodeItem(34, "F:", textColor, backColor));
			keys.add(new KeyCodeItem(35, "G:", textColor, backColor));
			keys.add(new KeyCodeItem(36, "H:", textColor, backColor));
			keys.add(new KeyCodeItem(37, "I:", textColor, backColor));
			keys.add(new KeyCodeItem(38, "J:", textColor, backColor));
			keys.add(new KeyCodeItem(39, "K:", textColor, backColor));
			keys.add(new KeyCodeItem(40, "L:", textColor, backColor));		
			keys.add(new KeyCodeItem(41, "M:", textColor, backColor));		
			keys.add(new KeyCodeItem(42, "N:", textColor, backColor));
			keys.add(new KeyCodeItem(43, "O:", textColor, backColor));
			keys.add(new KeyCodeItem(44, "P:", textColor, backColor));
			keys.add(new KeyCodeItem(45, "Q:", textColor, backColor));
			keys.add(new KeyCodeItem(46, "R:", textColor, backColor));
			keys.add(new KeyCodeItem(47, "S:", textColor, backColor));
			keys.add(new KeyCodeItem(48, "T:", textColor, backColor));
			keys.add(new KeyCodeItem(49, "U:", textColor, backColor));
			keys.add(new KeyCodeItem(50, "V:", textColor, backColor));
			keys.add(new KeyCodeItem(51, "W:", textColor, backColor));
			keys.add(new KeyCodeItem(52, "X:", textColor, backColor));
			keys.add(new KeyCodeItem(53, "Y:", textColor, backColor));
			//keys.add(new KeyCodeItem(54, "Z:", textColor, backColor));
		}		
	}
	
	private List<KeyCodeItem> getKeys()
	{
		if (keys.size() == 0)
		{			
			//viewSize = ImageSize.small_medium.getInDPI();
			Resources res = AppGlobal.context.getResources();
			viewSize = (int)res.getDimension(R.dimen.keybrowser_keycodes_size);

			int textColor = Color.YELLOW;

			int backColor = Color.parseColor("#516058");

			//special
			keys.add(new KeyCodeItem(111, Localization.getString("keyboard_esc"), textColor, backColor));
			keys.add(new KeyCodeItem(112, Localization.getString("keyboard_del"), textColor, backColor));
			keys.add(new KeyCodeItem(113, "lCtrl", textColor, backColor));
			keys.add(new KeyCodeItem(114, "rCtrl", textColor, backColor));
			keys.add(new KeyCodeItem(57, "lAlt", textColor, backColor));
			keys.add(new KeyCodeItem(58, "rAlt", textColor, backColor));
			keys.add(new KeyCodeItem(59, "lShift", textColor, backColor));
			keys.add(new KeyCodeItem(60, "rShift", textColor, backColor));
			keys.add(new KeyCodeItem(62, Localization.getString("keyboard_space"), textColor, backColor));
			keys.add(new KeyCodeItem(66, Localization.getString("keyboard_enter"), textColor, backColor));
			keys.add(new KeyCodeItem(67, Localization.getString("keyboard_back"), textColor, backColor));
			keys.add(new KeyCodeItem(116, Localization.getString("keyboard_scrolllock"), textColor, backColor));
			keys.add(new KeyCodeItem(122, Localization.getString("keyboard_home"), textColor, backColor));
			keys.add(new KeyCodeItem(123, Localization.getString("keyboard_end"), textColor, backColor));
			keys.add(new KeyCodeItem(124, Localization.getString("keyboard_ins"), textColor, backColor));
			keys.add(new KeyCodeItem(92, Localization.getString("keyboard_page_up"), textColor, backColor));
			keys.add(new KeyCodeItem(93, Localization.getString("keyboard_page_down"), textColor, backColor));
			keys.add(new KeyCodeItem(61, Localization.getString("keyboard_tab"), textColor, backColor));
			keys.add(new KeyCodeItem(115, Localization.getString("keyboard_caps_lock"), textColor, backColor));
			keys.add(new KeyCodeItem(121, Localization.getString("keyboard_pause"), textColor, backColor));

			backColor = Color.MAGENTA;

			//up, down, left, right
			keys.add(new KeyCodeItem(19, Localization.getString("arrow_up"), textColor, backColor));
			keys.add(new KeyCodeItem(20, Localization.getString("arrow_down"), textColor, backColor));
			keys.add(new KeyCodeItem(21, Localization.getString("arrow_left"), textColor, backColor));
			keys.add(new KeyCodeItem(22, Localization.getString("arrow_right"), textColor, backColor));

			backColor = Color.parseColor("#C54028");

			//a-z
			keys.add(new KeyCodeItem(29, "a", "A", textColor, backColor));
			keys.add(new KeyCodeItem(30, "b", "B", textColor, backColor));
			keys.add(new KeyCodeItem(31, "c", "C", textColor, backColor));
			keys.add(new KeyCodeItem(32, "d", "D", textColor, backColor));
			keys.add(new KeyCodeItem(33, "e", "E", textColor, backColor));
			keys.add(new KeyCodeItem(34, "f", "F", textColor, backColor));
			keys.add(new KeyCodeItem(35, "g", "G", textColor, backColor));
			keys.add(new KeyCodeItem(36, "h", "H", textColor, backColor));
			keys.add(new KeyCodeItem(37, "i", "I", textColor, backColor));
			keys.add(new KeyCodeItem(38, "j", "J", textColor, backColor));
			keys.add(new KeyCodeItem(39, "k", "K", textColor, backColor));
			keys.add(new KeyCodeItem(40, "l", "L", textColor, backColor));
			keys.add(new KeyCodeItem(41, "m", "M", textColor, backColor));
			keys.add(new KeyCodeItem(42, "n", "N", textColor, backColor));
			keys.add(new KeyCodeItem(43, "o", "O", textColor, backColor));
			keys.add(new KeyCodeItem(44, "p", "P", textColor, backColor));
			keys.add(new KeyCodeItem(45, "q", "Q", textColor, backColor));
			keys.add(new KeyCodeItem(46, "r", "R", textColor, backColor));
			keys.add(new KeyCodeItem(47, "s", "S", textColor, backColor));
			keys.add(new KeyCodeItem(48, "t", "T", textColor, backColor));
			keys.add(new KeyCodeItem(49, "u", "U", textColor, backColor));
			keys.add(new KeyCodeItem(50, "v", "V", textColor, backColor));
			keys.add(new KeyCodeItem(51, "w", "W", textColor, backColor));
			keys.add(new KeyCodeItem(52, "x", "X", textColor, backColor));
			keys.add(new KeyCodeItem(53, "y", "Y", textColor, backColor));
			keys.add(new KeyCodeItem(54, "z", "Z", textColor, backColor));

			backColor = Color.parseColor("#586B13");
			
			//0-9
			keys.add(new KeyCodeItem(8, "1", "!", textColor, backColor));
			keys.add(new KeyCodeItem(9, "2", "@", textColor, backColor));
			keys.add(new KeyCodeItem(10, "3", "#", textColor, backColor));
			keys.add(new KeyCodeItem(11, "4", "$", textColor, backColor));
			keys.add(new KeyCodeItem(12, "5", "%", textColor, backColor));
			keys.add(new KeyCodeItem(13, "6", "^", textColor, backColor));
			keys.add(new KeyCodeItem(14, "7", "&", textColor, backColor));
			keys.add(new KeyCodeItem(15, "8", "*", textColor, backColor));
			keys.add(new KeyCodeItem(16, "9", "(", textColor, backColor));
			keys.add(new KeyCodeItem(7, "0", ")", textColor, backColor));

			backColor = Color.parseColor("#076DFF");
			
			//chars
			keys.add(new KeyCodeItem(55, ",", "<", textColor, backColor));
			keys.add(new KeyCodeItem(56, ".", ">", textColor, backColor));
			keys.add(new KeyCodeItem(68, "`", "~", textColor, backColor));
			keys.add(new KeyCodeItem(69, "-", "_", textColor, backColor));
			keys.add(new KeyCodeItem(70, "=", "+", textColor, backColor));
			keys.add(new KeyCodeItem(71, "[", "{", textColor, backColor));
			keys.add(new KeyCodeItem(72, "]", "}", textColor, backColor));
			keys.add(new KeyCodeItem(74, ";", ":", textColor, backColor));
			keys.add(new KeyCodeItem(76, "/", "?", textColor, backColor));
			keys.add(new KeyCodeItem(73, "\\", "|", textColor, backColor));
			keys.add(new KeyCodeItem(75, "'", "\"", textColor, backColor));

			backColor = Color.parseColor("#C50B38");			
			
			//F1-F12
			keys.add(new KeyCodeItem(131, "F1", textColor, backColor));
			keys.add(new KeyCodeItem(132, "F2", textColor, backColor));
			keys.add(new KeyCodeItem(133, "F3", textColor, backColor));
			keys.add(new KeyCodeItem(134, "F4", textColor, backColor));
			keys.add(new KeyCodeItem(135, "F5", textColor, backColor));
			keys.add(new KeyCodeItem(136, "F6", textColor, backColor));
			keys.add(new KeyCodeItem(137, "F7", textColor, backColor));
			keys.add(new KeyCodeItem(138, "F8", textColor, backColor));
			keys.add(new KeyCodeItem(139, "F9", textColor, backColor));
			keys.add(new KeyCodeItem(140, "F10", textColor, backColor));
			keys.add(new KeyCodeItem(141, "F11", textColor, backColor));
			keys.add(new KeyCodeItem(142, "F12", textColor, backColor));

			backColor = Color.parseColor("#57116F");
			
			//numeric keyboard
			keys.add(new KeyCodeItem(143, Localization.getString("keyboard_numlock"), textColor, backColor));
			keys.add(new KeyCodeItem(144, Localization.getString("keyboard_numpad_0"), textColor, backColor));
			keys.add(new KeyCodeItem(145, Localization.getString("keyboard_numpad_1"), textColor, backColor));
			keys.add(new KeyCodeItem(146, Localization.getString("keyboard_numpad_2"), textColor, backColor));
			keys.add(new KeyCodeItem(147, Localization.getString("keyboard_numpad_3"), textColor, backColor));
			keys.add(new KeyCodeItem(148, Localization.getString("keyboard_numpad_4"), textColor, backColor));
			keys.add(new KeyCodeItem(149, Localization.getString("keyboard_numpad_5"), textColor, backColor));
			keys.add(new KeyCodeItem(150, Localization.getString("keyboard_numpad_6"), textColor, backColor));
			keys.add(new KeyCodeItem(151, Localization.getString("keyboard_numpad_7"), textColor, backColor));
			keys.add(new KeyCodeItem(152, Localization.getString("keyboard_numpad_8"), textColor, backColor));
			keys.add(new KeyCodeItem(153, Localization.getString("keyboard_numpad_9"), textColor, backColor));
			keys.add(new KeyCodeItem(160, Localization.getString("keyboard_numpad_enter"), textColor, backColor));
			keys.add(new KeyCodeItem(157, Localization.getString("keyboard_numpad_plus"), textColor, backColor));
			keys.add(new KeyCodeItem(156, Localization.getString("keyboard_numpad_minus"), textColor, backColor));		
			keys.add(new KeyCodeItem(155, Localization.getString("keyboard_numpad_star"), textColor, backColor));
			keys.add(new KeyCodeItem(154, Localization.getString("keyboard_numpad_slash"), textColor, backColor));
			keys.add(new KeyCodeItem(158, Localization.getString("keyboard_numpad_dot"), textColor, backColor));
		}
		return keys;
	}

	public void setOnKeyCodeListener(KeyCodeListener event)
	{
		this.keyCodeListener = event;
	}	
}

class KeyCodeItem
{
	private int keyCode;
	//private String text;
	private int textColor = Color.WHITE;
	private int backgroundColor = Color.BLACK;

	public Spanned html;
/*
	public KeyCodeItem(int keyCode, String text)
	{
		this.keyCode = keyCode;
		this.text = text;
	}*/

	public KeyCodeItem(int keyCode, String text, int textColor, int backgroundColor)
	{
		this.keyCode = keyCode;
		this.textColor = textColor;
		this.backgroundColor = backgroundColor;
		//this.text = text;
		this.html = Html.fromHtml("<font color='#FFFF00'>" + text +"</font>");
	}

	public KeyCodeItem(int keyCode, String text, String shiftedText, int textColor, int backgroundColor)
	{
		this.keyCode = keyCode;
		this.textColor = textColor;
		this.backgroundColor = backgroundColor;
		//this.text = text + " " + shiftedText;

		if (shiftedText.equals("<")) {
			shiftedText = "&#60;";
		} else if (shiftedText.equals(">")) {
			shiftedText = "&#62;";
		}

		this.html = Html.fromHtml("<font color='#FFFF00'>" + text +"</font>" +
				                  "<font color='#FFA500'> " + shiftedText +"</font>");
	}	
	
	public int getKeyCode()
	{
		return keyCode;
	}
	
	/*public String getText()
	{
		return text;
	}*/

	public int getTextColor()
	{
		return textColor;
	}

	public void setTextColor(int textColor)
	{
		this.textColor = textColor;
	}

	public int getBackgroundColor()
	{
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor)
	{
		this.backgroundColor = backgroundColor;
	}
}