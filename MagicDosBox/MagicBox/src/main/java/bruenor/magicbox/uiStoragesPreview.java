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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import magiclib.IO.StorageInfo;
import magiclib.IO.Storages;
import magiclib.controls.Dialog;
import magiclib.locales.Localization;

class StoragesPreview extends Dialog
{
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.storage_preview_explanation_title, "storprew_explanation_title");
		localize(R.id.storage_preview_fullaccess_title, "storprew_fullaccess_title");
		localize(R.id.storage_preview_fullaccess, "storprew_fullaccess");
		localize(R.id.storage_preview_readonlyaccess_title, "storprew_readonlyaccess_title");
		localize(R.id.storage_preview_readonlyaccess, "storprew_readonlyaccess");
		localize(R.id.storage_preview_detected_storages, "storprew_detected_storages");
	};
	
	public StoragesPreview(Context context)
	{
		super(context);

		setContentView(R.layout.storages_preview);
		setCaption("storprew_caption");
		
		LinearLayout content = (LinearLayout)findViewById(R.id.storage_preview_content);
		
		List<StorageInfo> list = Storages.getStorages();
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		int i = 0;
		
		for (StorageInfo info : list)
		{
			LinearLayout itemView = (LinearLayout)inflater.inflate(R.layout.storage_preview_item, null);
			
			TextView textView = (TextView)itemView.findViewById(R.id.storage_preview_item_storage_title);
			
			if ((info.diskTitle == null) || (info.diskTitle.equals("")))
			{
				textView.setText(Localization.getString("common_disk") + " : " + i + " (" + info.diskSize +"GB)");
				i++;
			}
			else
			{
				textView.setText(info.diskTitle);
			}
			
			textView = (TextView)itemView.findViewById(R.id.storage_preview_item_storage_access);
			textView.setText(Localization.getString(info.readOnly ? "storprew_readonlyaccess_title" : "storprew_fullaccess_title"));
			textView.setTextColor(info.readOnly ? Color.parseColor("#ff0000") : Color.parseColor("#a4c639"));
			
			textView = (TextView)itemView.findViewById(R.id.storage_preview_item_storage_path);
			textView.setText(info.path);
			
			content.addView(itemView);
		}
		
		ImageButton confirm = (ImageButton)findViewById(R.id.storage_preview_confirm);
		confirm.setOnClickListener(new View.OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				dismiss();
			}
		});
	}	
}