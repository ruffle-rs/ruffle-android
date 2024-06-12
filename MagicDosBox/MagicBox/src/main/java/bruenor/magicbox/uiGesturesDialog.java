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
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import magiclib.controls.Dialog;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.Direction;
import magiclib.core.EmuManager;
import magiclib.gestures.DoubleTapItem;
import magiclib.gestures.LongPressItem;
import magiclib.gestures.SwipeItem;
import magiclib.gestures.Swipes;
import magiclib.gestures.TwoPointTapItem;
import magiclib.layout.Layout;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetType;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;
import magiclib.mouse.MouseButton;

class GesturesBaseDialog extends Dialog {
	protected LayoutInflater li;
	public GesturesBaseDialog(Context context) {
		super(context);
		li = getLayoutInflater();
	}
}

class GesturesSettings extends GesturesBaseDialog {
	private final int DBLTAP_MENU_ID = -1000;
	private final int TWOPOINTTAP_MENU_ID = -1001;
	private final int LONGPRESS_MENU_ID = -1002;
	private final int SWIPES_MENU_ID = -1003;

	private LinearLayout mainView;
	private View dblTapMenu;
	private View twoPointTapMenu;
	private View longpressMenu;
	private View swipesMenu;

	//temporary variables
	//double tap
	private boolean temp_dbltap_enabled;
	private boolean temp_dbltap_zoom;
	private MouseButton temp_dbltap_mousebutton;
	//two point
	private boolean temp_twopoint_enabled;
	private MouseButton temp_twopoint_mousebutton;
	private boolean temp_twopoint_keepmousedown;
	//longpress
	private boolean temp_longpress_enabled;
	private MouseButton temp_longpress_mousebutton;
	private boolean temp_longpress_vibrate;
	private boolean temp_longpress_showinfo;
	private String temp_longpress_infoMessage;
	private int temp_longpress_timing;
	//swipes
	private Swipes temp_swipes = new Swipes();
	private List<ImageViewerItem> widgets;

	private View.OnClickListener onClick;

	public void onSetLocalizedLayout() {
		//localize(R.id.ctrlscheme_doubletap_caption, "common_doubletap");
	}

	public GesturesSettings() {
		super(AppGlobal.context);

		setContentView(R.layout.gestures);
		setCaption("genset_menu_gestures");

		mainView = (LinearLayout) findViewById(R.id.gestures_menu);

		//addSection(li, Localization.getString("genset_menu_gestures"), -1);
		dblTapMenu = addDblTapOption();
		twoPointTapMenu = addTwoPointTapOption();
		longpressMenu = addLongpressOption();
		swipesMenu = addSwipespOption();

		Layout currentLayout = EmuManager.getCurrentLayout();

		if (currentLayout.dblTap == null)
			currentLayout.dblTap =  new DoubleTapItem();

		if (currentLayout.twoPointTap == null)
			currentLayout.twoPointTap =  new TwoPointTapItem();

		if (currentLayout.longpress == null)
			currentLayout.longpress =  new LongPressItem();

		//double tap
		temp_dbltap_enabled = currentLayout.dblTap.enabled;
		temp_dbltap_zoom = currentLayout.dblTap.zoom;
		temp_dbltap_mousebutton = currentLayout.dblTap.mouseButton;

		//two point tap
		temp_twopoint_enabled = currentLayout.twoPointTap.enabled;
		temp_twopoint_mousebutton = currentLayout.twoPointTap.mouseButton;
		temp_twopoint_keepmousedown = currentLayout.twoPointTap.doMouseDownOnly;

		//longpress
		temp_longpress_enabled = currentLayout.longpress.enabled;
		temp_longpress_mousebutton = currentLayout.longpress.mouseButton;
		temp_longpress_vibrate = currentLayout.longpress.vibrate;
		temp_longpress_showinfo = currentLayout.longpress.showInfo;
		temp_longpress_infoMessage = currentLayout.longpress.infoMessage;
		temp_longpress_timing = currentLayout.longpress.timing;
		//swipes
		currentLayout.swipes.copyTo(temp_swipes);

		setDblTapMenuValue();
		setTwoPointTapMenuValue();
		setLongpressMenuValue();
		setSwipesMenuValue();

		findViewById(R.id.gestures_confirm).setOnClickListener(getOnClickEvent());
	}

