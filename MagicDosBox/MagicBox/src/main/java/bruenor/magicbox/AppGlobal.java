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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import magiclib.Global;
import magiclib.controls.ImageViewerItem;
import magiclib.core.ClientProject;
import magiclib.core.EmuManager;
import magiclib.core.EmuManagerMode;
import magiclib.layout.Layout;
import magiclib.layout.widgets.Folder;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetType;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;

class ProjectSpecificData extends ClientProject
{
	@Override
	public Field [] getDrawableFields()
	{
		return  R.drawable.class.getFields();
	}
}

class AppGlobal extends Global
{
	public static int systemWidgetDialogsCount;
	public static int themeType = 0;//0 - standard, 1 - fantasy


	public static void setupCurrentGame(Context context, String ID)
	{
		systemWidgetDialogsCount = 0;

		currentGameRootPath = gamesDataPath + ID + "/";
		currentGameConfigFile = currentGameRootPath + "config.xml";
		currentLayoutFile = currentGameRootPath + "layouts.xml";
		currentGameJournalsPath = currentGameRootPath + "Journals/";
		currentGameScreenShotsPath = currentGameRootPath + "Screenshots/";
		currentGameTempPath = currentGameRootPath + "Temp/";
		currentGameImagesPath = currentGameRootPath + "Images/";
		currentGameFontsPath = currentGameRootPath + "Fonts/";
		
		densityScale = context.getResources().getDisplayMetrics().density;
		try {
			vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

			if (Build.VERSION.SDK_INT >= 11) {
				if (!vibrator.hasVibrator()) {
					vibrator = null;
				}
			}

		} catch (Exception e) {
			vibrator = null;
		}

	}
	
	public static int getIndex(MotionEvent event)
	{
		  int idx = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

		  return idx;
	}
	
	public static boolean createOptionsMenu(Menu menu)
	{
		menu.addSubMenu(0,0,0,Localization.getString("navmenu_input_mode"));		
		menu.addSubMenu(0,1,1,Localization.getString("navmenu_general_settings"));
		menu.addSubMenu(0,2,2,Localization.getString("common_keyboard"));

		return true;
	}
	
	public static boolean optionsItemSelected(Context context, MenuItem item) 
	{	
		switch (item.getItemId())
		{
			case 0:
			{
				showInputMethodMenu();
				break;
			}
			case 1:
			{
				if ((EmuManager.mode == EmuManagerMode.design) ||
				    (EmuManager.mode == EmuManagerMode.play))
				{
					uiSettingsDialog dlg = new uiSettingsDialog();
					dlg.show();
				}
				else
					MessageInfo.shortInfo("msg_disabled_generalsettings");
				break;
			}
			case 2:
			{
				EmuManager.showOsKeyboard();
				break;
			}			
		}
		
		return true;
	}
	
	public static void showInputMethodMenu()
	{
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

		if (imm != null)
		{
			imm.showInputMethodPicker();							
		}
	}
	
	private static void addFolderChildren(List<ImageViewerItem> list, Folder folder, boolean all, List<WidgetType> enabledList)
	{
		for (Widget w : folder.getWidgets())
		{
			if (all || enabledList.contains(w.getType()))
			{
				list.add(uiImageViewer.getImageViewerItemFromWidget(w));
			}
		}
	}	
	
	public static void addAvailableMappings(List<ImageViewerItem> list,  WidgetType [] enabledTypes, boolean inChildren)
	{
		Layout layout = EmuManager.getCurrentLayout();

		boolean all = (enabledTypes.length == 0);
		List<WidgetType> enabledList = Arrays.asList(enabledTypes);

		for (Widget w : layout.widgets)
		{
			if (w.getType() == WidgetType.folder)
			{
				if (inChildren)
					addFolderChildren(list, (Folder)w, all, enabledList);

				if (all || enabledList.contains(w.getType()))
				{
					list.add(uiImageViewer.getImageViewerItemFromWidget(w));
				}
			}
			else
			{
				if (all || enabledList.contains(w.getType()))
				{
					list.add(uiImageViewer.getImageViewerItemFromWidget(w));
				}
			}
		}
	}

	public static void disposeImageView(ImageView img)
	{
		Drawable drawable = img.getDrawable();	        	
		drawable.setCallback(null);
	
		if (drawable instanceof BitmapDrawable) 
		{
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			Bitmap bitmap = bitmapDrawable.getBitmap();
			bitmap.recycle();
		}
	}
	
	public static void setAlpha(View view, int alpha)
	{
		if (Build.VERSION.SDK_INT < 11)
		{
			AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
			animation.setDuration(0);
			animation.setFillAfter(true);
			view.startAnimation(animation);
			view.refreshDrawableState();
		}
		else
		{
			view.setAlpha(alpha);
		}			
	}
}

