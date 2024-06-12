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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import magiclib.IO.FileBrowser;
import magiclib.logging.MessageInfo;

abstract interface AddStorageEventListener
{
	public abstract void onPick(String title, String path);
}

class uiAddStorage extends magiclib.controls.Dialog
{
	private String oldTitle;
	private String oldPath;
	
	private EditText titleEdit;
	private EditText pathEdit;
	private ImageButton confirm;
	private AddStorageEventListener event; 
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.add_storage_title_caption, "addst_title");
		localize(R.id.add_storage_path_caption, "addst_path");
		localize(R.id.add_storage_path_example, "addst_path_example");
        localize(R.id.add_storage_path_choose, "common_choose");
	}
	
	public uiAddStorage(Context context, String title, String path)
	{
		super(context);
		
		setContentView(R.layout.add_storage);
		setCaption("addst_caption");
		
		oldTitle = title;
		oldPath = path;
		
		titleEdit = (EditText)findViewById(R.id.add_storage_title);
		titleEdit.setText(title);
		
		pathEdit = (EditText)findViewById(R.id.add_storage_path);
		pathEdit.setText(path);

        Button choosePath = (Button)findViewById(R.id.add_storage_path_choose);
        choosePath.setOnClickListener(getChooseEvent());

		confirm = (ImageButton)findViewById(R.id.add_storage_confirm);
		confirm.setOnClickListener(getConfirmEvent());
	}

    private View.OnClickListener getChooseEvent()
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                FileBrowser fb;

                fb = new FileBrowser(getContext(), "/", null, true);

                fb.setCaption("fb_caption_choose_folder");
                fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
                {
                    @Override
                    public void onPick(String selected)
                    {
                        pathEdit.setText(selected);
                    }
                });

                fb.show();
            }
        };
    }

	private View.OnClickListener getConfirmEvent()
	{
		return new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				String t = titleEdit.getText().toString().trim();
				String p = pathEdit.getText().toString().trim();
				
				if (t.equals("") || p.equals(""))
				{
					MessageInfo.info("addst_msg_emptytitlepath");
					return;
				}
				
				if (!p.startsWith("/"))
				{
					MessageInfo.info("addst_msg_badstart");
				}
				
				File f = new File(p);
				
				if (!f.exists())
				{
					MessageInfo.info("addst_msg_pathnotexists");
					return;
				}
				
				if (Build.VERSION.SDK_INT >= 19)
				{
					//TODO - I don't know what to do
				}
				else
				{
					if (!f.canRead() || !f.canWrite())
					{
						MessageInfo.info("addst_msg_permissionerror");
						return;
					}					
				}
				
				if (!(t.equals(oldTitle) && p.equals(oldPath)))
				{
					if (event != null)
					{
						event.onPick(t, p);
					}
				}

				dismiss();
			}
		};
	}

	public void setOnAddStorageEventListener(AddStorageEventListener event)
	{
		this.event = event;
	}
}

