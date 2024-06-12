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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import magiclib.collection.CollectionItem;
import magiclib.controls.Dialog;
import magiclib.logging.Log;

abstract interface ProfileImportEventListener
{
	public abstract void onBasicsLoad(boolean success);
	public abstract void onFinish(CollectionItem game);
}

class uiGameProfileImport extends Dialog
{
	class BaseInfoExport extends AsyncTask<String, Integer, String> 
	{
		private boolean exportBasics()
		{
			int counter = 0;
			
			try 
			{
				File f = new File(AppGlobal.appTempPath);
				if (!f.exists())
					f.mkdirs();
				
				FileInputStream fin = new FileInputStream(importFile);
				ZipInputStream zin = new ZipInputStream(fin);
				ZipEntry entry = null;
				
			    while ((entry = zin.getNextEntry()) != null)
			    {
					if (!entry.isDirectory())
					{
						if (entry.getName().equals("/exportinfo.xml") || entry.getName().contains("/avatar.")) 
						{								
							f = new File(AppGlobal.appTempPath + entry.getName());
							
							if (f.exists())
								f.delete();

							if (entry.getName().contains("/avatar."))
							{
								avatarFile = f;
							}
							else
							{
								exportInfoFile = f;
							}						
							
							FileOutputStream fout = new FileOutputStream(f);
							
							byte[] buffer = new byte[8192];
							int len;
							
							while ((len = zin.read(buffer)) != -1)
							{
							  fout.write(buffer, 0, len);
							}
							
							fout.close();
							
							counter++;							
						}
						
						maxSteps++;
					}
					
					zin.closeEntry();
			    }
			    
			    zin.close();
			    
			    return (counter == 2);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			return false;
		}
		
		private boolean loadExportObjects()
		{
			Serializer serializer = new Persister();
			
			try 
			{
				serializer.read(information, exportInfoFile);
				return true;
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
				if (Log.DEBUG) Log.log("load export information failed");
			}
			
			return false;
		}
		
		@Override
		protected String doInBackground(String... params) 
		{
			if (exportBasics() && (loadExportObjects()))
			{				
				return "OK";
			}
			
			return "ERROR";
		}	
		
        @Override
        protected void onPostExecute(String result) 
        {
        	if (result.equals("OK"))
        	{
        		Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());		
        		avatar.setImageBitmap(bitmap);
        		
        		title.setText(information.title);
        		author.setText(information.author);
        		devices.setText(information.devices);
        		note.setText(information.note);
        	}
        	
        	event.onBasicsLoad(result.equals("OK"));
        }
	}
	
	class ImportGameProfile extends AsyncTask<String, Integer, CollectionItem>
	{
		public boolean excludeScreenshots = false;
				
		private int currentStep = 0;
		private boolean progressStarted = false;	

        @Override
        protected void onPreExecute() 
        {
            super.onPreExecute();
        }
        
        protected void onProgressUpdate(Integer... progress) 
        {
        	if (!progressStarted) 
        	{
        		progressStarted = true;        		
        		progressBar.setMax(progress[1]);
        	}
        	
        	progressBar.setProgress(progress[0]);
        }
        
        @Override
        protected void onPostExecute(CollectionItem result)
        {
        	event.onFinish(result);
        }        
        
        private boolean unpackZip(String path, String zipFile)
        {       
             InputStream is;
             ZipInputStream zis;
             try 
             {
                 is = new FileInputStream(zipFile);
                 zis = new ZipInputStream(new BufferedInputStream(is));          
                 ZipEntry ze;
                 byte[] buffer = new byte[1024];
                 int count;

                 while ((ze = zis.getNextEntry()) != null) 
                 {
                     File fmd = new File(path + ze.getName());

                     if (ze.isDirectory()) {
                        fmd.mkdirs();
                        publishProgress(currentStep++, maxSteps);
                        continue;
                     }
                     
                     String purePath = fmd.getParent().trim();
                     
                     if (!purePath.equals("") || !purePath.equals("/") || purePath.equals("\\"))
                     {
                    	 if (excludeScreenshots && purePath.endsWith("Screenshots"))
                    	 {
                    		 publishProgress(currentStep++, maxSteps);
                    		 continue;
                    	 }
                    	 
                    	 File d = new File(purePath);
                    	 if (!d.exists())
                    		 d.mkdirs();
                     }
                     
                     FileOutputStream fout = new FileOutputStream(fmd);

                     while ((count = zis.read(buffer)) != -1) 
                     {
                         fout.write(buffer, 0, count);             
                     }

                     fout.close();               
                     zis.closeEntry();
                     
                     publishProgress(currentStep++, maxSteps);
                 }

                 zis.close();
             } 
             catch(Exception e)
             {
                 e.printStackTrace();
                 return false;
             }

            return true;
        }
        