	private void setDblTapMenuValue() {
		if (!temp_dbltap_enabled) {
			setMenuItemValue(dblTapMenu, Localization.getString("common_disabled"));
			return;
		}

		if (temp_dbltap_zoom) {
			setMenuItemValue(dblTapMenu, Localization.getString("common_zoom"));
			return;
		}

		switch (temp_dbltap_mousebutton) {
			case left: {
				setMenuItemValue(dblTapMenu, Localization.getString("mouse_button_lleft"));
				break;
			}
			case middle: {
				setMenuItemValue(dblTapMenu, Localization.getString("mouse_button_lmiddle"));
				break;
			}
			case right: {
				setMenuItemValue(dblTapMenu, Localization.getString("mouse_button_lright"));
				break;
			}
		}
	}

	private void setTwoPointTapMenuValue() {
		if (!temp_twopoint_enabled) {
			setMenuItemValue(twoPointTapMenu, Localization.getString("common_disabled"));
			return;
		}

		switch (temp_twopoint_mousebutton) {
			case left: {
				setMenuItemValue(twoPointTapMenu, Localization.getString("mouse_button_lleft"));
				break;
			}
			case middle: {
				setMenuItemValue(twoPointTapMenu, Localization.getString("mouse_button_lmiddle"));
				break;
			}
			case right: {
				setMenuItemValue(twoPointTapMenu, Localization.getString("mouse_button_lright"));
				break;
			}
		}
	}

	private void setLongpressMenuValue() {
		if (!temp_longpress_enabled) {
			setMenuItemValue(longpressMenu, Localization.getString("common_disabled"));
			return;
		}

		switch (temp_longpress_mousebutton) {
			case left: {
				setMenuItemValue(longpressMenu, Localization.getString("mouse_button_lleft"));
				break;
			}
			case middle: {
				setMenuItemValue(longpressMenu, Localization.getString("mouse_button_lmiddle"));
				break;
			}
			case right: {
				setMenuItemValue(longpressMenu, Localization.getString("mouse_button_lright"));
				break;
			}
		}
	}

	private void setSwipesMenuValue() {
		if (!temp_swipes.isEnabled()) {
			setMenuItemValue(swipesMenu, Localization.getString("common_disabled"));
			return;
		}

		String value = "";

		for (SwipeItem swipe : temp_swipes.swipes)
		{
			if (swipe.isEnabled()) {
				switch (swipe.getDirection()) {
					case up: {
						value += Localization.getString("arrow_up") + " ";
						break;
					}
					case down: {
						value += Localization.getString("arrow_down") + " ";
						break;
					}
					case left: {
						value += Localization.getString("arrow_left") + " ";
						break;
					}
					case right: {
						value += Localization.getString("arrow_right") + " ";
						break;
					}
				}
			}
		}

		setMenuItemValue(swipesMenu, value);
	}

	private void setMenuItemValue(View view, String value) {
		((TextView) view.findViewById(R.id.gesture_menuitem_value)).setText(value);
	}

	private View addDblTapOption() {
		return addMenuItem(DBLTAP_MENU_ID, R.drawable.icon_dbltap, Localization.getString("common_doubletap"));
	}

	private View addTwoPointTapOption() {
		return addMenuItem(TWOPOINTTAP_MENU_ID, R.drawable.icon_twopointtap, Localization.getString("common_twopointtap"));
	}

	private View addLongpressOption() {
		return addMenuItem(LONGPRESS_MENU_ID, R.drawable.icon_longpress, Localization.getString("common_longpress"));
	}

	private View addSwipespOption() {
		return addMenuItem(SWIPES_MENU_ID, R.drawable.icon_swipes, Localization.getString("common_swipes"));
	}

	private void addSection(LayoutInflater li, String title, int index) {
		TextView textView = (TextView) li.inflate(R.layout.section_item, null);
		textView.setText(title);

		if (index > -1) {
			mainView.addView(textView, index);
		} else {
			mainView.addView(textView);
		}
	}

	private View addMenuItem(int ID, int imageResource, String title) {
		RelativeLayout view = (RelativeLayout) li.inflate(R.layout.gesture_menuitem, null);
		view.setId(ID);

		ImageView image = (ImageView) view.findViewById(R.id.gesture_menuitem_image);
		image.setImageResource(imageResource);

		TextView label = (TextView) view.findViewById(R.id.gesture_menuitem_label);
		label.setText(title);

		view.setOnClickListener(getOnClickEvent());

		mainView.addView(view);

		return view;
	}

