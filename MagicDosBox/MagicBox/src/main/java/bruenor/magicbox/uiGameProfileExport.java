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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import magiclib.IO.Files;
import magiclib.collection.CollectionItem;
import magiclib.controls.Dialog;
import magiclib.core.Screen;
import magiclib.locales.Localization;
import magiclib.logging.Log;
import magiclib.logging.MessageInfo;

class uiGameProfileExport extends Dialog
{
	class ExportGameProfile extends AsyncTask<String, Integer, String>
	{		 
		public boolean excludeScreenshots = false;

		private String zippedFolder;
		private String outputFile;
		private int maxSteps = 0;
		private int currentStep = 0;
		private boolean progressStarted = false;

		void countFiles(File src)
		{
			if(src.isDirectory())
			{
				File [] files = src.listFiles();
				
				if (files == null) 
					return;
							
				for (File file : files) 
				{	       			
					if (file.isDirectory() && ((excludeScreenshots && file.getName().equals("Screenshots")) || (file.getName().equals("Temp"))))
					{
						continue;
					}
	       			
					countFiles(file);
				}				
	       	}
	       	else
	       	{
	       		maxSteps++;
	       	}			
		}

		private void zipFolder(String srcFolder, String destZipFile) throws Exception 
	    {
	        FileOutputStream fileWriter = new FileOutputStream(destZipFile);
	        ZipOutputStream zip = new ZipOutputStream(fileWriter);
	        
	        addFolderToZip(true, "", srcFolder, zip);
	        addFileToZip(false, "", AppGlobal.appTempPath + "exportinfo.xml", zip);
	        /*
	        if (convertedLayout != null && convertedLayout.exists())
	        {
	        	addFileToZip(false, "", AppGlobal.appTempPath + "layouts.xml", zip);
	        }*/

	        zip.flush();
	        zip.close();
	    }
		
	    private void addFileToZip(boolean useFilter, String path, String srcFile, ZipOutputStream zip) throws Exception 
	    {
	        File folder = new File(srcFile);
	        
	        if (folder.isDirectory()) 
	        {
	        	if ((excludeScreenshots && folder.getName().equals("Screenshots")) || (folder.getName().equals("Temp")))
	        		return;
	        	
	            addFolderToZip(useFilter, path, srcFile, zip);
	        } 
	        else 
	        {
	        	if (useFilter && folder.getName().equals("exportinfo.xml"))
	        		return;

	            byte[] buf = new byte[1024];
	            int len;
	            
	            FileInputStream in = new FileInputStream(srcFile);
	            
	            if (path.startsWith(zippedFolder))
	            {
	            	zip.putNextEntry(new ZipEntry(path.replaceFirst(zippedFolder, "") + "/" + folder.getName()));
	            }
	            else
	            {
	            	zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
	            }	            	
	            
	            while ((len = in.read(buf)) > 0) 
	            {
	                zip.write(buf, 0, len);
	            }
	            
	            in.close();
	            
	            publishProgress(currentStep++, maxSteps);
	        }
	    }		
		
	    private void addFolderToZip(boolean useFilter, String path, String srcFolder, ZipOutputStream zip) throws Exception 
	    {
	        File folder = new File(srcFolder);
	        for (String fileName : folder.list()) 
	        {
	            if (path.equals("")) 
	            {
	                addFileToZip(useFilter, folder.getName(), srcFolder + "/" + fileName, zip);	            	
	            } 
	            else 
	            {
	                addFileToZip(useFilter, path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
	            }
	        }
	    }	    
	    
		@Override
		protected String doInBackground(String... params) 
		{
			try 
			{
				countFiles(new File(params[0]));
				
				zippedFolder = params[2];
				outputFile = params[1];
				zipFolder(params[0], params[1]);
				
				return params[1];
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			return "ERROR";
		}	    
	    
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
        protected void onPostExecute(String result) 
        {
        	if (!result.equals("ERROR"))
        	{
        		/*MessageInfo.infoEx(getLocaleString("gameprof_export_success") + result);

				if (shareWithServices) {
					AppGlobal.shareFile(new File(outputFile));
				}*/

				AlertDialog.Builder builder = new AlertDialog.Builder(AppGlobal.context);
				builder.setTitle(magiclib.R.string.app_name);
				builder.setMessage(getLocaleString("gameprof_export_success") + result + "\n\n" + getLocaleString("msg_share_file"));
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						AppGlobal.shareFile(new File(outputFile));
					}
				});

				builder.setNegativeButton("Cancel", null);
				builder.create().show();
        	}
        	else
        	{
        		MessageInfo.info("gameprof_export_failed");
        	}
        	