		@Override
		protected CollectionItem doInBackground(String... params)
		{
			try 
			{
				File targetFolder = new File(AppGlobal.gamesDataPath + UUID.randomUUID().toString());
				
				while (targetFolder.exists())
				{
					targetFolder = new File(AppGlobal.gamesDataPath + UUID.randomUUID().toString());
				}

				CollectionItem game = new CollectionItem(avatarFile.getName(), information.title, targetFolder.getName());
				
				///File dest = new File(AppGlobal.gamesDataPath + i.getID() + "/");
				
				targetFolder.mkdirs();
				
				if (unpackZip(targetFolder.getAbsolutePath() + "/", importFile.getAbsolutePath()))
				{
					return game;
				}
				
				return null;
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			return null;
		}		
	}
	
	private File importFile;
	private File avatarFile;
	private File exportInfoFile;
	private int maxSteps = 0;
	
	private ExportInformation information = new ExportInformation();
	private ImageView avatar;
	private TextView title;
	private ImageButton confirm;
	private LinearLayout initPanel;
	private LinearLayout progressPanel;	
	private ProgressBar progressBar;
	private EditText author;
	private EditText devices;
	private EditText note;
	private CheckBox importScreenhots;
	private ProfileImportEventListener event;
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.game_profile_import_author_title,             "common_author");
		localize(R.id.game_profile_import_compatible_devices_title, "gameprof_import_compatible_devices");
		localize(R.id.game_profile_import_note_title,               "common_note");
		localize(R.id.game_profile_import_screenshots,              "gameprof_import_screenshots");
		localize(R.id.game_profile_import_importing,                "gameprof_import_importing");
	}
	
	public uiGameProfileImport(Context context, File importFile) 
	{
		super(context);
		
		setContentView(R.layout.game_profile_import);
		setCaption("gameprof_import_title");
		
		this.importFile = importFile;
		
		avatar = (ImageView)findViewById(R.id.game_profile_import_avatar);
		title = (TextView)findViewById(R.id.game_profile_import_gametitle);
		confirm = (ImageButton)findViewById(R.id.game_profile_import_confirm);
		initPanel = (LinearLayout)findViewById(R.id.game_profile_import_initpanel);
		progressPanel = (LinearLayout)findViewById(R.id.game_profile_import_progresspanel);
		progressBar = (ProgressBar)findViewById(R.id.game_profile_import_progressbar);
		author = (EditText)findViewById(R.id.game_profile_import_author);
		devices = (EditText)findViewById(R.id.game_profile_import_compatible_devices);
		note = (EditText)findViewById(R.id.game_profile_import_note);
		importScreenhots = (CheckBox)findViewById(R.id.game_profile_import_screenshots);
		
		initPanel.setVisibility(View.VISIBLE);
		progressPanel.setVisibility(View.GONE);		
		
		confirm.setOnClickListener(confirmEvent());	
	}

	public void init()
	{
		BaseInfoExport baseInfo = new BaseInfoExport();
		baseInfo.execute();
	}

	private View.OnClickListener confirmEvent()
	{
		return new View.OnClickListener() 
		{		
			@Override
			public void onClick(View v) 
			{
				setOnCloseDialogEventListener(new OnCloseDialogEventListener ()
				{
					@Override
					public boolean onClose() 
					{
						return false;
					}							
				});
				
				confirm.setEnabled(false);
				setCancelable(false);
				
				initPanel.setVisibility(View.GONE);
				progressPanel.setVisibility(View.VISIBLE);	
				
				ImportGameProfile importer = new ImportGameProfile();
				importer.excludeScreenshots = !importScreenhots.isChecked();
				importer.execute();
			}
		};
	}
	
	public void setOnProfileImportEventListener(ProfileImportEventListener event) 
	{
		this.event = event;
	}
}