	private View.OnClickListener getOnClickEvent() {
		if (onClick == null) {
			onClick = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
						case R.id.gestures_confirm: {
							Layout currentLayout = EmuManager.getCurrentLayout();

							//doubletap
							currentLayout.dblTap.enabled = temp_dbltap_enabled;
							currentLayout.dblTap.mouseButton = temp_dbltap_mousebutton;
							currentLayout.dblTap.zoom = temp_dbltap_zoom;

							//twopoint
							currentLayout.twoPointTap.enabled = temp_twopoint_enabled;
							currentLayout.twoPointTap.mouseButton = temp_twopoint_mousebutton;
							currentLayout.twoPointTap.doMouseDownOnly = temp_twopoint_keepmousedown;

							//longpress
							currentLayout.longpress.enabled = temp_longpress_enabled;
							currentLayout.longpress.mouseButton = temp_longpress_mousebutton;
							currentLayout.longpress.vibrate = temp_longpress_vibrate;
							currentLayout.longpress.showInfo = temp_longpress_showinfo;
							currentLayout.longpress.infoMessage = temp_longpress_infoMessage;
							currentLayout.longpress.timing = temp_longpress_timing;

							//swipes
							temp_swipes.copyTo(currentLayout.swipes);

							currentLayout.update();

							dismiss();
							break;
						}
						case DBLTAP_MENU_ID: {
							showDblTapOptions();
							break;
						}
						case TWOPOINTTAP_MENU_ID: {
							showTwoPointTapOptions();
							break;
						}
						case LONGPRESS_MENU_ID: {
							showLongpressTapOptions();
							break;
						}
						case SWIPES_MENU_ID: {
							showSwipesOptions();
							break;
						}
					}
				}
			};
		}

		return onClick;
	}

	private void showDblTapOptions() {
		final Dialog parent = this;

		final uiImageViewer viewer = new uiImageViewer(getContext());
		viewer.setCaption("common_choose");

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				images.add(new ImageViewerItem(R.drawable.icon_disabled, "disabled", "common_disabled"));
				images.add(new ImageViewerItem(R.drawable.icon_mouse2, "mouse", "common_mouse"));
				images.add(new ImageViewerItem(R.drawable.img_telescope, "zoom", "common_zoom"));
				return true;
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("disabled")) {
					temp_dbltap_enabled = false;
					setDblTapMenuValue();
				} else if (selected.getName().equals("mouse")) {
					viewer.dismiss();
					MouseGestureSettings d = new MouseGestureSettings(temp_dbltap_mousebutton, false, false);
					d.setOnMouseGestureEventListener(new MouseGestureEventListener() {
						@Override
						public void onPick(MouseButton button, boolean keepMouseDown) {
							temp_dbltap_enabled = true;
							temp_dbltap_mousebutton = button;
							setDblTapMenuValue();
						}
					});
					d.show();
				} else if (selected.getName().equals("zoom")) {
					temp_dbltap_enabled = true;
					temp_dbltap_zoom = true;
					setDblTapMenuValue();
				}
			}
		});

		viewer.show();
	}

	private void showTwoPointTapOptions() {
		final uiImageViewer viewer = new uiImageViewer(getContext());
		viewer.setCaption("common_choose");

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				images.add(new ImageViewerItem(R.drawable.icon_disabled, "disabled", "common_disabled"));
				images.add(new ImageViewerItem(R.drawable.icon_mouse2, "mouse", "common_mouse"));
				return true;
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("disabled")) {
					temp_twopoint_enabled = false;
					setTwoPointTapMenuValue();
				} else if (selected.getName().equals("mouse")) {
					viewer.dismiss();
					MouseGestureSettings d = new MouseGestureSettings(temp_twopoint_mousebutton, true, temp_twopoint_keepmousedown);
					d.setOnMouseGestureEventListener(new MouseGestureEventListener() {
						@Override
						public void onPick(MouseButton button, boolean keepMouseDown) {
							temp_twopoint_enabled = true;
							temp_twopoint_mousebutton = button;
							temp_twopoint_keepmousedown = keepMouseDown;
							setTwoPointTapMenuValue();
						}
					});
					d.show();
				}
			}
		});

		viewer.show();
	}

	private void showLongpressTapOptions() {
		final uiImageViewer viewer = new uiImageViewer(getContext());
		viewer.setCaption("common_choose");

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				images.add(new ImageViewerItem(R.drawable.icon_disabled, "disabled", "common_disabled"));
				images.add(new ImageViewerItem(R.drawable.icon_mouse2, "mouse", "common_mouse"));
				return true;
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("disabled")) {
					temp_longpress_enabled = false;
					setLongpressMenuValue();
				} else if (selected.getName().equals("mouse")) {
					viewer.dismiss();
					LongpressGestureSettings d = new LongpressGestureSettings(temp_longpress_mousebutton, temp_longpress_vibrate,
							temp_longpress_showinfo, temp_longpress_infoMessage, temp_longpress_timing);
					d.setOnLongpressGestureEventListener(new LongpressGestureEventListener() {
						@Override
						public void onPick(MouseButton button, boolean vibrate, boolean showInfo, String message, int timing) {
							temp_longpress_enabled = true;
							temp_longpress_mousebutton = button;
							temp_longpress_vibrate = vibrate;
							temp_longpress_showinfo = showInfo;
							temp_longpress_infoMessage = message;
							temp_longpress_timing = timing;
							setLongpressMenuValue();
						}
					});
					d.show();
				}
			}
		});

		viewer.show();
	}

	private void showSwipesOptions() {
		final Dialog parent = this;
		parent.hide();
		SwipesGestureSettings d = new SwipesGestureSettings(this, temp_swipes, widgets);
		d.setOnSwipesGestureEventListener(new SwipesGestureEventListener() {
			@Override
			public void onPick(Swipes swipes, List<ImageViewerItem> w) {
				swipes.copyTo(temp_swipes);
				widgets = w;
				setSwipesMenuValue();
				parent.show();
			}
		});
		d.show();
	}
}