        	dismiss();
        }
	}
	
	private ExportInformation information;
	private ImageView avatar;
	private TextView title;
	private EditText fileNameEdit;
	private ImageButton confirm;
	private ExportGameProfile exporter;
	private LinearLayout initPanel;
	private LinearLayout progressPanel;	
	private ProgressBar progressBar;
	private CollectionItem game;
	private EditText author;
	private EditText devices;
	private EditText note;
	private CheckBox exportScreenhots;

	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.game_profile_export_filename_title,           "gameprof_export_filename");
		localize(R.id.game_profile_export_author_title,             "gameprof_export_author");
		localize(R.id.game_profile_export_compatible_devices_title, "gameprof_export_compatible_devices");
		localize(R.id.game_profile_export_note_title,               "common_note");
		localize(R.id.game_profile_export_exportscreenshots,        "gameprof_export_screenshots");
		localize(R.id.game_profile_export_exporting,                "gameprof_export_screenshots");
	}
	
	public uiGameProfileExport(Context context, CollectionItem game)
	{
		super(context);

		setContentView(R.layout.game_profile_export);
		setCaption("gameprof_export_title");
		
		this.game = game;
		
		avatar = (ImageView)findViewById(R.id.game_profile_export_avatar);
		title = (TextView)findViewById(R.id.game_profile_export_gametitle);
		fileNameEdit = (EditText)findViewById(R.id.game_profile_export_filename);
		confirm = (ImageButton)findViewById(R.id.game_profile_export_confirm);
		initPanel = (LinearLayout)findViewById(R.id.game_profile_export_initpanel);
		progressPanel = (LinearLayout)findViewById(R.id.game_profile_export_progresspanel);
		progressBar = (ProgressBar)findViewById(R.id.game_profile_export_progressbar);
		author = (EditText)findViewById(R.id.game_profile_export_author);
		devices = (EditText)findViewById(R.id.game_profile_export_compatible_devices);
		note = (EditText)findViewById(R.id.game_profile_export_note);
		exportScreenhots = (CheckBox)findViewById(R.id.game_profile_export_exportscreenshots);
		
		Bitmap bitmap = BitmapFactory.decodeFile(AppGlobal.gamesDataPath + game.getID() + "/" + game.getAvatar());
		avatar.setImageBitmap(bitmap);

		title.setText(game.getDescription());
		fileNameEdit.setText(fixFileName(game.getDescription()));

		initPanel.setVisibility(View.VISIBLE);
		progressPanel.setVisibility(View.GONE);

		confirm.setOnClickListener(confirmEvent());
	}
	
	private String fixFileName(String name) 
	{
		name = name.replace("&", "and");
		name = name.replaceAll("[^a-zA-Z0-9.-]", "_");
		name = name.replaceAll("(\\_)\\1+","$1");
		return name;
	}
	
	private boolean collectInformation()
	{
		try 
		{
			if (information == null)
			{
				information = new ExportInformation();
			}
		
			information.author = author.getText().toString().trim();
		
			if (information.author.equals(""))
			{
				MessageInfo.info("gameprof_export_missing_author");
				return false;
			}

			information.devices = devices.getText().toString().trim();
		
			if (information.devices.equals(""))
			{	
				MessageInfo.info("gameprof_export_missing_device");
				return false;
			}		
		
			information.title = game.getDescription();
			information.note = note.getText().toString();
		
			PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
			information.appVersion = pInfo.versionName;
			return true;
		} 
		catch (NameNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	@SuppressWarnings("deprecation")
	private void setRealScreenSize()
	{
		WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

	    if (Build.VERSION.SDK_INT >= 17)
	    {
	        //new pleasant way to get real metrics
	        DisplayMetrics realMetrics = new DisplayMetrics();
	        display.getRealMetrics(realMetrics);
	        information.realScreenWidth = realMetrics.widthPixels;
	        information.realScreenHeight = realMetrics.heightPixels;

	    } 
	    else if (Build.VERSION.SDK_INT >= 14) 
	    {
	        //reflection for this weird in-between time
	        try 
	        {
	            Method mGetRawH = Display.class.getMethod("getRawHeight");
	            Method mGetRawW = Display.class.getMethod("getRawWidth");
	            information.realScreenWidth = (Integer) mGetRawW.invoke(display);
	            information.realScreenHeight = (Integer) mGetRawH.invoke(display);
	        } catch (Exception e) 
	        {
	            //this may not be 100% accurate, but it's all we've got
	        	information.realScreenWidth = display.getWidth();
	        	information.realScreenHeight = display.getHeight();
	        }

	    } 
	    else 
	    {
	        //This should be close, as lower API devices should not have window navigation bars
	    	information.realScreenWidth = display.getWidth();
	    	information.realScreenHeight = display.getHeight();
	    }		
	}
	
	private View.OnClickListener confirmEvent()
	{
		return new View.OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				if (!collectInformation())					
					return;
				
		    	AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		    	builder.setTitle(Localization.getString("gameprof_export_orientation_detection"));
				builder.setMessage(getLocaleString("gameprof_export_correct_orientation"));
				
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface arg0, int arg1) 
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
						
						boolean error = true;
						
						try
						{
							WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
							Display display = wm.getDefaultDisplay();
					        
							information.screenWidth = Screen.screenWidth;//AppGlobal.getDisplayWidth(display);
							information.screenHeight = Screen.screenHeight;//AppGlobal.getDisplayHeight(display);
							information.orientation = Screen.orientation;//getContext().getResources().getConfiguration().orientation;
							information.dpi = getContext().getResources().getDisplayMetrics().density;
						
							setRealScreenSize();
							
							String fileName = fixFileName(fileNameEdit.getText().toString());
						
							File exportDir = new File(AppGlobal.appExportPath);
							if (!exportDir.exists())
								exportDir.mkdirs();
						
							Serializer serializer = new Persister();
							
							try 
							{
								File tempPath = new File(AppGlobal.appTempPath);
								if (!tempPath.exists())
									tempPath.mkdirs();
							
								File exportInfo = new File(AppGlobal.appTempPath + "/exportinfo.xml");
								exportInfo.delete();
								
								serializer.write(information, exportInfo);
								error = false;
							} 
							catch (Exception e) 
							{
								e.printStackTrace();
								if (Log.DEBUG) Log.log("save information failed");
							}						
						
							if (!error)
							{
								error = true;
								
								exporter = new ExportGameProfile();		
								exporter.excludeScreenshots = !exportScreenhots.isChecked();
								exporter.execute(AppGlobal.gamesDataPath + game.getID(), AppGlobal.appExportPath + fileName + ".mgc", game.getID());
								error = false;
							}
						}
						catch(Exception exc)
						{
							if (Log.DEBUG) Log.log("Export error : " + exc.getMessage());						
						}
						
						if (error)
						{							
							MessageInfo.info("gameprof_export_failed");
							dismiss();
						}
					}
				});
				
				builder.create().show();				
			}
		};
	}
}

@Root
class ExportInformation
{
	/*main info*/
	@Element
	public String title;
	
	@Element
	public String author;
	
	@Element
	public String devices;

	@Element(data=true, required=false)
	public String note;	
	
	/*technical info*/
	
	@Element
	public String appVersion;

	@Element
	public int screenWidth;	
	
	@Element
	public int screenHeight;	
	
	@Element
	public int realScreenWidth;	
	
	@Element
	public int realScreenHeight;		
	
	@Element
	public float dpi;	
	
	@Element
	public int orientation;			
}
