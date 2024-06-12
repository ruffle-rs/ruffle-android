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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import magiclib.Global;
import magiclib.controls.Dialog;
import magiclib.controls.ImageViewer;
import magiclib.core.EmuManager;
import magiclib.core.Screen;
import magiclib.keyboard.Key;
import magiclib.keyboard.KeyCodeInfo;
import magiclib.layout.widgets.KeyWidget;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetType;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;

public class uiAddWidget extends Dialog
{
	private float widgetLeft;
	private float widgetTop;
	private int imgSize = 0;
	private int containerSize = 0;
	private AddWidgetAdapter adapter;
	private GridView grid;
	private int twoColumns = -1;

	public List<AddWidgetItem> widgets = new ArrayList<>();
	
	class AddWidgetAdapter extends ArrayAdapter<AddWidgetItem> 
	{
		private List<AddWidgetItem> items;

	    public AddWidgetAdapter(Context context, int textViewResourceId, List<AddWidgetItem> items) 
	    {
	        super(context, textViewResourceId, items);
	        
	        this.items = items;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) 
	    {
	    	View v = convertView;
	    	
	    	ImageView avatar = null;
	    	LinearLayout textContainer = null;
	    	TextView title = null;
	    	TextView description = null;

	        if (v == null)
	        {
	            v = getLayoutInflater().inflate(R.layout.addwidget_item, null);

	            avatar = (ImageView) v.findViewById(R.id.addwidget_item_imageview);
	            textContainer = (LinearLayout) v.findViewById(R.id.addwidget_item_textcontainer);
	            title = (TextView) v.findViewById(R.id.addwidget_item_title);
	            description = (TextView) v.findViewById(R.id.addwidget_item_description);

	            title.setTextColor(Color.parseColor("#3399ff"));
	            description.setTextColor(Color.parseColor("#339900"));

            	avatar.getLayoutParams().height = imgSize;
            	avatar.getLayoutParams().width = imgSize;

				title.setTextSize(TypedValue.COMPLEX_UNIT_PX, ImageViewer.textSize);
				description.setTextSize(TypedValue.COMPLEX_UNIT_PX, ImageViewer.textSize);
				textContainer.getLayoutParams().height = imgSize;
				textContainer.getLayoutParams().width = containerSize;
	        }
	        
	        AddWidgetItem item = items.get(position);

	        if (item != null) 
	        {	
	        	if (avatar == null)
	        		avatar = (ImageView) v.findViewById(R.id.addwidget_item_imageview);
	        	
	        	if (title == null)
	        		title = (TextView) v.findViewById(R.id.addwidget_item_title);

	        	if (description == null)
	        		description = (TextView) v.findViewById(R.id.addwidget_item_description);

				if (textContainer == null)
					textContainer = (LinearLayout) v.findViewById(R.id.addwidget_item_textcontainer);

				if (textContainer.getLayoutParams().width != containerSize) {
					textContainer.getLayoutParams().width = containerSize;
				}

	        	avatar.setImageResource(item.getImageID());
	        	title.setText(item.getTitle());	            
	            description.setText(item.getDescription());
	            
				if (!AppGlobal.isDonated)
				{
					if (item.isDonated())
					{
						AppGlobal.setGrayScale(avatar);
						title.setTextColor(Color.GRAY);
						description.setTextColor(Color.GRAY);
					} else
					{
						AppGlobal.setColorScale(avatar);
			            title.setTextColor(Color.parseColor("#3399ff"));
			            description.setTextColor(Color.parseColor("#339900"));						
					}
				}	
	        }
	        
            return v;
	    }	    
	}
	
	@Override
	public void onSetLocalizedLayout() 
	{
		// TODO Auto-generated method stub
	}	
/*
	@Override
	public void onResize(int width, int height) {
		//MessageInfo.infoEx(String.format("orientation changed [%d,%d]", width, height));
		//getWindow().setLayout(width, height);

		GridView grid = (GridView)findViewById(R.id.addwidget_dialog_gridview);
		int itemWidth = imgSize + defaultContainerSize + Global.DensityToPixels(3);

		int marginWidth = (width - contentHolder.getWidth());

		int padding = Global.DensityToPixels(16);
		int twoColumns = grid.getListPaddingLeft() + (itemWidth * 2) + grid.getListPaddingRight();
		twoColumns += (padding * 2) + marginWidth;

		WindowManager wm = (WindowManager)AppGlobal.context. getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		if (width > AppGlobal.getDisplayWidth(display)) {
			getWindow().setLayout(-2, -2);
			return;
		}

		if (twoColumns > width) {
			int displayWidth = AppGlobal.getDisplayWidth(display);
			if (twoColumns <= displayWidth) {
				getWindow().setLayout(twoColumns, height);
			}else {
				//try without padding;
				if (twoColumns - displayWidth < 2*padding ) {
					padding = (2*padding - (twoColumns - displayWidth)) / 2;
					content.setPadding(padding, 0, padding,0);
					getWindow().setLayout(displayWidth, height);
				} else {
					getWindow().setLayout(-2, -2);
				}
			}
		}
	}*/