interface SwipesGestureEventListener {
	void onPick(Swipes swipes, List<ImageViewerItem> widgets);
}

class SwipesGestureSettings extends Dialog {
	private SwipesGestureEventListener event;
	private Swipes swipes;
	private Dialog parent;
	private List<ImageViewerItem> widgets;
	private View.OnClickListener onClick;

	@Override
	protected void onStop() {
		super.onStop();
		parent.show();
	}
	public void setOnSwipesGestureEventListener(SwipesGestureEventListener event) {
		this.event = event;
	}
	public SwipesGestureSettings(Dialog parent, Swipes sw, List<ImageViewerItem> w) {
		super(AppGlobal.context);

		setContentView(R.layout.gesture_swipes);
		setCaption("common_swipes");

		this.parent = parent;

		this.swipes = new Swipes();
		sw.copyTo(this.swipes);

		if (w == null) {
			this.widgets = new LinkedList<>();
			buildWidgetList();
		} else {
			this.widgets = w;
		}

		View.OnClickListener swipeSettingsClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Direction d = null;

				switch (v.getId())
				{
					case R.id.swipes_up_settings:
					{
						d = Direction.up;
						break;
					}
					case R.id.swipes_down_settings:
					{
						d = Direction.down;
						break;
					}
					case R.id.swipes_left_settings:
					{
						d = Direction.left;
						break;
					}
					case R.id.swipes_right_settings:
					{
						d = Direction.right;
						break;
					}
				}

				SwipeItem item = swipes.getSwipeItem(d);

				if (item.getWidget() == null)
					return;

				uiSwipeSettingsDialog dlg = new uiSwipeSettingsDialog(getContext(), swipes.getSwipeItem(d));
				dlg.show();
			}
		};

		findViewById(R.id.swipes_up_settings).setOnClickListener(swipeSettingsClick);
		findViewById(R.id.swipes_down_settings).setOnClickListener(swipeSettingsClick);
		findViewById(R.id.swipes_left_settings).setOnClickListener(swipeSettingsClick);
		findViewById(R.id.swipes_right_settings).setOnClickListener(swipeSettingsClick);

		View.OnClickListener swipesWidgetClick = new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				if (widgets.size() == 0)
				{
					MessageInfo.info("msg_no_widgets");
					return;
				}

				uiImageViewer viewer = new uiImageViewer(getContext());
				viewer.setCaption("common_widgets");
				viewer.useItemBackground = true;
				viewer.initAdapter(widgets);

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
					@Override
					public void onPick(ImageViewerItem selected)
					{
						Button b = (Button) v;

						switch (v.getId()) {
							case R.id.swipes_keyup:
							{
								setSwipeItem(Direction.up, (Widget)selected.getTag());
								setSwipeInfo(b, selected, Direction.up);
								break;
							}
							case R.id.swipes_keydown:
							{
								setSwipeItem(Direction.down, (Widget)selected.getTag());
								setSwipeInfo(b, selected, Direction.down);
								break;
							}
							case R.id.swipes_keyleft:
							{
								setSwipeItem(Direction.left, (Widget)selected.getTag());
								setSwipeInfo(b, selected, Direction.left);
								break;
							}
							case R.id.swipes_keyright:
							{
								setSwipeItem(Direction.right, (Widget)selected.getTag());
								setSwipeInfo(b, selected, Direction.right);
								break;
							}
						}
					}
				});

				viewer.show();
			}
		};

		Button but = (Button)getView().findViewById(R.id.swipes_keyup);
		but.setOnClickListener(swipesWidgetClick);
		setSwipeInfo(but, null, Direction.up);

		but = (Button)getView().findViewById(R.id.swipes_keydown);
		but.setOnClickListener(swipesWidgetClick);
		setSwipeInfo(but, null, Direction.down);

		but = (Button)getView().findViewById(R.id.swipes_keyleft);
		but.setOnClickListener(swipesWidgetClick);
		setSwipeInfo(but, null, Direction.left);

		but = (Button)getView().findViewById(R.id.swipes_keyright);
		but.setOnClickListener(swipesWidgetClick);
		setSwipeInfo(but, null, Direction.right);

		findViewById(R.id.swipes_confirm).setOnClickListener(getOnClickEvent());
	}

	public void buildWidgetList()
	{
		AppGlobal.addAvailableMappings(widgets,
				new WidgetType[]{WidgetType.point_click,
						WidgetType.key,
						WidgetType.special,
						WidgetType.folder,
						WidgetType.combo
				},
				true);
	}

	private View.OnClickListener getOnClickEvent() {
		if (onClick == null) {
			onClick = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
						case R.id.swipes_confirm: {
							dismiss();
							if (event!=null) {
								event.onPick(swipes, widgets);
							}
							break;
						}
					}
				}
			};
		}

		return onClick;
	}

	private void setSwipeInfo(Button button, ImageViewerItem imageData, Direction direction)
	{
		SwipeItem item = swipes.getSwipeItem(direction);

		if ((item != null) && (item.getWidget() != null))
		{
			button.setText(item.getWidget().getText());

			ImageView img = null;

			switch (direction)
			{
				case up:
				{
					img = (ImageView)getView().findViewById(R.id.swipes_keyup_img);
					break;
				}
				case down:
				{
					img = (ImageView)getView().findViewById(R.id.swipes_keydown_img);
					break;
				}
				case left:
				{
					img = (ImageView)getView().findViewById(R.id.swipes_keyleft_img);
					break;
				}
				case right:
				{
					img = (ImageView)getView().findViewById(R.id.swipes_keyright_img);
					break;
				}
				default:
					break;
			}

			if (img != null)
			{
				if (imageData == null)
					imageData = uiImageViewer.getImageViewerItemFromWidget(item.getWidget());

				img.setImageBitmap(imageData.getImageBitmap());
				//img.setBackground(new BitmapDrawable(AppGlobal.context.getResources(), imageData.getBackgroundImageBitmap()));
				AppGlobal.setBackgroundDrawable(img, new BitmapDrawable(AppGlobal.context.getResources(), imageData.getBackgroundImageBitmap()));
			}

			CheckBox enabled = null;

			switch (direction)
			{
				case up:
				{
					enabled = (CheckBox)getView().findViewById(R.id.swipes_keyup_enabled);
					break;
				}
				case down:
				{
					enabled = (CheckBox)getView().findViewById(R.id.swipes_keydown_enabled);
					break;
				}
				case left:
				{
					enabled = (CheckBox)getView().findViewById(R.id.swipes_keyleft_enabled);
					break;
				}
				case right:
				{
					enabled = (CheckBox)getView().findViewById(R.id.swipes_keyright_enabled);
					break;
				}
				default:
					break;
			}

			if (enabled != null)
			{
				enabled.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						SwipeItem item = null;

						switch(v.getId())
						{
							case R.id.swipes_keyup_enabled:
							{
								item = swipes.getSwipeItem(Direction.up);
								break;
							}
							case R.id.swipes_keydown_enabled:
							{
								item = swipes.getSwipeItem(Direction.down);
								break;
							}
							case R.id.swipes_keyleft_enabled:
							{
								item = swipes.getSwipeItem(Direction.left);
								break;
							}
							case R.id.swipes_keyright_enabled:
							{
								item = swipes.getSwipeItem(Direction.right);
								break;
							}
						}

						if (item != null)
						{
							item.setEnabled(((CheckBox)v).isChecked());
						}
					}
				});

				enabled.setChecked(item.isEnabled());
			}
		}
	}

	private void setSwipeItem(Direction direction, Widget widget)
	{
		SwipeItem item = swipes.getSwipeItem(direction);

		if (item != null)
		{
			item.setWidget(widget);
		}
	}
}

