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
import java.util.Arrays;
import java.util.Comparator;

import bruenor.magicbox.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class uiScreenshotGallery extends Dialog
{
	private static String currentFilename = "";
	
	private ImageView screenshot = null;
	private File[] files;
	private int minIndex = 0;
	private int maxIndex = 0;
	private int fileIndex = 0;
	
	private class ScrShotGestureDetector extends SimpleOnGestureListener 
	{
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) 
        {        	
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) 
            {
                if (fileIndex < maxIndex)
                {
                	fileIndex++;
                	showImage();
                }
            }
            else 
            if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) 
            {
                if (fileIndex > minIndex)
                {
                	fileIndex--;                	
                	showImage();
                }
            }
            
            return super.onFling(e1, e2, velocityX, velocityY);
        }
	}
		
	public uiScreenshotGallery(Context context)
	{
		super(context);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.screenshot_gallery);
		screenshot = (ImageView)findViewById(R.id.screenshot_image);
		screenshot.setScaleType(ScaleType.FIT_CENTER);
		
		final GestureDetector gestureDetector = new GestureDetector(context, new ScrShotGestureDetector());
		
		screenshot.setOnTouchListener(new View.OnTouchListener()
		{			
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				return (!gestureDetector.onTouchEvent(event));
			}
		});
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
	    lp.height = WindowManager.LayoutParams.MATCH_PARENT;	    
	    getWindow().setAttributes(lp);
	}
	
	public void setFiles(File[] files)
	{
		this.files = files;
		
		maxIndex = files.length - 1; 
		
		Arrays.sort(this.files, new Comparator<File>(){
		    public int compare(File f1, File f2)
		    {
		        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
		    } });		
		
		fileIndex = getCurrentFileIndex();
		
		showImage();
	}
	
	private void showImage()
	{
		Bitmap bmp = BitmapFactory.decodeFile(files[fileIndex].getAbsolutePath());
		screenshot.setImageBitmap(bmp);
		
		currentFilename = files[fileIndex].getName(); 
	}
	
	private int getCurrentFileIndex()
	{
		if (currentFilename.equals(""))
			return 0;
		
		int i = 0;
		
		for(File f : files)
		{
			if (f.getName().equals(currentFilename))
			{
				return i;
			}
			
			i++;
		}
		
		return 0;
	}
}