	private void setWindowSize() {
		//if (getContext().getResources().getConfiguration().orientation == 2) {
			if (grid == null) {
				grid = (GridView)findViewById(R.id.addwidget_dialog_gridview);
			}

			int outerMargin = getWindow().getDecorView() == null? 0 : (getWindow().getDecorView().getPaddingLeft() + getWindow().getDecorView().getPaddingRight());
			int itemWidth = imgSize + defaultContainerSize + Global.DensityToPixels(3);

		    Resources res = Global.context.getResources();
			int padding = (int)res.getDimension(R.dimen.padding_main);

			int listPadding = 0;

			try {
				listPadding = grid.getListPaddingLeft() + grid.getListPaddingRight();

				if (listPadding == 0) {
					Field f = grid.getClass().getSuperclass().getDeclaredField("mSelectionLeftPadding");
					f.setAccessible(true);
					listPadding+=f.getInt(grid);

					f = grid.getClass().getSuperclass().getDeclaredField("mSelectionRightPadding");
					f.setAccessible(true);
					listPadding+=f.getInt(grid);
				}
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			twoColumns = listPadding + (itemWidth * 2);
			twoColumns += (padding * 2) + outerMargin;

			WindowManager wm = (WindowManager) AppGlobal.context. getSystemService(Context.WINDOW_SERVICE);
			int displayWidth = Screen.screenWidth;//AppGlobal.getDisplayWidth(wm.getDefaultDisplay());

			if (twoColumns > displayWidth) {
				if (twoColumns - displayWidth <= 2*padding ) {
					padding = (2*padding - (twoColumns - displayWidth)) / 2;
					content.setPadding(padding, 0, padding, 0);
					getWindow().setLayout(displayWidth, -2);
				} else {
					getWindow().setLayout(-2, -2);
				}
			} else {
				getWindow().setLayout(twoColumns, -2);
				//getWindow().getDecorView().setMinimumWidth(twoColumns);
			}
		/*} else {
			getWindow().setLayout(-2, -2);
		}*/
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setWindowSize();
	};

	public uiAddWidget(Context context, float left, float top)
	{
		super(context);
		
		setContentView(R.layout.addwidget_dialog);
		setCaption("widget_new_caption");

		widgetLeft = left;
		widgetTop = top;

		loadItems();
		setWindowSize();
	}

	private int lastWidth = 0;
	private int defaultContainerSize;

	private void loadItems()
	{
		final GridView grid = (GridView)findViewById(R.id.addwidget_dialog_gridview);
		grid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int w = grid.getWidth();

				if (lastWidth == w)
					return;

				lastWidth = w;
				w = w - (grid.getListPaddingRight() + grid.getListPaddingLeft());

				int itemPadding = Global.DensityToPixels(3);
				int itemWidth = imgSize + defaultContainerSize + itemPadding;

				if (itemWidth > w) {
					itemWidth = w;
				}

				int colCount = (int) Math.floor(w / itemWidth);
				if (colCount == 0)
					colCount = 1;

				int diff = w - colCount * itemWidth;
				itemWidth += (int)Math.floor(diff / colCount);

				containerSize = itemWidth - imgSize - itemPadding;

				grid.setColumnWidth(itemWidth);
				grid.setStretchMode(GridView.NO_STRETCH) ;
			}
		});

		Resources res = Global.context.getResources();

		imgSize = (int)res.getDimension(R.dimen.addwidget_imgsize);
		defaultContainerSize = (int)res.getDimension(R.dimen.addwidget_defcontainerwidth);

		//grid.setColumnWidth(imgSize + containerSize + Global.DensityToPixels(3));

        //grid.setColumnWidth(imageSize.getInDPI() * 2 + (imageSize.getInDPI() / 3));
		//grid.setMinimumWidth(imgSize + containerSize + Global.DensityToPixels(3));
        grid.setStretchMode(GridView.NO_STRETCH) ;
        
		widgets.clear();
		
		widgets.add(new AddWidgetItem(WidgetType.key,
									  R.drawable.img_key, 
									  "widget_new_key", 
									  "widget_new_key_hint"));
		widgets.add(new AddWidgetItem(WidgetType.touch_action, 
									  R.drawable.img_mousepaw, 
									  "widget_new_mousetouch", 
									  "widget_new_mousetouch_hint"));
		
		if (!AppGlobal.isDonated)
		{
			widgets.add(new AddWidgetItem(WidgetType.special, 
                    					  R.drawable.img_magic, 
                    					  "widget_new_special", 
                    					  "widget_new_special_hint"));			
		}
				
		widgets.add(new AddWidgetItem(WidgetType.mouse_type,
									  R.drawable.img_navigation,
									  "widget_new_mousenavig",
									  "widget_new_mousenavig_hint"));
		widgets.add(new AddWidgetItem(WidgetType.dpad,
									  R.drawable.img_masterofpuppets,
									  "widget_new_joystick",
									  "widget_new_joystick_hint"));
		widgets.add(new AddWidgetItem(WidgetType.folder, 
				                      R.drawable.img_bag,
									  "widget_new_folder",
									  "widget_new_folder_hint"));
		widgets.add(new AddWidgetItem(WidgetType.journal,
									  R.drawable.img_journal,
									  "widget_new_journal",
									  "widget_new_journal_hint"));
		widgets.add(new AddWidgetItem(WidgetType.walkthrough,
									  R.drawable.img_map,
									  "widget_new_walkthrough",
									  "widget_new_walkthrough_hint"));
		widgets.add(new AddWidgetItem(WidgetType.combo,
									  R.drawable.img_magicanvil,
									  "widget_new_combo",
									  "widget_new_combo_hint"));
		widgets.add(new AddWidgetItem(WidgetType.point_click,
									  R.drawable.img_dummytarget,
									  "widget_new_target",
									  "widget_new_target_hint"));
		widgets.add(new AddWidgetItem(WidgetType.zoom,
				R.drawable.img_telescope,
				"widget_new_zoom",
				"widget_new_zoom_hint"));
