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
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import magiclib.controls.Dialog;
import magiclib.core.EmuManager;
import magiclib.graphics.EmuVideo;
import magiclib.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

enum SaveStateAction
{
	save,
	load
}

public class uiSaveState extends Dialog
{
	public List<SaveStateSlot> list = new ArrayList<SaveStateSlot>();
	
	private SaveStateAction action;
	
	class SaveStateAdapter extends ArrayAdapter<SaveStateSlot> 
	{
		private List<SaveStateSlot> items;
		
	    public SaveStateAdapter(Context context, int textViewResourceId, List<SaveStateSlot> items) 
	    {
	        super(context, textViewResourceId, items);
	        
	        this.items = items;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) 
	    {
	    	View v = convertView;
	    	
	        if (v == null) 
	        {
	            v = getLayoutInflater().inflate(R.layout.savestate_item, null);
	        }	    	
	        
	        SaveStateSlot item = items.get(position);

	        if (item != null) 
	        {	 	        
	            ImageView avatar = (ImageView) v.findViewById(R.id.savestate_item__imageview);
	            TextView description = (TextView) v.findViewById(R.id.savestate_item_description);
	            TextView slot = (TextView) v.findViewById(R.id.savestate_item_slotnumber);
	            
	            slot.setText("Time portal : " + item.getSlotID());
	            
	            if (item.isEmpty)
	            {
	            	description.setText("empty");
	            	avatar.setImageResource(R.drawable.img_empty);
	            }
	            else
	            {
	            	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	            	
	            	description.setText(sdf.format(item.saveFile.lastModified()));
	            	avatar.setImageBitmap(BitmapFactory.decodeFile(item.screenshot.getAbsolutePath()));
	            }
	        }
	        
            return v;
	    }
	}
	
	public uiSaveState(Context context, SaveStateAction action)
	{
		super(context);
		
		this.action = action;
		setContentView(R.layout.savestate);
		
		
		
		loadAdapter();
		
		if (action == SaveStateAction.save)
		{
			setCaption("Set time portal");
		}
		else
		{
			setCaption("Travel");			
		}
	}
	
	private void loadAdapter()
	{
		list.add( new SaveStateSlot(1));
		list.add( new SaveStateSlot(2));
		list.add( new SaveStateSlot(3));
		list.add( new SaveStateSlot(4));
		list.add( new SaveStateSlot(5));
		list.add( new SaveStateSlot(6));
		
		final ListView listView = (ListView)findViewById(R.id.savestate_listview);
		
		listView.setAdapter(new SaveStateAdapter(getContext(), android.R.layout.simple_list_item_1, list));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
			{
				SaveStateSlot item = (SaveStateSlot)listView.getItemAtPosition(position);
				
				if (item != null)
				{
					if (Log.DEBUG) Log.log("item is not null");
					
					String dirPath = AppGlobal.currentGameRootPath + "Save" + item.getSlotID() + "/";
					
					File dir = new File(dirPath);
					
					if (action == SaveStateAction.save)
					{
						if (Log.DEBUG) Log.log("save");
						
						if (!dir.exists())
						{
							dir.mkdirs();
						}

						if (!EmuManager.isPaused())
						{
							EmuManager.pause();
						}	        				  

						try
						{
							Thread.sleep(1000);
						} 
						catch (InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}        				  

						MagicLauncher.nativeSaveState(dirPath);

						try 
						{
							FileOutputStream out = new FileOutputStream(dirPath + "screenshot.png");
							
							Bitmap bmp = EmuVideo.surface.geBitmap();

							bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
							out.flush();
							out.close();
							
							bmp.recycle();
							bmp = null;
						} 
						catch (Exception e) 
						{
							e.printStackTrace();
						}        				  

						EmuManager.unPause();

						dismiss();
					}
					else
					{     
						if (item.isEmpty)
							return;
						
						if (!EmuManager.isPaused())
						{
							EmuManager.pause();
						}	

						try
						{
							Thread.sleep(1000);
						} 
						catch (InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						MagicLauncher.nativeLoadState(dirPath);

						try
						{
							Thread.sleep(100);
						} 
						catch (InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						EmuManager.unPause();

						dismiss();
					}
				}
			}			
		});
	}
}

class SaveStateSlot
{
	public int slotID;
	public boolean isEmpty = true;
	public String description;
	public File screenshot;
	public File saveFile;
	
	public SaveStateSlot() 
	{
		//empty constructor 
	};
	
	public SaveStateSlot(int slotID) 
	{
		this.slotID = slotID;
		
		loadData();
	};	
	
	private void loadData()
	{
		String dirPath = AppGlobal.currentGameRootPath + "Save" + slotID + "/";
		
		File dir = new File(dirPath);
		
		if (!dir.exists())
		{			
			return;
		}
		
		saveFile = new File(dirPath + "save.sav");		
		screenshot = new File(dirPath + "screenshot.png");
		
		if ((saveFile.exists()) && (screenshot.exists()))
		{
			this.isEmpty = false;
		}
	}
	
	public int getSlotID()
	{
		return slotID;
	}
}