interface LongpressGestureEventListener {
	void onPick(MouseButton button, boolean vibrate, boolean showInfo, String message, int timing);
}

class LongpressGestureSettings extends Dialog {
	private int minLongpressTiming = 400;//ms
	private int maxLongpressTiming = 1000;//ms

	private CheckBox vibrate;
	private CheckBox showInfo;
	private EditText infoText;
	private LongpressGestureEventListener event;
	private MouseButton mouseButton;
	private View.OnClickListener onClick;
	private TextView mouseButtonView;
	private int timingValue;
	private TextView longpressTimingValue;
	private SeekBar longpressTimingSeek;

	public void setOnLongpressGestureEventListener(LongpressGestureEventListener event) {
		this.event = event;
	}

	@Override
	public void onSetLocalizedLayout() {
		localize(R.id.longpress_notify, "common_notify");
		localize(R.id.longpress_vibrate, "longpress_vibrate");
		localize(R.id.longpress_showinfo, "longpress_showinfo");
		localize(R.id.longpress_others, "common_other");
		localize(R.id.longpress_button_title, "common_mousebutton");
		localize(R.id.longpress_timing_title, "longpress_timing_title");
	}

	public LongpressGestureSettings(MouseButton button, boolean vibrate, boolean showInfo, String message, int timing) {
		super(AppGlobal.context);

		setContentView(R.layout.gesture_longpress);
		setCaption("common_longpress");

		this.vibrate = (CheckBox)findViewById(R.id.longpress_vibrate);
		this.showInfo = (CheckBox)findViewById(R.id.longpress_showinfo);
		this.infoText = (EditText)findViewById(R.id.longpress_showinfo_text);
		mouseButtonView = (TextView)findViewById(R.id.longpress_button_value);

		this.vibrate.setChecked(vibrate);
		this.showInfo.setChecked(showInfo);
		this.infoText.setText(message);

		this.mouseButton = button;
		this.timingValue = timing;

		setMouseButtonView();
		longpressTimingValue = (TextView)findViewById(R.id.longpress_timing_value);
		longpressTimingValue.setText("" + timingValue);

		longpressTimingSeek = (SeekBar)findViewById(R.id.longpress_timingseek);
		longpressTimingSeek.setMax(maxLongpressTiming - minLongpressTiming);
		longpressTimingSeek.setProgress(timingValue - minLongpressTiming);

		longpressTimingSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				timingValue = progress + minLongpressTiming;
				longpressTimingValue.setText("" + timingValue);
			}
		});

		findViewById(R.id.longpress_button_minus).setOnClickListener(getOnClick());
		findViewById(R.id.longpress_button_plus).setOnClickListener(getOnClick());
		findViewById(R.id.longpress_timing_minus).setOnClickListener(getOnClick());
		findViewById(R.id.longpress_timing_plus).setOnClickListener(getOnClick());
		findViewById(R.id.longpress_confirm).setOnClickListener(getOnClick());
	}

	private View.OnClickListener getOnClick() {
		if (onClick == null) {
			onClick =  new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
						case R.id.longpress_confirm: {
							dismiss();
							if (event != null) {
								event.onPick(mouseButton, vibrate.isChecked(), showInfo.isChecked(), infoText.getText().toString(), timingValue);
							}
							break;
						}
						case R.id.longpress_button_minus: {
							if (mouseButton == MouseButton.left) {
								break;
							}

							if (mouseButton == MouseButton.right) {
								mouseButton = MouseButton.middle;
							} else {
								mouseButton = MouseButton.left;
							}
							setMouseButtonView();
							break;
						}
						case R.id.longpress_button_plus: {
							if (mouseButton == MouseButton.right) {
								break;
							}

							if (mouseButton == MouseButton.left) {
								mouseButton = MouseButton.middle;
							} else {
								mouseButton = MouseButton.right;
							}
							setMouseButtonView();
							break;
						}
						case R.id.longpress_timing_minus:
						{
							if (timingValue == minLongpressTiming)
								return;

							timingValue-=50;
							longpressTimingSeek.setProgress(timingValue - minLongpressTiming);
							longpressTimingValue.setText("" + timingValue);
							break;
						}
						case R.id.longpress_timing_plus:
						{
							if (timingValue == maxLongpressTiming)
								return;

							timingValue+=50;
							longpressTimingSeek.setProgress(timingValue - minLongpressTiming);
							longpressTimingValue.setText("" + timingValue);
							break;
						}
					}
				}
			};
		}

		return  onClick;
	}
	private void setMouseButtonView() {
		switch (mouseButton)
		{
			case left:
			{
				mouseButtonView.setText(Localization.getString("mouse_button_sleft"));
				break;
			}
			case middle:
			{
				mouseButtonView.setText(Localization.getString("mouse_button_smiddle"));
				break;
			}
			case right:
			{
				mouseButtonView.setText(Localization.getString("mouse_button_sright"));
				break;
			}
			default:
				break;
		}
	}
}