/*
		if (AppGlobal.isDebuggable) {
			widgets.add(new AddWidgetItem(WidgetType.speech_to_text,
					R.drawable.icon_imgnone,
					"Speech to text",
					"???"));
		}*/

		// widgets.add(new AddWidgetItem(WidgetType.mouse_move,
		// R.drawable.icon_f10, "Mouse move", "Run mouse,  run!"));
		// widgets.add(new AddWidgetItem(WidgetType.switcher,
		// R.drawable.icon_f10, "Chameleon", "Chameleon"));
		
		if (AppGlobal.isDonated)
		{		
			widgets.add(new AddWidgetItem(WidgetType.special, 
										  R.drawable.img_magic, 
										  "widget_new_special", 
										  "widget_new_special_hint"));
		}
		
		for (AddWidgetItem w : widgets)
		{
			w.setDonated(AppGlobal.isDonatedWidget(w.getType()));
		}

		adapter = new AddWidgetAdapter(getContext(), android.R.layout.simple_list_item_1, widgets);
		grid.setAdapter(adapter);
		
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() 
        {
        	  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) 
        	  {        		  
        		  AddWidgetItem item = (AddWidgetItem)grid.getItemAtPosition(position);
        		  
        		  if (item != null) {
					  if ((!AppGlobal.isDonated) && (item.isDonated())) {
						  MessageInfo.info("msg_donated_feature");
						  return;
					  }

					  switch (item.getType()) {
						  case key: {
							  addKeyWidget(widgetLeft, widgetTop);
							  break;
						  }
						  default: {
							  addWidgetToLayout(EmuManager.createWidgetByType(item.getType(), widgetLeft, widgetTop));
							  dismiss();
						  }
					  }
				  }
        		  
        	  }
        });			
	}

	private void addKeyWidget(final float left, final float top)
	{
		dismiss();

		uiKeyCodesDialog d = new uiKeyCodesDialog(AppGlobal.context);
		d.setOnKeyCodeListener(new KeyCodeListener()
		{
			@Override
			public void onPick(KeyCodeItem selected)
			{
				String title = KeyCodeInfo.getDosboxKeyInfo(selected.getKeyCode(), false);

				int size = AppGlobal.widgetSize;
				KeyWidget w = new KeyWidget(left, top, size, size, title);

				Key key = w.getDesignKey(0);
				key.setEnabled(true);
				key.setKeyCode(selected.getKeyCode());

				w.setActiveKeys();
				addWidgetToLayout(w);
			}
		});

		d.show();
	}

	private void addWidgetToLayout(Widget w)
	{
		if (w != null)
		{
			w.setTransparency(100);
			w.update();

			EmuManager.addWidget(w);
			EmuManager.addNewWidget(w);
		}
	}
}

class AddWidgetItem
{
	private int imageID;
	private String title;
	private WidgetType type;
	private String description;
	private boolean donated;

	/**
	 * @param title - localized title code
	 * @param description - localized description code*/
	public AddWidgetItem(WidgetType type, int imageID, String title, String description)
	{
		this.type = type;
		this.imageID = imageID;
		this.title = Localization.getString(title);
		this.description = Localization.getString(description);
	}

	public int getImageID()
	{
		return imageID;
	}
	public void setImageID(int imageID)
	{
		this.imageID = imageID;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public WidgetType getType()
	{
		return type;
	}

	public void setType(WidgetType type)
	{
		this.type = type;
	}
	
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public boolean isDonated()
	{
		return donated;
	}

	public void setDonated(boolean donated)
	{
		this.donated = donated;
	}
}