interface MouseGestureEventListener {
	void onPick(MouseButton button, boolean keepMouseDown);
}

class MouseGestureSettings extends Dialog {
	private View.OnClickListener onClick;
	private TextView mouseButtonView;
	private MouseButton mouseButton;
	private MouseGestureEventListener event;
	private CheckBox keepMouseDownView;

	@Override
	public void onSetLocalizedLayout() {
		localize(R.id.mouse_button_title, "common_mousebutton");
		localize(R.id.mouse_button_keepdown, "twopoint_onlymousedown");
	}

	public void setOnMouseGestureEventListener(MouseGestureEventListener event) {
		this.event = event;
	}

	public MouseGestureSettings(MouseButton button, boolean showKeepMouseDown, boolean keepMouseDown) {
		super(AppGlobal.context);

		setContentView(R.layout.gesture_mouse);
		setCaption("common_mouse");

		this.mouseButton = button;

		keepMouseDownView = (CheckBox)findViewById(R.id.mouse_button_keepdown);
		if (!showKeepMouseDown) {
			keepMouseDownView.setVisibility(View.GONE);
		} else {
			keepMouseDownView.setChecked(keepMouseDown);
		}

		mouseButtonView = (TextView)findViewById(R.id.mouse_button_value);
		findViewById(R.id.mouse_button_minus).setOnClickListener(getOnClick());
		findViewById(R.id.mouse_button_plus).setOnClickListener(getOnClick());
		findViewById(R.id.mouse_confirm).setOnClickListener(getOnClick());

		setMouseButtonView();
	}

	private View.OnClickListener getOnClick() {
		if (onClick == null) {
			onClick =  new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
						case R.id.mouse_confirm: {
							dismiss();
							if (event != null) {
								event.onPick(mouseButton, keepMouseDownView.isChecked());
							}
							break;
						}
						case R.id.mouse_button_minus: {
							if (mouseButton == MouseButton.left) {
								break;
							}

							if (mouseButton == MouseButton.right) {
								mouseButton = MouseButton.middle;
							} else {
								mouseButton = MouseButton.left;
							}
							setMouseButtonView();
							break;
						}
						case R.id.mouse_button_plus: {
							if (mouseButton == MouseButton.right) {
								break;
							}

							if (mouseButton == MouseButton.left) {
								mouseButton = MouseButton.middle;
							} else {
								mouseButton = MouseButton.right;
							}
							setMouseButtonView();
							break;
						}
					}
				}
			};
		}

		return  onClick;
	}

	private void setMouseButtonView() {
		switch (mouseButton)
		{
			case left:
			{
				mouseButtonView.setText(Localization.getString("mouse_button_sleft"));
				break;
			}
			case middle:
			{
				mouseButtonView.setText(Localization.getString("mouse_button_smiddle"));
				break;
			}
			case right:
			{
				mouseButtonView.setText(Localization.getString("mouse_button_sright"));
				break;
			}
			default:
				break;
		}
	}
}

