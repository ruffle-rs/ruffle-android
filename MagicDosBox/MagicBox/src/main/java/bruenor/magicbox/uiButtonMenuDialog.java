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
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import magiclib.Global;
import magiclib.IO.FileBrowser;
import magiclib.IO.FileBrowserCopyFile;
import magiclib.IO.FileBrowserItem;
import magiclib.IO.Files;
import magiclib.IO.Storages;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.Align;
import magiclib.core.ColorPicker;
import magiclib.core.ColorPickerItem;
import magiclib.core.Direction;
import magiclib.core.EmuConfig;
import magiclib.core.EmuManager;
import magiclib.core.Screen;
import magiclib.fonts.ExternalFonts;
import magiclib.gui_modes.BoundaryMode;
import magiclib.gui_modes.FindPointMode;
import magiclib.gui_modes.WidgetConfigurationDialog;
import magiclib.keyboard.Key;
import magiclib.keyboard.KeyAction;
import magiclib.keyboard.KeyCodeInfo;
import magiclib.layout.widgets.CloseParentFolderComboAction;
import magiclib.layout.widgets.Combo;
import magiclib.layout.widgets.ComboAction;
import magiclib.layout.widgets.ComboActionType;
import magiclib.layout.widgets.DelayComboAction;
import magiclib.layout.widgets.DpadType;
import magiclib.layout.widgets.EFolderComboAction;
import magiclib.layout.widgets.EMouseToggleComboAction;
import magiclib.layout.widgets.ENonLayoutComboAction;
import magiclib.layout.widgets.Folder;
import magiclib.layout.widgets.FolderComboAction;
import magiclib.layout.widgets.FolderLayout;
import magiclib.layout.widgets.Journal;
import magiclib.layout.widgets.JoystickButton;
import magiclib.layout.widgets.KeyComboAction;
import magiclib.layout.widgets.KeyWidget;
import magiclib.layout.widgets.MouseNavigationComboAction;
import magiclib.layout.widgets.MouseToggleComboAction;
import magiclib.layout.widgets.MouseTypeAction;
import magiclib.layout.widgets.NonLayoutComboAction;
import magiclib.layout.widgets.PointClick;
import magiclib.layout.widgets.PointClickAction;
import magiclib.layout.widgets.SpecialAction;
import magiclib.layout.widgets.SpecialComboAction;
import magiclib.layout.widgets.TargetComboAction;
import magiclib.layout.widgets.TouchMouseAction;
import magiclib.layout.widgets.VirtualDpad;
import magiclib.layout.widgets.Walkthrough;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetSwitcher;
import magiclib.layout.widgets.WidgetType;
import magiclib.layout.widgets.ZoomWidget;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;
import magiclib.mouse.MouseButton;

class uiButtonMenuDialog extends WidgetConfigurationDialog
{
	private GradientDrawable gradBgrColor;
	private ImageView backgroundColor;

	private uiButtonMenuDialog self;
	private WidgetType currentType;
	private boolean runAtStart;

	private ScrollView mainScroll;
	private LinearLayout key_layout;
	private LinearLayout mouse_layout;
	private LinearLayout mousetype_layout;
	private LinearLayout dpad_main_layout;
	private LinearLayout dpad_classic_layout;
	private LinearLayout dpad_mouse_layout;
	private LinearLayout dpad_2axis_layout;
	private LinearLayout folder_layout;
	private LinearLayout journal_layout;
	private LinearLayout walkthrough_layout;
	private LinearLayout pointClick_layout;
	private LinearLayout specialAction_layout;
	private LinearLayout mousemove_layout;
	private LinearLayout combo_layout;
	private LinearLayout switcher_layout;
	private LinearLayout joybutton_layout;
	private LinearLayout zoom_layout;

	private boolean deleteButton = false;

	class ComboActionAdapter
	{
		public List<ComboAction> items;

		private LinearLayout root;
		private List<RelativeLayout> views;
		private View.OnClickListener onClick1Event;
		private View.OnClickListener onClick2Event;

		public ComboActionAdapter(LinearLayout root, List<ComboAction> items)
		{
			this.root = root;
			this.items = items;

			views = new ArrayList<RelativeLayout>();

			if (items.size() > 0)
			{
				for (ComboAction item : items)
				{
					addItem(item, false);
				}
			}
		}

		public void clear()
		{
			if (items != null)
				items.clear();

			if (views != null)
				views.clear();
		}

		public void updateItem(ComboAction item)
		{
			int position = items.indexOf(item);
			View v = views.get(position);

			if (item != null)
			{
				TextView text = (TextView) v.findViewById(R.id.combo_item_index);

				ImageView comboImage = (ImageView) v.findViewById(R.id.combo_item_image);
				comboImage.setImageResource(item.getIconID());

				ImageView widgetImage = (ImageView) v.findViewById(R.id.combo_item_widgetimage);
				TextView widgetText = (TextView) v.findViewById(R.id.combo_item_widgettext);
				ImageView widgetSettings = (ImageView)v.findViewById(R.id.combo_item_widgetsettings);

				if (text != null)
				{
					text.setText(Integer.toString(position));
				}

				text = (TextView) v.findViewById(R.id.combo_item_label);
				text.setText(item.getTitle());

				text = (TextView) v.findViewById(R.id.combo_item_value);
				text.setText(item.getText());

				ImageViewerItem imgData = item.getImageViewerItem();

				if (imgData != null)
				{
					widgetImage.setVisibility(View.VISIBLE);
					widgetText.setVisibility(View.VISIBLE);

					if (item.getType() == ComboActionType.send_key || item.getType() == ComboActionType.target)
					{
						int padding = AppGlobal.DensityToPixels(10);
						widgetImage.setPadding(padding, padding, padding, padding);

						widgetSettings.setVisibility(View.GONE);

						if (item.getType() == ComboActionType.target) {
							if (((TargetComboAction)item).action == PointClickAction.up) {
								widgetImage.setVisibility(View.GONE);
							} else {
								widgetImage.setVisibility(View.VISIBLE);
							}
						}
					}
					else
					{
						widgetSettings.setVisibility(View.VISIBLE);
					}

					widgetImage.setImageBitmap(imgData.getImageBitmap());
                    //widgetImage.setBackground(new BitmapDrawable(AppGlobal.context.getResources(), imgData.getBackgroundImageBitmap()));
					AppGlobal.setBackgroundDrawable(widgetImage, new BitmapDrawable(AppGlobal.context.getResources(), imgData.getBackgroundImageBitmap()));

                    widgetText.setText(imgData.getDescription());
				}
				else
				{
					widgetImage.setVisibility(View.GONE);
					widgetText.setVisibility(View.GONE);
					widgetSettings.setVisibility(View.GONE);
				}
			}
		}

		public void removeItem(int position)
		{
			View v = views.get(position);
			root.removeView(v);

			items.remove(position);
			views.remove(v);
		}

		public ComboAction findItemByView(View v)
		{
			int position = views.indexOf(v);

			if (position == -1)
				return null;

			return items.get(position);
		}

		public void addItem(ComboAction item, boolean newItem)
		{
			RelativeLayout view = (RelativeLayout) getLayoutInflater().inflate(R.layout.combo_item, null);

			if (newItem)
			{
				items.add(item);
			}

			views.add(view);
			root.addView(view);

			RelativeLayout click2Panel = (RelativeLayout)view.findViewById(R.id.combo_click2_panel);
			click2Panel.setTag(view);
			click2Panel.setOnClickListener(getOnClick2Event());

			LinearLayout click1Panel = (LinearLayout)view.findViewById(R.id.combo_click1_panel);
			click1Panel.setTag(view);
			click1Panel.setOnClickListener(getOnClick1Event());

			click1Panel.setOnLongClickListener(new View.OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View v)
				{
					removeItem(views.indexOf(v.getTag()));

					for (ComboAction item : items)
					{
						updateItem(item);
					}

					return true;
				}
			});

			updateItem(item);
		}

		private View.OnClickListener getOnClick2Event()
		{
			if (onClick2Event == null)
			{
				onClick2Event = new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						v = (View)v.getTag();

						ComboAction item = caAdapter.findItemByView(v);

						if (item.getType() == ComboActionType.send_key)
						{
							setKeyComboSettings((KeyComboAction)item);
							return;
						}

						if (item.getType() == ComboActionType.folder)
						{
							setFolderSettings((FolderComboAction)item);
							return;
						}

						if (item.getType() == ComboActionType.nonlayout)
						{
							setNonLayoutSettings((NonLayoutComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.target)
						{
							final TargetComboAction targetAction = (TargetComboAction)item;

							if (targetAction.action == PointClickAction.up)
								return;

							FindPointMode.start(temp_combo, targetAction.x, targetAction.y, new FindPointMode.OnFindPointModeListener() {
								@Override
								public void onFinish(float x, float y) {
									targetAction.x = x;
									targetAction.y = y;
									caAdapter.updateItem(targetAction);
								}
							});
							return;
						}
					}
				};
			}

			return onClick2Event;
		}

		private void setKeyComboSettings(final KeyComboAction keyAction)
		{
			final Key key = keyAction.getKey();

			KeySettings settings = new KeySettings(getContext(), key, keyAction.action, true, true);
			settings.setKeySettingsEventListener(new KeySettingsEventListener()
			{
				@Override
				public void onChange(boolean ctrl, boolean alt, boolean shift, KeyAction action)
				{
					key.ctrl = ctrl;
					key.alt = alt;

					keyAction.action = action;

					if (key.shift != shift)
					{
						key.shift = shift;
						caAdapter.updateItem(keyAction);
					}
				}
			});
			settings.show();
		}

		private void setKeyComboAction(final KeyComboAction action)
		{
			uiKeyCodesDialog d = new uiKeyCodesDialog(context);
			d.setOnKeyCodeListener(new KeyCodeListener() {
				@Override
				public void onPick(KeyCodeItem selected) {
					action.getKey().setKeyCode(selected.getKeyCode());

					caAdapter.updateItem(action);
				}
			});

			d.show();
		}

		private void setDelayComboAction(final DelayComboAction action)
		{
			uiComboDelayEdit d = new uiComboDelayEdit(context, action.getDelay());
			d.setOnDelayEventListener(new DelayEventListener() {
				@Override
				public void onPick(int value) {
					action.setDelay(value);

					caAdapter.updateItem(action);
				}
			});

			d.show();
		}

		private void setFolderSettings(final FolderComboAction action)
		{
			uiComboFolderEdit d = new uiComboFolderEdit(context, action.getWidget(), "widget_edit_combo_bag_action");
			d.setCaption("widget_edit_combo_bag_caption");
			d.setRadioItemLabel(0, getLocaleString("widget_edit_combo_bag_open"));
			d.setRadioItemLabel(1, getLocaleString("widget_edit_combo_bag_close"));
			d.setRadioItemLabel(2, getLocaleString("widget_edit_combo_bag_switch"));
			d.setOnComboFolderEditEventListener(new ComboFolderEditEventListener() {
				@Override
				public void onPick(Widget widget, int actionIndex) {
					action.setWidget(widget);

					switch (actionIndex) {
						case 0: {
							action.setAction(EFolderComboAction.open_folder);
							break;
						}
						case 1: {
							action.setAction(EFolderComboAction.close_folder);
							break;
						}
						case 2: {
							action.setAction(EFolderComboAction.open_or_close_folder);
							break;
						}
					}

					caAdapter.updateItem(action);
				}
			});

			switch (action.getAction())
			{
				case open_folder:
				{
					d.setSelectedRadioItem(0);
					break;
				}
				case close_folder:
				{
					d.setSelectedRadioItem(1);
					break;
				}
				case open_or_close_folder:
				{
					d.setSelectedRadioItem(2);
					break;
				}
			}

			d.show();
		}

		private void setFolderAction(final FolderComboAction action)
		{
			final uiImageViewer viewer = new uiImageViewer(getContext());

			if (viewer.loadFoldersList())
			{
				viewer.setCaption("common_bags");
                viewer.useItemBackground = true;
				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener()
				{
					@Override
					public void onPick(ImageViewerItem selected)
					{
						action.setWidget((Widget)selected.getTag());
						caAdapter.updateItem(action);
					}
				});

				viewer.setOnDismissListener(new OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						show();
					}
				});

				viewer.show();
			}
			else
			{
				MessageInfo.info("msg_no_bags");
				viewer.dismiss();
			}
		}

		private void setNonLayoutSettings(final NonLayoutComboAction action)
		{
			uiComboFolderEdit d = new uiComboFolderEdit(context, action.getWidget(), "widget_edit_combo_gp_action");
			d.setCaption("widget_edit_combo_gp_caption");
			d.setRadioItemLabel(0, getLocaleString("widget_edit_combo_gp_vanish"));
			d.setRadioItemLabel(1, getLocaleString("widget_edit_combo_gp_materialize"));
			d.setRadioItemLabel(2, getLocaleString("widget_edit_combo_gp_switch"));
			d.setOnComboFolderEditEventListener(new ComboFolderEditEventListener()
			{
				@Override
				public void onPick(Widget widget, int actionIndex)
				{
					action.setWidget(widget);

					switch (actionIndex)
					{
						case 0:
						{
							action.setAction(ENonLayoutComboAction.set_nonlayout);
							break;
						}
						case 1:
						{
							action.setAction(ENonLayoutComboAction.unset_nonlayout);
							break;
						}
						case 2:
						{
							action.setAction(ENonLayoutComboAction.set_or_unset_nonlayout);
							break;
						}
					}

					caAdapter.updateItem(action);
				}
			});

			switch (action.getAction())
			{
				case set_nonlayout:
				{
					d.setSelectedRadioItem(0);
					break;
				}
				case unset_nonlayout:
				{
					d.setSelectedRadioItem(1);
					break;
				}
				case set_or_unset_nonlayout:
				{
					d.setSelectedRadioItem(2);
					break;
				}
			}

			d.show();
		}

		private void setNonLayoutAction(final NonLayoutComboAction action)
		{
			final uiImageViewer viewer = new uiImageViewer(getContext());

			viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
				@Override
				public boolean onSet(List images) {
					AppGlobal.addAvailableMappings(images, new WidgetType[]{}, true);

					if (images.size() == 0) {
						MessageInfo.info("msg_no_widgets");
						return false;
					}

					return true;
				}
			});

			viewer.setCaption("common_widgets");
            viewer.useItemBackground = true;
			viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
				@Override
				public void onPick(ImageViewerItem selected) {
					action.setWidget((Widget) selected.getTag());
					caAdapter.updateItem(action);
				}
			});

			viewer.show();
		}

		private void setMouseToggleAction(final MouseToggleComboAction action)
		{
			uiComboFolderEdit d = new uiComboFolderEdit(context, null, "widget_edit_combo_mbt_action");
			d.setCaption("widget_edit_combo_mbt_caption");
			d.setRadioItemLabel(0, getLocaleString("widget_edit_combo_mbt_left"));
			d.setRadioItemLabel(1, getLocaleString("widget_edit_combo_mbt_right"));
			d.setRadioItemLabel(2, getLocaleString("widget_edit_combo_mbt_switch"));
			d.setOnComboFolderEditEventListener(new ComboFolderEditEventListener() {
				@Override
				public void onPick(Widget widget, int actionIndex) {
					switch (actionIndex) {
						case 0: {
							action.setAction(EMouseToggleComboAction.mouse_left);
							break;
						}
						case 1: {
							action.setAction(EMouseToggleComboAction.mouse_right);
							break;
						}
						case 2: {
							action.setAction(EMouseToggleComboAction.mouse_toggle);
							break;
						}
					}

					caAdapter.updateItem(action);
				}
			});

			switch (action.getAction())
			{
				case mouse_left:
				{
					d.setSelectedRadioItem(0);
					break;
				}
				case mouse_right:
				{
					d.setSelectedRadioItem(1);
					break;
				}
				case mouse_toggle:
				{
					d.setSelectedRadioItem(2);
					break;
				}
			}

			d.show();
		}

		private void setNavigationToggleAction(final MouseNavigationComboAction action)
		{
			uiComboFolderEdit d = new uiComboFolderEdit(context, null, "common_mouse");
			d.setCaption("widget_edit_combo_menu_nav");
			d.setRadioItemLabel(0, getLocaleString("mouse_type_absolute"));
			d.setRadioItemLabel(1, getLocaleString("mouse_type_relative"));
			d.setRadioItemLabel(2, getLocaleString("widget_edit_combo_mbt_switch"));
			d.setOnComboFolderEditEventListener(new ComboFolderEditEventListener() {
				@Override
				public void onPick(Widget widget, int actionIndex) {
					switch (actionIndex) {
						case 0: {
							action.setType(0);
							break;
						}
						case 1: {
							action.setType(1);
							break;
						}
						case 2: {
							action.setType(2);
							break;
						}
					}

					caAdapter.updateItem(action);
				}
			});

			switch (action.type)
			{
				case 0:
				{
					d.setSelectedRadioItem(0);
					break;
				}
				case 1:
				{
					d.setSelectedRadioItem(1);
					break;
				}
				case 2:
				{
					d.setSelectedRadioItem(2);
					break;
				}
			}

			d.show();
		}

		private void setSpecialAction(final SpecialComboAction action)
		{
			ComboSpecialActionDialog d = new ComboSpecialActionDialog(context, action);
			d.setOnComboSpecialActionEventListener(new ComboSpecialActionEventListener() {
				@Override
				public void onPick(SpecialAction.Action specialAction) {
					action.setAction(specialAction);
					caAdapter.updateItem(action);
				}
			});
			d.show();
		}

		private void setTargetSettings(final TargetComboAction action)
		{
			uiComboTargetEdit d = new uiComboTargetEdit(context, action, new uiComboTargetEdit.ConfirmEventListener() {
				@Override
				public void onConfirm(PointClickAction a, MouseButton b) {
					action.action = a;
					action.mouseButton = b;
					caAdapter.updateItem(action);
				}
			});
			d.show();
		}

		private View.OnClickListener getOnClick1Event()
		{
			if (onClick1Event == null)
			{
				onClick1Event = new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						v = (View)v.getTag();

						final ComboAction item = caAdapter.findItemByView(v);

						if (item.getType() == ComboActionType.send_key)
						{
							setKeyComboAction((KeyComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.delay)
						{
							setDelayComboAction((DelayComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.folder)
						{
							setFolderAction((FolderComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.nonlayout)
						{
							setNonLayoutAction((NonLayoutComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.mouse_toggle)
						{
							setMouseToggleAction((MouseToggleComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.special)
						{
							setSpecialAction((SpecialComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.target)
						{
							setTargetSettings((TargetComboAction) item);
							return;
						}

						if (item.getType() == ComboActionType.mouse_navigation)
						{
							setNavigationToggleAction((MouseNavigationComboAction) item);
							return;
						}
					}
				};
			}

			return onClick1Event;
		}
	}

	private Context context;
	private LayoutInflater inflater;

	//temp vars
	private Key temp_key1;
	private Key temp_key2;
	private int temp_multiTapDelay;
	private boolean temp_key_toggle;
	private TouchMouseAction temp_touchAction;
	private MouseTypeAction temp_mouseTypeAction;
	private VirtualDpad temp_dpad;
	private Folder temp_folder;
	private Journal temp_journal;
	private Walkthrough temp_walkthrough;
	private Combo temp_combo;
	private PointClick temp_pointclick;
	public SpecialAction temp_specialAction;
	public WidgetSwitcher temp_switcher;
	public JoystickButton temp_joybutton;
	public ZoomWidget temp_zoomWidget;

	private String temp_title;
	private int temp_textColor;
	private Align temp_textAlign;
	private int temp_textOpacity;
	private boolean temp_textAntialiasing;
	private String temp_textFont;
	private String temp_bitmapName;
	private String temp_bgrBitmapName;
	private int temp_opacity;
	private int temp_bitmapOpacity = 255;
	private int temp_bgrBitmapOpacity;
	private boolean temp_isVisible;
	private boolean temp_isUndetectable;
	private boolean temp_deactOnLeave;
	private boolean temp_synfeed;
	private boolean temp_isTappableOnly;
	private int temp_backgroundColor;
	private boolean temp_bitmapEnabled;
	private boolean temp_textEnabled;
	private boolean temp_bgrColorEnabled;
	private boolean temp_bgrBitmapEnabled;

	@Override
	protected void onStop()
	{
		button = null;

		if ((temp_combo != null) && (temp_combo.actions != null))
		{
			temp_combo.actions.clear();
		}

		if (caAdapter != null)
		{
			caAdapter.clear();
		}

		super.onStop();
	}

	private void loadKeySettings()
	{
		setCaption("widget_edit_key_caption");

		currentType = WidgetType.key;

		KeyWidget key = (KeyWidget) button;

		if (temp_key1 == null)
		{
			temp_key1 = new Key();

			key.getDesignKey(0).copyTo(temp_key1);
		}

		if (temp_key2 == null)
		{
			temp_key2 = new Key();

			key.getDesignKey(1).copyTo(temp_key2);
		}

		temp_multiTapDelay = key.getMultiTapDelay();
		temp_key_toggle = key.isToggle();

		if (key_layout == null)
		{
			key_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_key, null);

			localize(key_layout, R.id.button_menu_runwidgetatstartup, "common_runatstart");
			localize(key_layout, R.id.button_menu_key_toggle, "common_toggle");

			localize(key_layout, R.id.button_menu_key_singletap,  "widget_edit_key_singletap_title");
			localize(key_layout, R.id.button_menu_key_enabled1,   "common_enabled");
			localize(key_layout, R.id.button_menu_keycode1_title, "widget_edit_key_keycode_title");
			localize(key_layout, R.id.button_menu_key_ctrl1,      "common_ctrl");
			localize(key_layout, R.id.button_menu_key_alt1,       "common_alt");
			localize(key_layout, R.id.button_menu_key_shift1,     "common_shift");

			localize(key_layout, R.id.button_menu_key_doubletap,  "widget_edit_key_doubletap_title");
			localize(key_layout, R.id.button_menu_key_enabled2,   "common_enabled");
			localize(key_layout, R.id.button_menu_keycode2_title, "widget_edit_key_keycode_title");
			localize(key_layout, R.id.button_menu_key_ctrl2,      "common_ctrl");
			localize(key_layout, R.id.button_menu_key_alt2,       "common_alt");
			localize(key_layout, R.id.button_menu_key_shift2,     "common_shift");

			localize(key_layout, R.id.button_menu_key_timing_title,   "widget_edit_key_timing_title");
			localize(key_layout, R.id.button_menu_key_timing_mstitle, "widget_edit_key_timing_mstitle");

			View.OnClickListener keyBoolEvent = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (v.getId() == R.id.button_menu_key_toggle)
					{
						temp_key_toggle = !temp_key_toggle;
						return;
					}

					if (v.getId() == R.id.button_menu_key_enabled1)
					{
						temp_key1.setEnabled(!temp_key1.isEnabled());
						return;
					}

					if (v.getId() == R.id.button_menu_key_enabled2)
					{
						temp_key2.setEnabled(!temp_key2.isEnabled());
						return;
					}

					if (v.getId() == R.id.button_menu_key_ctrl1)
					{
						temp_key1.setCtrl(!temp_key1.isCtrl());
						return;
					}

					if (v.getId() == R.id.button_menu_key_ctrl2)
					{
						temp_key2.setCtrl(!temp_key2.isCtrl());
						return;
					}

					if (v.getId() == R.id.button_menu_key_alt1)
					{
						temp_key1.setAlt(!temp_key1.isAlt());
						return;
					}

					if (v.getId() == R.id.button_menu_key_alt2)
					{
						temp_key2.setAlt(!temp_key2.isAlt());
						return;
					}

					if (v.getId() == R.id.button_menu_key_shift1)
					{
						temp_key1.setShift(!temp_key1.isShift());

						if (temp_key1.keyCode != -1) {
							Button b = (Button) key_layout.findViewById(R.id.button_menu_key_button1);
							b.setText(KeyCodeInfo.getDosboxKeyInfo(temp_key1.keyCode, temp_key1.shift));
						}
						return;
					}

					if (v.getId() == R.id.button_menu_key_shift2)
					{
						temp_key2.setShift(!temp_key2.isShift());

						if (temp_key2.keyCode != -1) {
							Button b = (Button) key_layout.findViewById(R.id.button_menu_key_button2);
							b.setText(KeyCodeInfo.getDosboxKeyInfo(temp_key2.keyCode, temp_key2.shift));
						}
						return;
					}

					if (v.getId() == R.id.button_menu_runwidgetatstartup)
					{
						runAtStart=!runAtStart;
					}
				}
			};

			CheckBox cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_enabled1);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key1.isEnabled());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_enabled2);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key2.isEnabled());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_toggle);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key_toggle);

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_ctrl1);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key1.isCtrl());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_ctrl2);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key2.isCtrl());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_alt1);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key1.isAlt());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_alt2);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key2.isAlt());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_shift1);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key1.isShift());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_key_shift2);
			cbx.setOnClickListener(keyBoolEvent);
			cbx.setChecked(temp_key2.isShift());

			cbx = (CheckBox) key_layout.findViewById(R.id.button_menu_runwidgetatstartup);
			cbx.setOnClickListener(keyBoolEvent);
			runAtStart = EmuConfig.startupWidgetEnabled && EmuConfig.startupWidgetID.equals(button.getName());
			cbx.setChecked(runAtStart);

			View.OnClickListener onKey_getKey = new View.OnClickListener()
			{
				@Override
				public void onClick(final View v)
				{
					uiKeyCodesDialog d = new uiKeyCodesDialog(context);
					d.setOnKeyCodeListener(new KeyCodeListener()
					{
						@Override
						public void onPick(KeyCodeItem selected)
						{
							Key key;

							if (v.getId() == R.id.button_menu_key_button1)
							{
								key = temp_key1;
							} else
							{
								key = temp_key2;
							}

							key.setKeyCode(selected.getKeyCode());
							//key.setText(selected.getText());

							((Button)v).setText(KeyCodeInfo.getDosboxKeyInfo(key.keyCode, key.shift));
						}
					});

					d.show();
				}
			};

			Button b = (Button) key_layout.findViewById(R.id.button_menu_key_button1);
			b.setText((temp_key1.keyCode==-1)?"":KeyCodeInfo.getDosboxKeyInfo(temp_key1.keyCode, temp_key1.shift));
			b.setOnClickListener(onKey_getKey);

			b = (Button) key_layout.findViewById(R.id.button_menu_key_button2);
			b.setText((temp_key2.keyCode==-1)?"":KeyCodeInfo.getDosboxKeyInfo(temp_key2.keyCode, temp_key2.shift));
			b.setOnClickListener(onKey_getKey);

			final TextView mltValue = (TextView)key_layout.findViewById(R.id.button_menu_key_timing_value);
			mltValue.setText("" + temp_multiTapDelay);

			final SeekBar mltSeek = (SeekBar)key_layout.findViewById(R.id.button_menu_key_timingseek);
			final int minValue = 200;//ms
			final int maxValue = 1500;//ms

			mltSeek.setMax(maxValue - minValue);
			mltSeek.setProgress(temp_multiTapDelay - minValue);

			mltSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
			{
				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{
					// TODO Auto-generated method stub				
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{
					// TODO Auto-generated method stub				
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
				{
					temp_multiTapDelay = progress + minValue;
					mltValue.setText("" + temp_multiTapDelay);
				}
			});

			View.OnClickListener buttonClick = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					switch (v.getId())
					{
					case R.id.button_menu_key_timing_minus:
					{
						if (temp_multiTapDelay == minValue)
							return;

						temp_multiTapDelay-=50;
						break;
					}
					case R.id.button_menu_key_timing_plus:
					{
						if (temp_multiTapDelay == maxValue)
							return;

						temp_multiTapDelay+=50;
						break;
					}
					}

					mltSeek.setProgress(temp_multiTapDelay - minValue);
					mltValue.setText("" + temp_multiTapDelay);
				}
			};

			ImageButton zoom = (ImageButton)key_layout.findViewById(R.id.button_menu_key_timing_minus);
			zoom.setOnClickListener(buttonClick);

			zoom = (ImageButton)key_layout.findViewById(R.id.button_menu_key_timing_plus);
			zoom.setOnClickListener(buttonClick);
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(key_layout);
	}

	private void loadTouchSettings()
	{
		setCaption("widget_edit_mousetouch_caption");

		currentType = WidgetType.touch_action;

		if (temp_touchAction == null)
		{
			temp_touchAction = new TouchMouseAction();

			TouchMouseAction ta = (TouchMouseAction) button;

			ta.copyTo(temp_touchAction);
		}

		if (mouse_layout == null)
		{
			mouse_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_mouse, null);

			localize(mouse_layout, R.id.button_menu_mouse_duration_title,                 "widget_edit_mousetouch_duration_title");
			localize(mouse_layout, R.id.button_menu_mouse_radioduration_permanent,        "widget_edit_mousetouch_duration_permanent");
			localize(mouse_layout, R.id.button_menu_mouse_radioduration_temporary,        "widget_edit_mousetouch_duration_temporary");
			localize(mouse_layout, R.id.button_menu_mouse_radioduration_click,            "widget_edit_mousetouch_duration_click");
			localize(mouse_layout, R.id.button_menu_mouse_action_title,                   "widget_edit_mousetouch_action_title");
			localize(mouse_layout, R.id.button_menu_mouse_radioaction_leftclick,          "mouse_button_lleft");
			localize(mouse_layout, R.id.button_menu_mouse_radioaction_rightclick,         "mouse_button_lright");
			localize(mouse_layout, R.id.button_menu_mouse_radioaction_middleclick,        "mouse_button_lmiddle");
			localize(mouse_layout, R.id.button_menu_mouse_radioaction_leftplusrightclick, "widget_edit_mousetouch_action_lrbutton");
			localize(mouse_layout, R.id.button_menu_mouse_radioaction_movemouse,          "widget_edit_mousetouch_action_move");
			localize(mouse_layout, R.id.button_menu_mouse_doubleclick,                    "widget_edit_mousetouch_action_dblclick");
			localize(mouse_layout, R.id.button_menu_mouse_ignorenextup,                   "widget_edit_mousetouch_ignore");

			CompoundButton.OnCheckedChangeListener cbxListener = new CompoundButton.OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					switch (buttonView.getId())
					{
					case R.id.button_menu_mouse_ignorenextup:
					{
						temp_touchAction.disableEverySecondMouseUp = isChecked;
						break;
					}
					case R.id.button_menu_mouse_doubleclick:
					{
						temp_touchAction.doubleClick = isChecked;
						break;
					}
					}
				}
			};

			//disable every second mouse up
			final CheckBox mouseUpDisable = (CheckBox) mouse_layout.findViewById(R.id.button_menu_mouse_ignorenextup);
			mouseUpDisable.setOnCheckedChangeListener(cbxListener);
			mouseUpDisable.setChecked(temp_touchAction.disableEverySecondMouseUp);

			//double click
			final CheckBox mouseDoubleClick = (CheckBox) mouse_layout.findViewById(R.id.button_menu_mouse_doubleclick);
			mouseDoubleClick.setOnCheckedChangeListener(cbxListener);
			mouseDoubleClick.setChecked(temp_touchAction.doubleClick);

			OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
				{
					switch (radioGroup.getId())
					{
					case R.id.button_menu_mouse_radioduration:
					{
						RadioGroup rdg = (RadioGroup) mouse_layout.findViewById(R.id.button_menu_mouse_radioaction);

						switch (checkedId)
						{
						case R.id.button_menu_mouse_radioduration_permanent:
						{
							temp_touchAction.touch_action_duration = 0;

							((RadioButton) rdg.getChildAt(4)).setText(getLocaleString("widget_edit_mousetouch_action_move"));
							mouseUpDisable.setVisibility(View.GONE);
							mouseDoubleClick.setVisibility(View.GONE);
							break;
						}
						case R.id.button_menu_mouse_radioduration_temporary:
						{
							temp_touchAction.touch_action_duration = 1;

							((RadioButton) rdg.getChildAt(4)).setText(getLocaleString("widget_edit_mousetouch_action_move"));
							mouseUpDisable.setVisibility(View.VISIBLE);
							mouseDoubleClick.setVisibility(View.VISIBLE);
							break;
						}
						case R.id.button_menu_mouse_radioduration_click:
						{
							temp_touchAction.touch_action_duration = 2;

							((RadioButton) rdg.getChildAt(4)).setText(getLocaleString("widget_edit_mousetouch_bysystem"));
							mouseUpDisable.setVisibility(View.GONE);
							mouseDoubleClick.setVisibility(View.VISIBLE);
							break;
						}
						}
						break;
					}
					case R.id.button_menu_mouse_radioaction:
					{
						switch (checkedId)
						{
						case R.id.button_menu_mouse_radioaction_leftclick:
						{
							temp_touchAction.touch_action_button = MouseButton.left;
							break;
						}
						case R.id.button_menu_mouse_radioaction_rightclick:
						{
							temp_touchAction.touch_action_button = MouseButton.right;
							break;
						}
						case R.id.button_menu_mouse_radioaction_middleclick:
						{
							temp_touchAction.touch_action_button = MouseButton.middle;
							break;
						}
						case R.id.button_menu_mouse_radioaction_leftplusrightclick:
						{
							temp_touchAction.touch_action_button = MouseButton.left_plus_right;
							break;
						}
						case R.id.button_menu_mouse_radioaction_movemouse:
						{
							temp_touchAction.touch_action_button = MouseButton.none;
							break;
						}
						}
						break;
					}
					}
				}
			};

			RadioGroup grp = (RadioGroup) mouse_layout.findViewById(R.id.button_menu_mouse_radioduration);
			grp.setOnCheckedChangeListener(onRadioChange);

			RadioGroup grpAction = (RadioGroup) mouse_layout.findViewById(R.id.button_menu_mouse_radioaction);

			switch (temp_touchAction.touch_action_duration)
			{
			case 0:
			{
				grp.check(R.id.button_menu_mouse_radioduration_permanent);
				((RadioButton) grpAction.getChildAt(4)).setText(getLocaleString("widget_edit_mousetouch_action_move"));
				mouseUpDisable.setVisibility(View.GONE);
				mouseDoubleClick.setVisibility(View.GONE);
				break;
			}
			case 1:
			{
				grp.check(R.id.button_menu_mouse_radioduration_temporary);
				((RadioButton) grpAction.getChildAt(4)).setText(getLocaleString("widget_edit_mousetouch_action_move"));
				mouseUpDisable.setVisibility(View.VISIBLE);
				mouseDoubleClick.setVisibility(View.VISIBLE);
				break;
			}
			case 2:
			{
				grp.check(R.id.button_menu_mouse_radioduration_click);
				((RadioButton) grpAction.getChildAt(4)).setText(getLocaleString("widget_edit_mousetouch_bysystem"));
				mouseUpDisable.setVisibility(View.GONE);
				mouseDoubleClick.setVisibility(View.VISIBLE);
				break;
			}
			}

			grpAction.setOnCheckedChangeListener(onRadioChange);

			switch (temp_touchAction.touch_action_button)
			{
			case left:
			{
				grpAction.check(R.id.button_menu_mouse_radioaction_leftclick);
				break;
			}
			case right:
			{
				grpAction.check(R.id.button_menu_mouse_radioaction_rightclick);
				break;
			}
			case middle:
			{
				grpAction.check(R.id.button_menu_mouse_radioaction_middleclick);
				break;
			}
			case left_plus_right:
			{
				grpAction.check(R.id.button_menu_mouse_radioaction_leftplusrightclick);
				break;
			}
			case none:
			{
				grpAction.check(R.id.button_menu_mouse_radioaction_movemouse);
				break;
			}
			default:
				break;
			}
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(mouse_layout);
	}

	private void loadMouseTypeSettings()
	{
		setCaption("widget_edit_mousetype_caption");

		currentType = WidgetType.mouse_type;

		if (temp_mouseTypeAction == null)
		{
			temp_mouseTypeAction = new MouseTypeAction();
			temp_mouseTypeAction.mouse_type_duration = ((MouseTypeAction) button).getMouseTypeDuration();
		}

		if (mousetype_layout == null)
		{
			mousetype_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_mouse_type, null);

			localize(mousetype_layout, R.id.button_menu_mousetype_duration_title,          "widget_edit_mousetype_duration_title");
			localize(mousetype_layout, R.id.button_menu_mousetype_radioduration_permanent, "widget_edit_mousetype_duration_permanent");
			localize(mousetype_layout, R.id.button_menu_mousetype_radioduration_temporary, "widget_edit_mousetype_duration_temporary");

			OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
				{
					switch (radioGroup.getId())
					{
					case R.id.button_menu_mousetype_radioduration:
					{
						switch (checkedId)
						{
						case R.id.button_menu_mousetype_radioduration_permanent:
						{
							temp_mouseTypeAction.mouse_type_duration = 0;
							break;
						}
						case R.id.button_menu_mousetype_radioduration_temporary:
						{
							temp_mouseTypeAction.mouse_type_duration = 1;
							break;
						}
						}
						break;
					}
					}
				}
			};

			RadioGroup grp = (RadioGroup) mousetype_layout.findViewById(R.id.button_menu_mousetype_radioduration);
			grp.setOnCheckedChangeListener(onRadioChange);

			switch (temp_mouseTypeAction.mouse_type_duration)
			{
			case 0:
			{
				grp.check(R.id.button_menu_mousetype_radioduration_permanent);
				break;
			}
			case 1:
			{
				grp.check(R.id.button_menu_mousetype_radioduration_temporary);
				break;
			}
			}
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(mousetype_layout);
	}

	private TextView dpadTypeTitle;
	private TextView dpadInfo;
	private void loadDpadMainLayout()
	{
		if (dpad_main_layout != null)
			return;

		dpad_main_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_dpad_main, null);

		localize(dpad_main_layout, R.id.button_menu_dpad_look_title,          "widget_edit_dpad_look_title");
		localize(dpad_main_layout, R.id.button_menu_dpad_opacity_title,       "common_opacity");
		localize(dpad_main_layout, R.id.button_menu_dpad_cross_opacity_title, "widget_edit_dpad_cross_opacity_title");
		localize(dpad_main_layout, R.id.button_menu_dpad_settings_title,      "common_settings");
		localize(dpad_main_layout, R.id.button_menu_dpad_circle_opacity_title,      "common_circle");

		final ImageView circle = (ImageView)dpad_main_layout.findViewById(R.id.button_menu_dpad_circle);
		AppGlobal.setAlpha(circle, temp_dpad.getCircleOpacity());

		final ImageView crosshair = (ImageView)dpad_main_layout.findViewById(R.id.button_menu_dpad_crosshair);
		AppGlobal.setAlpha(crosshair, temp_dpad.getCrosshairOpacity());

		crosshair.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				uiImageViewer viewer = new uiImageViewer(getContext());
				viewer.setCaption("imgview_caption_choose_cross");
				viewer.loadCrosshairs();

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener()
				{
					@Override
					public void onPick(ImageViewerItem selected)
					{
						Bitmap bitmap = AppGlobal.getBitmapFromImageViewerItem(selected);
						temp_dpad.setCrosshair(selected.getName());

						crosshair.setImageBitmap(bitmap);
					}
				});

				viewer.show();
			}
		});

		crosshair.setImageBitmap(AppGlobal.getBitmap(temp_dpad.getCrosshair()));

		final TextView crosshairOpacityText = (TextView)dpad_main_layout.findViewById(R.id.button_menu_dpad_cross_value);
		crosshairOpacityText.setText("" + temp_dpad.getCrosshairOpacity());

		final TextView circleOpacityText = (TextView)dpad_main_layout.findViewById(R.id.button_menu_dpad_circle_value);
		circleOpacityText.setText("" + temp_dpad.getCircleOpacity());

		OnSeekBarChangeListener onSeek = new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (seekBar.getId() == R.id.button_menu_dpad_crossseek) {
					temp_dpad.setCrosshairOpacity(progress);
					crosshairOpacityText.setText("" + progress);
					AppGlobal.setAlpha(crosshair, progress);
				} else {
					temp_dpad.setCircleOpacity(progress);
					circleOpacityText.setText("" + progress);
					AppGlobal.setAlpha(circle, progress);
				}
			}
		};

		final SeekBar crosshairOpacity = (SeekBar)dpad_main_layout.findViewById(R.id.button_menu_dpad_crossseek);
		crosshairOpacity.setMax(255);
		crosshairOpacity.setProgress(temp_dpad.getCrosshairOpacity());
		crosshairOpacity.setOnSeekBarChangeListener(onSeek);

		final SeekBar circleOpacity = (SeekBar)dpad_main_layout.findViewById(R.id.button_menu_dpad_circleseek);
		circleOpacity.setMax(255);
		circleOpacity.setProgress(temp_dpad.getCircleOpacity());
		circleOpacity.setOnSeekBarChangeListener(onSeek);

		dpadTypeTitle = (TextView)dpad_main_layout.findViewById(R.id.button_menu_dpad_type);
		dpadInfo = (TextView)dpad_main_layout.findViewById(R.id.button_menu_dpad_type_info);

		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.button_menu_dpad_cross_minus:
					{
						if (temp_dpad.getCrosshairOpacity() == 0)
							return;

						temp_dpad.setCrosshairOpacity(temp_dpad.getCrosshairOpacity() - 1);

						crosshairOpacity.setProgress(temp_dpad.getCrosshairOpacity());
						crosshairOpacityText.setText("" + temp_dpad.getCrosshairOpacity());
						break;
					}
					case R.id.button_menu_dpad_cross_plus:
					{
						if (temp_dpad.getCrosshairOpacity() == 255)
							return;

						temp_dpad.setCrosshairOpacity(temp_dpad.getCrosshairOpacity() + 1);

						crosshairOpacity.setProgress(temp_dpad.getCrosshairOpacity());
						crosshairOpacityText.setText("" + temp_dpad.getCrosshairOpacity());
						break;
					}
					case R.id.button_menu_dpad_circle_minus:
					{
						if (temp_dpad.getCircleOpacity() == 0)
							return;

						temp_dpad.setCircleOpacity(temp_dpad.getCircleOpacity() - 1);

						circleOpacity.setProgress(temp_dpad.getCircleOpacity());
						circleOpacityText.setText("" + temp_dpad.getCircleOpacity());
						break;
					}
					case R.id.button_menu_dpad_circle_plus:
					{
						if (temp_dpad.getCircleOpacity() == 255)
							return;

						temp_dpad.setCircleOpacity(temp_dpad.getCircleOpacity() + 1);

						circleOpacity.setProgress(temp_dpad.getCircleOpacity());
						circleOpacityText.setText("" + temp_dpad.getCircleOpacity());
						break;
					}
					case R.id.button_menu_dpad_type_minus:
					{
						if (temp_dpad.getDpadType() == DpadType.native_two_axis) {
							temp_dpad.setDpadType(DpadType.mouse_abs);
							loadDpadMouseLayout();
							updateDpadView(dpad_2axis_layout, dpad_mouse_layout);
							dpadInfo.setText(Localization.getString("widget_edit_dpad_mouseabs_info"));

						} else if (temp_dpad.getDpadType() == DpadType.mouse_abs) {
							temp_dpad.setDpadType(DpadType.eight_way);

							loadDpadClassicLayout();
							updateDpadView(dpad_mouse_layout, dpad_classic_layout);

							dpadInfo.setText(Localization.getString("widget_edit_dpad_keyboard_info"));

							set8WayDpadView();
						} else if (temp_dpad.getDpadType() == DpadType.eight_way) {
							temp_dpad.setDpadType(DpadType.four_way);

							set4WayDpadView();
						}
						setDpatTypeTitle();
						break;
					}
					case R.id.button_menu_dpad_type_plus:
					{
						if (temp_dpad.getDpadType() == DpadType.four_way) {
							temp_dpad.setDpadType(DpadType.eight_way);

							set8WayDpadView();

						} else if (temp_dpad.getDpadType() == DpadType.eight_way) {
							temp_dpad.setDpadType(DpadType.mouse_abs);

							loadDpadMouseLayout();
							updateDpadView(dpad_classic_layout, dpad_mouse_layout);

							dpadInfo.setText(Localization.getString("widget_edit_dpad_mouseabs_info"));
						} else if (temp_dpad.getDpadType() == DpadType.mouse_abs) {
							temp_dpad.setDpadType(DpadType.native_two_axis);

							loadTwoAxisLayout();
							updateDpadView(dpad_mouse_layout, dpad_2axis_layout);

							dpadInfo.setText(Localization.getString("widget_edit_dpad_twoaxis_info"));
						}
						setDpatTypeTitle();
						break;
					}
				}
			}
		};

		//crosshair
		ImageButton zoom = (ImageButton)dpad_main_layout.findViewById(R.id.button_menu_dpad_cross_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton)dpad_main_layout.findViewById(R.id.button_menu_dpad_cross_plus);
		zoom.setOnClickListener(buttonClick);

		//circle
		zoom = (ImageButton)dpad_main_layout.findViewById(R.id.button_menu_dpad_circle_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton)dpad_main_layout.findViewById(R.id.button_menu_dpad_circle_plus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton)dpad_main_layout.findViewById(R.id.button_menu_dpad_type_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton)dpad_main_layout.findViewById(R.id.button_menu_dpad_type_plus);
		zoom.setOnClickListener(buttonClick);
	}

	private void setDpatTypeTitle() {
		switch (temp_dpad.getDpadType()) {
			case four_way:{
				dpadTypeTitle.setText(Localization.getString("widget_edit_dpad_type_fourway"));
				break;
			}
			case eight_way:{
				dpadTypeTitle.setText(Localization.getString("widget_edit_dpad_type_eightway"));
				break;
			}
			case mouse_abs:{
				dpadTypeTitle.setText(Localization.getString("common_mouse"));
				break;
			}
			case native_two_axis:{
				dpadTypeTitle.setText(Localization.getString("common_2axis"));
				break;
			}
		}
	}

	private void set4WayDpadView() {
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_joindirections).setVisibility(View.VISIBLE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow5).setVisibility(View.GONE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow6).setVisibility(View.GONE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow7).setVisibility(View.GONE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow8).setVisibility(View.GONE);
	}

	private void set8WayDpadView() {
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_joindirections).setVisibility(View.GONE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow5).setVisibility(View.VISIBLE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow6).setVisibility(View.VISIBLE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow7).setVisibility(View.VISIBLE);
		dpad_classic_layout.findViewById(R.id.button_menu_dpad_tableRow8).setVisibility(View.VISIBLE);
	}

	private void updateDpadView(View removeView, View addView) {
		if (removeView != null) {
			dpad_main_layout.removeView(removeView);
		}

		dpad_main_layout.addView(addView);
	}

	private void loadDpadMouseLayout()
	{
		if (dpad_mouse_layout != null)
			return;

		dpad_mouse_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_dpad_mouseabs, null);

		localize(dpad_mouse_layout, R.id.button_menu_dpad_recentermouse, "widget_edit_dpad_flag_recenter");
		localize(dpad_mouse_layout, R.id.button_menu_dpad_mouseaction, "common_mouseaction");
		localize(dpad_mouse_layout, R.id.button_menu_dpad_boundaries_enabled, "common_enabled");
		localize(dpad_mouse_layout, R.id.button_menu_dpad_boundaries_title, "widget_edit_dpad_boundary");

		CheckBox cbx = (CheckBox) dpad_mouse_layout.findViewById(R.id.button_menu_dpad_recentermouse);
		cbx.setChecked(temp_dpad.getRecenterMouse());

		cbx.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				temp_dpad.setRecenterMouse(!temp_dpad.getRecenterMouse());
			}
		});

		cbx = (CheckBox) dpad_mouse_layout.findViewById(R.id.button_menu_dpad_boundaries_enabled);
		cbx.setChecked(temp_dpad.mouseBoundaryEnabled);

		cbx.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				temp_dpad.mouseBoundaryEnabled = !temp_dpad.mouseBoundaryEnabled;
			}
		});

		final TextView mouseAction = (TextView)dpad_mouse_layout.findViewById(R.id.button_menu_dpad_mouseaction_value);
		setDpadMouseActionTitle(mouseAction);

		final Button boundaryButton = (Button)dpad_mouse_layout.findViewById(R.id.button_dpad_boundaries);
		boundaryButton.setText("[" + (int) temp_dpad.mouseBoundaryX1 + "," + (int) temp_dpad.mouseBoundaryY1 + "][" + (int) temp_dpad.mouseBoundaryX2 + "," + (int) temp_dpad.mouseBoundaryY2 + "]");

		View.OnClickListener onClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v) {
				switch (v.getId())
				{
					case R.id.button_menu_dpad_mouseaction_minus: {
						MouseButton mb = temp_dpad.getMouseButton();

						if (mb != null) {
							if (mb == MouseButton.middle) {
								temp_dpad.setMouseButton(MouseButton.right);
							} else if (mb == MouseButton.right) {
								temp_dpad.setMouseButton(MouseButton.left);
							} else {
								temp_dpad.setMouseButton(null);
							}

							setDpadMouseActionTitle(mouseAction);
						}
						break;
					}
					case R.id.button_menu_dpad_mouseaction_plus: {
						MouseButton mb = temp_dpad.getMouseButton();

						if (mb != MouseButton.middle) {
							if (mb == null) {
								temp_dpad.setMouseButton(MouseButton.left);
							} else if (mb == MouseButton.left) {
								temp_dpad.setMouseButton(MouseButton.right);
							} else {
								temp_dpad.setMouseButton(MouseButton.middle);
							}

							setDpadMouseActionTitle(mouseAction);
						}
						break;
					}
					case R.id.button_dpad_boundaries: {
						BoundaryMode.start(button, temp_dpad.mouseBoundaryX1, temp_dpad.mouseBoundaryY1, temp_dpad.mouseBoundaryX2, temp_dpad.mouseBoundaryY2, new BoundaryMode.OnBoundaryModeListener() {
							@Override
							public void onFinish(float x1, float y1, float x2, float y2) {
								temp_dpad.mouseBoundaryX1 = x1;
								temp_dpad.mouseBoundaryY1 = y1;
								temp_dpad.mouseBoundaryX2 = x2;
								temp_dpad.mouseBoundaryY2 = y2;

								boundaryButton.setText("[" + (int) temp_dpad.mouseBoundaryX1 + "," + (int) temp_dpad.mouseBoundaryY1 + "][" + (int) temp_dpad.mouseBoundaryX2 + "," + (int) temp_dpad.mouseBoundaryY2 + "]");
							}
						});
						break;
					}
				}
			}
		};

		boundaryButton.setOnClickListener(onClick);

		dpad_mouse_layout.findViewById(R.id.button_menu_dpad_mouseaction_minus).setOnClickListener(onClick);
		dpad_mouse_layout.findViewById(R.id.button_menu_dpad_mouseaction_plus).setOnClickListener(onClick);
	}

	private void loadTwoAxisLayout()
	{
		if (dpad_2axis_layout != null)
			return;

		dpad_2axis_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_dpad_twoaxis, null);

		localize(dpad_2axis_layout, R.id.button_menu_dpad_addjoybuttons_info, "common_addvirtualbuttons");
		localize(dpad_2axis_layout, R.id.button_menu_dpad_fullRange, "widget_edit_dpad_fullrange");

		View.OnClickListener onClick = new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (v.getId() == R.id.button_menu_dpad_fullRange) {
					temp_dpad.setTwoAxisFullRange(!temp_dpad.twoAxisFullRange);
					((CheckBox) v).setChecked(temp_dpad.twoAxisFullRange);
					return;
				}

				uiImageViewer buttonPicker = new uiImageViewer(getContext(), true);
				buttonPicker.setCaption("common_buttons");
				buttonPicker.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
					@Override
					public boolean onSet(List images) {
						images.add(new ImageViewerItem(R.drawable.img_button_a, "0", "common_button1"));
						images.add(new ImageViewerItem(R.drawable.img_button_b, "1", "common_button2"));
						images.add(new ImageViewerItem(R.drawable.img_button_x, "2", "common_button3"));
						images.add(new ImageViewerItem(R.drawable.img_button_y, "3", "common_button4"));
						return true;
					}
				});

				buttonPicker.setOnImageViewerMultiEventListener(new ImageViewer.ImageViewerMultiEventListener() {
					@Override
					public void onPick(List<ImageViewerItem> selected) {
						if (selected == null || selected.size() == 0) {
							return;
						}

						int index;
						int middleW = Screen.screenWidth / 2;
						int middleH = Screen.screenHeight / 2;
						int x, y;
						String imageCode;
						for (ImageViewerItem item : selected) {
							index = Integer.parseInt(item.getName());
							switch (index) {
								case 0: {
									x = middleW - AppGlobal.widgetSize / 2;
									y = middleH + AppGlobal.widgetSize / 2;
									imageCode = "img_button_a";
									break;
								}
								case 1: {
									x = middleW + AppGlobal.widgetSize / 2;
									y = middleH - AppGlobal.widgetSize / 2;
									imageCode = "img_button_b";
									break;
								}
								case 2: {
									x = middleW - (AppGlobal.widgetSize + AppGlobal.widgetSize / 2);
									y = middleH - AppGlobal.widgetSize / 2;
									imageCode = "img_button_x";
									break;
								}
								case 3: {
									x = middleW - AppGlobal.widgetSize / 2;
									y = middleH - (AppGlobal.widgetSize + AppGlobal.widgetSize / 2);
									imageCode = "img_button_y";
									break;
								}
								default:
									continue;
							}

							JoystickButton w = new JoystickButton(x, y, AppGlobal.widgetSize, AppGlobal.widgetSize, "Button " + index);
							w.setButton(index);
							w.setTextEnabled(false);
							w.setBitmap(imageCode);
							w.setBitmapEnabled(true);
							w.setTransparency(0);
							w.getBitmap().setTransparency(120);
							w.update();

							EmuManager.addWidget(w);
							EmuManager.addNewWidget(w);

							MessageInfo.info("msg_buttons_added_toscreen");
						}
					}
				});

				buttonPicker.show();
			}
		};

		ImageButton button = (ImageButton)dpad_2axis_layout.findViewById(R.id.button_menu_dpad_addjoybuttons);
		button.setOnClickListener(onClick);

		CheckBox cbx = (CheckBox)dpad_2axis_layout.findViewById(R.id.button_menu_dpad_fullRange);
		cbx.setChecked(temp_dpad.twoAxisFullRange);
		cbx.setOnClickListener(onClick);
	}

	private void setDpadMouseActionTitle(TextView textView) {
		if (temp_dpad.getMouseButton() == null) {
			textView.setText(Localization.getString("common_move"));
		} else {
			switch (temp_dpad.getMouseButton()) {
				case left: {
					textView.setText(Localization.getString("mouse_button_sleft"));
					break;
				}
				case right: {
					textView.setText(Localization.getString("mouse_button_sright"));
					break;
				}
				case middle: {
					textView.setText(Localization.getString("mouse_button_smiddle"));
					break;
				}
			}
		}
	}

	private String setDirectionCodeLabel(Direction Direction) {
		int keyCode = temp_dpad.getKey(Direction).keyCode;

		if (keyCode == -1) {
			return "";
		}

		return KeyCodeInfo.getDosboxKeyInfo(keyCode, false);
	}

	private void loadDpadClassicLayout()
	{
		if (dpad_classic_layout != null)
			return;

		dpad_classic_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_dpad_classic, null);

		localize(dpad_classic_layout, R.id.button_menu_dpad_joindirections,      "common_diagonals_on");
		localize(dpad_classic_layout, R.id.button_menu_dpad_activateOutside,     "widget_edit_dpad_flag_activeoutside");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyup_title,         "widget_edit_dpad_direction_keyup");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keydown_title,       "widget_edit_dpad_direction_keydown");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyleft_title,       "widget_edit_dpad_direction_keyleft");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyright_title,      "widget_edit_dpad_direction_keyright");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyrightup_title, "widget_edit_dpad_direction_keyright-up");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyrightdown_title, "widget_edit_dpad_direction_keyright-down");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyleftup_title, "widget_edit_dpad_direction_keyleft-up");
		localize(dpad_classic_layout, R.id.button_menu_dpad_keyleftdown_title, "widget_edit_dpad_direction_keyleft-down");

		View.OnClickListener DirectionClick = new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				uiKeyCodesDialog d = new uiKeyCodesDialog(context);
				d.setOnKeyCodeListener(new KeyCodeListener()
				{
					@Override
					public void onPick(KeyCodeItem selected)
					{
						//Button b = (Button) v;
						//b.setText(selected.getText());

						Key key = null;

						switch (v.getId())
						{
							case R.id.button_menu_dpad_keyup:
							{
								key = temp_dpad.getKey(Direction.up);
								break;
							}
							case R.id.button_menu_dpad_keydown:
							{
								key = temp_dpad.getKey(Direction.down);
								break;
							}
							case R.id.button_menu_dpad_keyleft:
							{
								key = temp_dpad.getKey(Direction.left);
								break;
							}
							case R.id.button_menu_dpad_keyright:
							{
								key = temp_dpad.getKey(Direction.right);
								break;
							}
							case R.id.button_menu_dpad_keyrightup:
							{
								key = temp_dpad.getKey(Direction.right_up);
								break;
							}
							case R.id.button_menu_dpad_keyrightdown:
							{
								key = temp_dpad.getKey(Direction.right_down);
								break;
							}
							case R.id.button_menu_dpad_keyleftup:
							{
								key = temp_dpad.getKey(Direction.left_up);
								break;
							}
							case R.id.button_menu_dpad_keyleftdown:
							{
								key = temp_dpad.getKey(Direction.left_down);
								break;
							}
						}

						if (key != null)
						{
							key.setKeyCode(selected.getKeyCode());
							//key.setText(selected.getText());
							((Button)v).setText(KeyCodeInfo.getDosboxKeyInfo(key.keyCode, key.shift));
						}
					}
				});

				d.show();
			}
		};

		View.OnClickListener modifiersClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Key key = temp_dpad.getKey((Direction)v.getTag());
				KeySettings d = new KeySettings(AppGlobal.context, key, null, true, false);
				d.setKeySettingsEventListener(new KeySettingsEventListener() {
					@Override
					public void onChange(boolean ctrl, boolean alt, boolean shift, KeyAction action) {
						key.ctrl = ctrl;
						key.alt = alt;
						key.shift = shift;
					}
				});
				d.show();
			}
		};

		Button but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyup);
		but.setText(setDirectionCodeLabel(Direction.up));
		but.setOnClickListener(DirectionClick);
		ImageButton iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyup_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.up);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keydown);
		but.setText(setDirectionCodeLabel(Direction.down));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keydown_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.down);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyleft);
		but.setText(setDirectionCodeLabel(Direction.left));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyleft_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.left);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyright);
		but.setText(setDirectionCodeLabel(Direction.right));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyright_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.right);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyrightup);
		but.setText(setDirectionCodeLabel(Direction.right_up));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyrightup_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.right_up);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyrightdown);
		but.setText(setDirectionCodeLabel(Direction.right_down));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyrightdown_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.right_down);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyleftup);
		but.setText(setDirectionCodeLabel(Direction.left_up));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyleftup_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.left_up);

		but = (Button) dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyleftdown);
		but.setText(setDirectionCodeLabel(Direction.left_down));
		but.setOnClickListener(DirectionClick);
		iBut = (ImageButton)dpad_classic_layout.findViewById(R.id.button_menu_dpad_keyleftdown_settings);
		iBut.setOnClickListener(modifiersClick);
		iBut.setTag(Direction.left_down);

		CheckBox cbx = (CheckBox) dpad_classic_layout.findViewById(R.id.button_menu_dpad_joindirections);
		cbx.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				temp_dpad.setJoinDirections(!temp_dpad.isJoinDirections());
			}
		});

		cbx.setChecked(temp_dpad.isJoinDirections());

		cbx = (CheckBox) dpad_classic_layout.findViewById(R.id.button_menu_dpad_activateOutside);
		cbx.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				temp_dpad.setActiveOutside(!temp_dpad.isActiveOutside());
			}
		});

		cbx.setChecked(temp_dpad.isActiveOutside());
	}

	private void loadDpadSettings()
	{
		setCaption("widget_edit_dpad_caption");

		currentType = WidgetType.dpad;

		if (temp_dpad == null)
		{
			temp_dpad = new VirtualDpad();

			((VirtualDpad) button).copyTo(temp_dpad);
		}

		loadDpadMainLayout();

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(dpad_main_layout);

		if (temp_dpad.getDpadType() == DpadType.native_two_axis) {
			loadTwoAxisLayout();
			updateDpadView(null, dpad_2axis_layout);
			dpadInfo.setText(Localization.getString("widget_edit_dpad_twoaxis_info"));
		} else if (temp_dpad.getDpadType() == DpadType.mouse_abs) {
			loadDpadMouseLayout();
			updateDpadView(null, dpad_mouse_layout);
			dpadInfo.setText(Localization.getString("widget_edit_dpad_mouseabs_info"));
		} else {
			loadDpadClassicLayout();

			if (temp_dpad.getDpadType() == DpadType.four_way) {
				set4WayDpadView();
			} else {
				set8WayDpadView();
			}

			updateDpadView(null, dpad_classic_layout);
			dpadInfo.setText(Localization.getString("widget_edit_dpad_keyboard_info"));
		}

		setDpatTypeTitle();
	}

	private void loadFolderSettings()
	{
		setCaption("widget_edit_bag_caption");

		currentType = WidgetType.folder;

		if (temp_folder == null)
		{
			temp_folder = new Folder();

			((Folder) button).copyTo(temp_folder);
		}

		if (folder_layout == null)
		{
			folder_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_folder, null);

			localize(folder_layout, R.id.button_menu_folder_type_title,                "widget_edit_type_title");
			localize(folder_layout, R.id.button_menu_folder_folderlayout_autoalign,    "widget_edit_type_autoaligned");
			localize(folder_layout, R.id.button_menu_folder_folderlayout_custom_align, "widget_edit_type_custom");
			localize(folder_layout, R.id.button_menu_folder_customalign_title,         "widget_edit_type_customalign_title");
			localize(folder_layout, R.id.button_menu_folder_customalign_columns,       "widget_edit_type_customalign_columns");
			localize(folder_layout, R.id.button_menu_folder_customalign_rows,          "widget_edit_type_customalign_rows");

			final LinearLayout lin = (LinearLayout) folder_layout.findViewById(R.id.button_menu_folder_customalign);

			OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
				{
					switch (checkedId)
					{
					case R.id.button_menu_folder_folderlayout_autoalign:
					{
						temp_folder.setFolderLayout(FolderLayout.auto_aligned);
						lin.setVisibility(View.GONE);
						break;
					}
					case R.id.button_menu_folder_folderlayout_custom_align:
					{
						temp_folder.setFolderLayout(FolderLayout.custom_aligned);
						lin.setVisibility(View.VISIBLE);

						mainScroll.post(new Runnable()
						{
							@Override
							public void run()
							{
								mainScroll.scrollTo(0, folder_layout.getBottom());
							}
						});
						break;
					}
					}
				}
			};

			RadioGroup grp = (RadioGroup) folder_layout.findViewById(R.id.button_menu_folder_folderlayout);
			grp.setOnCheckedChangeListener(onRadioChange);

			switch (temp_folder.getFolderLayout())
			{
			case auto_aligned:
			{
				grp.check(R.id.button_menu_folder_folderlayout_autoalign);
				lin.setVisibility(View.GONE);
				break;
			}
			case custom_aligned:
			{
				grp.check(R.id.button_menu_folder_folderlayout_custom_align);
				lin.setVisibility(View.VISIBLE);
				break;
			}
			}

			final TextView txtCols = (TextView) folder_layout.findViewById(R.id.button_menu_folder_customalign_cols_text);
			txtCols.setText("" + temp_folder.getColumnCount());

			final TextView txtRows = (TextView) folder_layout.findViewById(R.id.button_menu_folder_customalign_rows_text);
			txtRows.setText("" + temp_folder.getRowCount());

			View.OnClickListener butClick = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					switch (v.getId())
					{
					case R.id.button_menu_folder_customalign_cols_plus:
					{
						temp_folder.plusColumns();
						break;
					}
					case R.id.button_menu_folder_customalign_cols_minus:
					{
						temp_folder.minusColumns();
						break;
					}
					case R.id.button_menu_folder_customalign_rows_plus:
					{
						temp_folder.plusRows();
						break;
					}
						case R.id.button_menu_folder_customalign_rows_minus:
					{
						temp_folder.minusRows();
						break;
					}
					}

					txtCols.setText("" + temp_folder.getColumnCount());
					txtRows.setText("" + temp_folder.getRowCount());
				}
			};

			Button b = (Button) folder_layout.findViewById(R.id.button_menu_folder_customalign_cols_plus);
			b.setOnClickListener(butClick);

			b = (Button) folder_layout.findViewById(R.id.button_menu_folder_customalign_cols_minus);
			b.setOnClickListener(butClick);

			b = (Button) folder_layout.findViewById(R.id.button_menu_folder_customalign_rows_plus);
			b.setOnClickListener(butClick);

			b = (Button) folder_layout.findViewById(R.id.button_menu_folder_customalign_rows_minus);
			b.setOnClickListener(butClick);
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(folder_layout);
	}

	private void loadJournalSettings()
	{
		setCaption("widget_edit_journal_caption");

		currentType = WidgetType.journal;

		if (temp_journal == null)
		{
			//temp_journal = new uiJournal(button);
			temp_journal = new Journal();

			/*if (button.getJournal() != null)
			{
				button.getJournal().copyTo(temp_journal);
			}*/
		}
		/*
		if (journal_layout == null)
		{
			journal_layout = (LinearLayout)inflater.inflate(R.layout.button_menu_journal, null);
		}

		RelativeLayout sv = (RelativeLayout)findViewById(R.id.button_menu_mainview);
		
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.BELOW, R.id.button_menu_section_specific);		
		
		sv.addView(journal_layout, p);*/
	}

	private void loadWalkthroughSettings()
	{
		setCaption("widget_edit_walkthrough_caption");

		currentType = WidgetType.walkthrough;

		if (temp_walkthrough == null)
		{
			temp_walkthrough = new Walkthrough();

			((Walkthrough) button).copyTo(temp_walkthrough);
		}

		if (walkthrough_layout == null)
		{
			walkthrough_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_walkthrough, null);

			localize(walkthrough_layout, R.id.button_menu_walkthrough_load, "widget_edit_walkthrough_find");

			final EditText html = (EditText) walkthrough_layout.findViewById(R.id.button_menu_walkthrough_htmlpath);
			html.setText(temp_walkthrough.getHtmlPath());
			html.setEnabled(false);

			Button butLoad = (Button) walkthrough_layout.findViewById(R.id.button_menu_walkthrough_load);
			butLoad.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Storages.onDrivePick(context, new Storages.onDrivePickListener()
					{
						@Override
						public void onPick(String drive)
						{
							FileBrowser fb = new FileBrowser(AppGlobal.context, drive, new String[] { ".html" });
							fb.setCaption("fb_caption_choose_html");
							fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
							{
								@Override
								public void onPick(String selected)
								{
									temp_walkthrough.setHtmlPath("file://" + selected);
									html.setText(temp_walkthrough.getHtmlPath());
								}
							});
							fb.show();
						}
					});
				}
			});
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(walkthrough_layout);
	}

	private ComboActionAdapter caAdapter;

	private void loadComboSettings()
	{
		setCaption("widget_edit_combo_caption");

		currentType = WidgetType.combo;
/*
		if (temp_combo == null)
		{
			temp_combo = new uiCombo(button);
			((uiCombo) button).copyTo(temp_combo);
			
			//temp_combo.actions = new ArrayList<ComboAction>();
			//caAdapter = new ComboActionAdapter(context, temp_combo.actions);	
		}*/

		if (combo_layout == null)
		{
			temp_combo = new Combo(button);
			((Combo) button).copyTo(temp_combo);

			combo_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_combo, null);

			localize(combo_layout, R.id.button_menu_runwidgetatstartup, "common_runatstart");

			View.OnClickListener keyBoolEvent = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (v.getId() == R.id.button_menu_runwidgetatstartup)
					{
						runAtStart=!runAtStart;
					}
				}
			};

			CheckBox cbx = (CheckBox) combo_layout.findViewById(R.id.button_menu_runwidgetatstartup);
			cbx.setOnClickListener(keyBoolEvent);
			runAtStart = EmuConfig.startupWidgetEnabled && EmuConfig.startupWidgetID.equals(button.getName());
			cbx.setChecked(runAtStart);

			ImageButton butAdd = (ImageButton) combo_layout.findViewById(R.id.button_menu_combo_add);
			butAdd.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					uiImageViewer viewer = new uiImageViewer(context);
					viewer.setCaption("widget_edit_combo_menu_caption");
					viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {

						@Override
						public boolean onSet(List images) {
							images.add(new ImageViewerItem(R.drawable.img_key, "key", getLocaleString("widget_edit_combo_menu_key")));
							images.add(new ImageViewerItem(R.drawable.img_campfire, "delay", getLocaleString("widget_edit_combo_menu_delay")));
							images.add(new ImageViewerItem(R.drawable.img_pouch, "close_parent_folder", getLocaleString("widget_edit_combo_menu_closebag")));
							images.add(new ImageViewerItem(R.drawable.img_bag, "folder", getLocaleString("widget_edit_combo_menu_bag")));
							images.add(new ImageViewerItem(R.drawable.img_ghost, "nonlayout", getLocaleString("widget_edit_combo_menu_gp")));
							images.add(new ImageViewerItem(R.drawable.img_claws, "mbtoggle", getLocaleString("widget_edit_combo_menu_mbtoggle")));
							images.add(new ImageViewerItem(R.drawable.img_magic, "special", getLocaleString("widget_edit_combo_menu_special")));
							images.add(new ImageViewerItem(R.drawable.img_dummytarget, "target", getLocaleString("widget_edit_combo_menu_target")));
							images.add(new ImageViewerItem(R.drawable.img_navigation, "navigation", getLocaleString("widget_edit_combo_menu_nav")));
							return true;
						}
					});

					viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
						@Override
						public void onPick(ImageViewerItem selected) {
							ComboAction a = null;

							if (selected.getName().equals("key")) {
								a = new KeyComboAction();
							} else if (selected.getName().equals("delay")) {
								a = new DelayComboAction();
							} else if (selected.getName().equals("close_parent_folder")) {
								a = new CloseParentFolderComboAction();
							} else if (selected.getName().equals("folder")) {
								a = new FolderComboAction();
							} else if (selected.getName().equals("nonlayout")) {
								a = new NonLayoutComboAction();
							} else if (selected.getName().equals("mbtoggle")) {
								a = new MouseToggleComboAction();
							} else if (selected.getName().equals("special")) {
								a = new SpecialComboAction();
							} else if (selected.getName().equals("target")) {
								a = new TargetComboAction();
							} else if (selected.getName().equals("navigation")) {
								a = new MouseNavigationComboAction();
							}

							caAdapter.addItem(a, true);

							mainScroll.post(new Runnable()
							{
								@Override
								public void run()
								{
									mainScroll.scrollTo(0, combo_layout.getBottom());
								}
							});
						}
					});

					viewer.show();
				}
			});

			LinearLayout lw = (LinearLayout) combo_layout.findViewById(R.id.button_menu_combo_listview);

			if (caAdapter == null)
				caAdapter = new ComboActionAdapter(lw, temp_combo.getActionList());
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(combo_layout);
	}

	private void loadPointClickSettings()
	{
		setCaption("widget_edit_pointclick_caption");

		currentType = WidgetType.point_click;

		if (temp_pointclick == null)
		{
			temp_pointclick = new PointClick();
			((PointClick) button).copyTo(temp_pointclick);
		}

		if (pointClick_layout == null)
		{
			pointClick_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_pointclick, null);

			localize(pointClick_layout, R.id.button_menu_pointclick_target_title, "widget_edit_pointclick_target_title");
			localize(pointClick_layout, R.id.button_menu_pointclick_find, "widget_edit_pointclick_target_find");
			localize(pointClick_layout, R.id.button_menu_pointclick_mouseaction_title, "common_mouseaction");
			localize(pointClick_layout, R.id.button_menu_pointclick_radioaction_click, "widget_edit_pointclick_mouseaction_click");
			localize(pointClick_layout, R.id.button_menu_pointclick_radioaction_move, "widget_edit_pointclick_mouseaction_move");
			localize(pointClick_layout, R.id.button_menu_pointclick_mousebutton_title, "button_menu_pointclick_mousebutton_title");
			localize(pointClick_layout, R.id.button_menu_pointclick_radiobutton_left, "mouse_button_sleft");
			localize(pointClick_layout, R.id.button_menu_pointclick_radiobutton_middle, "mouse_button_smiddle");
			localize(pointClick_layout, R.id.button_menu_pointclick_radiobutton_right, "mouse_button_sright");
		}

		final Button findPoint = (Button) pointClick_layout.findViewById(R.id.button_menu_pointclick_find);

		findPoint.setText("[" + (int) temp_pointclick.getX() + "," + (int) temp_pointclick.getY() + "]");
		findPoint.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FindPointMode.start(temp_pointclick, temp_pointclick.getX(), temp_pointclick.getY(), new FindPointMode.OnFindPointModeListener() {
					@Override
					public void onFinish(float x, float y) {
						temp_pointclick.setX(x);
						temp_pointclick.setY(y);
						findPoint.setText("[" + (int) temp_pointclick.getX() + "," + (int) temp_pointclick.getY() + "]");
					}
				});
			}
		});

		final LinearLayout buttonPanel = (LinearLayout) pointClick_layout.findViewById(R.id.button_menu_pointclick_radiobuttonpanel);

		OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
			{
				switch (checkedId)
				{
				case R.id.button_menu_pointclick_radioaction_click:
				{
					temp_pointclick.setPointAction(PointClickAction.click);
					buttonPanel.setVisibility(View.VISIBLE);

					mainScroll.post(new Runnable()
					{
						@Override
						public void run()
						{
							mainScroll.scrollTo(0, buttonPanel.getBottom());
						}
					});

					break;
				}
				case R.id.button_menu_pointclick_radioaction_move:
				{
					temp_pointclick.setPointAction(PointClickAction.move);
					buttonPanel.setVisibility(View.GONE);
					break;
				}
				case R.id.button_menu_pointclick_radiobutton_left:
				{
					temp_pointclick.setMouseButton(MouseButton.left);
					break;
				}
				case R.id.button_menu_pointclick_radiobutton_middle:
				{
					temp_pointclick.setMouseButton(MouseButton.middle);
					break;
				}
				case R.id.button_menu_pointclick_radiobutton_right:
				{
					temp_pointclick.setMouseButton(MouseButton.right);
					break;
				}
				}
			}
		};

		RadioGroup grp = (RadioGroup) pointClick_layout.findViewById(R.id.button_menu_pointclick_radioaction);
		grp.setOnCheckedChangeListener(onRadioChange);

		switch (temp_pointclick.getPointAction())
		{
		case click:
		{
			grp.check(R.id.button_menu_pointclick_radioaction_click);
			break;
		}
		case move:
		{
			grp.check(R.id.button_menu_pointclick_radioaction_move);
			break;
		}
		}

		grp = (RadioGroup) pointClick_layout.findViewById(R.id.button_menu_pointclick_radiobutton);
		grp.setOnCheckedChangeListener(onRadioChange);

		switch (temp_pointclick.getMouseButton())
		{
		case left:
		{
			grp.check(R.id.button_menu_pointclick_radiobutton_left);
			break;
		}
		case middle:
		{
			grp.check(R.id.button_menu_pointclick_radiobutton_middle);
			break;
		}
		case right:
		{
			grp.check(R.id.button_menu_pointclick_radiobutton_right);
			break;
		}
		default:
			break;
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(pointClick_layout);
	}

	private void loadSpecialActionSettings()
	{
		setCaption("widget_edit_special_caption");

		currentType = WidgetType.special;

		if (temp_specialAction == null)
		{
			temp_specialAction = new SpecialAction();
			((SpecialAction) button).copyTo(temp_specialAction);
		}

		if (specialAction_layout == null)
		{
			specialAction_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_special_action, null);

			localize(specialAction_layout, R.id.button_menu_specialaction_title,                 "widget_edit_special_action_title");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_showkeyboard,    "common_show_keyboard");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_showbuiltinkeyboard,    "widget_edit_special_action_dbxkeyboard");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_pause,           "widget_edit_special_action_pause");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_mousecalibrate,  "widget_edit_special_action_mousereset");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_specialkeys,     "widget_edit_special_action_specialkeys");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_quit,            "widget_edit_special_action_quit");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_screenshot,      "widget_edit_special_action_screenshot");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_hideallbuttons,  "widget_edit_special_action_hidebuttons");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_generalsettings, "widget_edit_special_action_showgenset");
			localize(specialAction_layout, R.id.button_menu_specialaction_radio_turbomode, "common_turbomode");
		}

		OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
			{
				switch (checkedId) {
					case R.id.button_menu_specialaction_radio_showkeyboard: {
						temp_specialAction.setCall(SpecialAction.Action.show_keyboard);
						break;
					}
					case R.id.button_menu_specialaction_radio_showbuiltinkeyboard: {
						temp_specialAction.setCall(SpecialAction.Action.show_built_in_keyboard);
						break;
					}
					case R.id.button_menu_specialaction_radio_pause: {
						temp_specialAction.setCall(SpecialAction.Action.pause);
						break;
					}
					case R.id.button_menu_specialaction_radio_mousecalibrate: {
						temp_specialAction.setCall(SpecialAction.Action.reset_mouse_position);
						break;
					}
					case R.id.button_menu_specialaction_radio_specialkeys: {
						temp_specialAction.setCall(SpecialAction.Action.special_keys);
						break;
					}
					case R.id.button_menu_specialaction_radio_quit: {
						temp_specialAction.setCall(SpecialAction.Action.quit);
						break;
					}
					case R.id.button_menu_specialaction_radio_screenshot: {
						temp_specialAction.setCall(SpecialAction.Action.screenshot);
						break;
					}
					case R.id.button_menu_specialaction_radio_hideallbuttons: {
						temp_specialAction.setCall(SpecialAction.Action.hide_buttons);
						break;
					}
					case R.id.button_menu_specialaction_radio_generalsettings: {
						temp_specialAction.setCall(SpecialAction.Action.general_settings);
						break;
					}
					case R.id.button_menu_specialaction_radio_turbomode: {
						temp_specialAction.setCall(SpecialAction.Action.turboMode);
						break;
					}
				}
			}
		};

		RadioGroup grp = (RadioGroup) specialAction_layout.findViewById(R.id.button_menu_specialaction_calltype);
		grp.setOnCheckedChangeListener(onRadioChange);

		switch (temp_specialAction.getCall()) {
			case show_keyboard: {
				grp.check(R.id.button_menu_specialaction_radio_showkeyboard);
				break;
			}
			case show_built_in_keyboard: {
				grp.check(R.id.button_menu_specialaction_radio_showbuiltinkeyboard);
				break;
			}
			case pause: {
				grp.check(R.id.button_menu_specialaction_radio_pause);
				break;
			}
			case reset_mouse_position: {
				grp.check(R.id.button_menu_specialaction_radio_mousecalibrate);
				break;
			}
			case special_keys: {
				grp.check(R.id.button_menu_specialaction_radio_specialkeys);
				break;
			}
			case quit: {
				grp.check(R.id.button_menu_specialaction_radio_quit);
				break;
			}
			case screenshot: {
				grp.check(R.id.button_menu_specialaction_radio_screenshot);
				break;
			}
			case hide_buttons: {
				grp.check(R.id.button_menu_specialaction_radio_hideallbuttons);
				break;
			}
			case general_settings: {
				grp.check(R.id.button_menu_specialaction_radio_generalsettings);
				break;
			}
			case turboMode: {
				grp.check(R.id.button_menu_specialaction_radio_turbomode);
				break;
			}
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(specialAction_layout);
	}

	private void loadSwitcherSettings()
	{
		setCaption("Chameleon settings");

		currentType = WidgetType.switcher;

		if (temp_switcher == null)
		{
			temp_switcher = new WidgetSwitcher();

			((WidgetSwitcher) button).copyTo(temp_switcher);
		}

		if (switcher_layout == null)
		{
			switcher_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_switcher, null);

			/*final TextView html = (TextView) walkthrough_layout.findViewById(R.id.button_menu_walkthrough_htmlpath);
			html.setText(temp_walkthrough.getHtmlPath());
			html.setEnabled(false);
			html.setBackgroundColor(Color.GRAY);

			Button butLoad = (Button) walkthrough_layout.findViewById(R.id.button_menu_walkthrough_load);
			butLoad.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					uiFileBrowser fb = new uiFileBrowser(uiLayoutManager.ctx, new String[] { ".html" });
					fb.setOnPickFileEvent(new OnPickFileClickListener()
					{
						@Override
						public void onPick(String selected)
						{
							temp_walkthrough.setHtmlPath("file://" + selected);

							html.setText(temp_walkthrough.getHtmlPath());
						}
					});
					fb.show();
				}
			});*/
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(switcher_layout);
	}


	private void loadZoomWidgetSettings() {
		currentType = WidgetType.zoom;

		if (temp_zoomWidget == null)
		{
			temp_zoomWidget = new ZoomWidget();

			((ZoomWidget) button).copyTo(temp_zoomWidget);
		}

		if (zoom_layout == null)
		{
			zoom_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_zoomwidget, null);

			localize(zoom_layout, R.id.button_menu_zoom_duration_title,          "widget_edit_zoom_duration_title");
			localize(zoom_layout, R.id.button_menu_zoom_radioduration_permanent, "widget_edit_zoom_duration_permanent");
			localize(zoom_layout, R.id.button_menu_zoom_radioduration_temporary, "widget_edit_zoom_duration_temporary");

			OnCheckedChangeListener onRadioChange = new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
				{
					switch (radioGroup.getId())
					{
						case R.id.button_menu_zoom_radioduration:
						{
							switch (checkedId)
							{
								case R.id.button_menu_zoom_radioduration_permanent:
								{
									temp_zoomWidget.duration = 0;
									break;
								}
								case R.id.button_menu_zoom_radioduration_temporary:
								{
									temp_zoomWidget.duration = 1;
									break;
								}
							}
							break;
						}
					}
				}
			};

			RadioGroup grp = (RadioGroup) zoom_layout.findViewById(R.id.button_menu_zoom_radioduration);
			grp.setOnCheckedChangeListener(onRadioChange);

			switch (temp_zoomWidget.duration)
			{
				case 0:
				{
					grp.check(R.id.button_menu_zoom_radioduration_permanent);
					break;
				}
				case 1:
				{
					grp.check(R.id.button_menu_zoom_radioduration_temporary);
					break;
				}
			}
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(zoom_layout);
	}

	private void loadJoyButtonSettings()
	{
		currentType = WidgetType.joybutton;

		if (temp_joybutton == null)
		{
			temp_joybutton = new JoystickButton();

			((JoystickButton) button).copyTo(temp_joybutton);
		}

		if (joybutton_layout == null)
		{
			joybutton_layout = (LinearLayout) inflater.inflate(R.layout.button_menu_joybutton, null);

			final ImageButton b = (ImageButton)joybutton_layout.findViewById(R.id.button_menu_joybutton);
			b.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					uiImageViewer buttonPicker = new uiImageViewer(getContext());
					buttonPicker.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
						@Override
						public boolean onSet(List images) {
							images.add(new ImageViewerItem(R.drawable.img_button_a, "0", "A"));
							images.add(new ImageViewerItem(R.drawable.img_button_b, "1", "B"));
							images.add(new ImageViewerItem(R.drawable.img_button_x, "2", "X"));
							images.add(new ImageViewerItem(R.drawable.img_button_y, "3", "Y"));
							return true;
						}
					});

					buttonPicker.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
						@Override
						public void onPick(ImageViewerItem selected) {
							temp_joybutton.button = Integer.parseInt(selected.getName());
							setJoyButtonByType(b);
						}
					});
					buttonPicker.show();
				}
			});

			setJoyButtonByType(b);
		}

		LinearLayout sv = (LinearLayout) findViewById(R.id.button_menu_mainview);
		sv.addView(joybutton_layout);
	}

	private void setJoyButtonByType(ImageButton b) {
		switch (temp_joybutton.button) {
			case 0:{
				b.setImageResource(R.drawable.img_button_a);
				break;
			}
			case 1:{
				b.setImageResource(R.drawable.img_button_b);
				break;
			}
			case 2:{
				b.setImageResource(R.drawable.img_button_x);
				break;
			}
			case 3:{
				b.setImageResource(R.drawable.img_button_y);
				break;
			}
		}
	}

	private void setPreviewText(TextView previewText)
	{
		previewText.setText(temp_title);
		//previewText.setAlpha(temp_textOpacity);
		AppGlobal.setAlpha(previewText, temp_textOpacity);
		previewText.setTextColor(temp_textColor);
	}

	private void loadTitleSettings()
	{
		final Button textData = (Button) findViewById(R.id.button_menu_textdata);
		final TextView previewText = (TextView)findViewById(R.id.button_menu_preview_text);

		textData.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				uiTextSettingsDialog dlg = new uiTextSettingsDialog(context, temp_title, temp_textAlign, temp_textOpacity, temp_textAntialiasing, temp_textFont);
				dlg.setOnTextSettingsChangeListener(new TextSettingsChangeListener()
				{
					@Override
					public void onChange(String text, Align align, int opacity, boolean antialiasing, String fontCode)
					{
						temp_title = text;
						temp_textAlign = align;
						temp_textOpacity = opacity;
						temp_textAntialiasing = antialiasing;
						temp_textFont = fontCode;

						textData.setText(temp_title);

						setPreviewText(previewText);
					}
				});

				dlg.show();
			}
		});

		textData.setText(temp_title);

		CheckBox textEnabled = (CheckBox)findViewById(R.id.button_menu_textenabled);
		textEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				temp_textEnabled = isChecked;
				previewText.setVisibility(isChecked?View.VISIBLE:View.INVISIBLE);
			}
		});

		textEnabled.setChecked(temp_textEnabled);

	    final GradientDrawable drawable = new GradientDrawable();
	    drawable.setShape(GradientDrawable.RECTANGLE);
		drawable.setColor(temp_textColor);
	    drawable.setStroke(5, Color.LTGRAY);

		final Button textColor = (Button)findViewById(R.id.button_menu_textcolor);
		textColor.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ColorPicker dlg = new ColorPicker(getContext(), temp_textColor);
				dlg.setCaption("common_textcolor");
				dlg.setOnColorPickListener(new ColorPicker.ColorPickListener()
				{
					@Override
					public void onPick(ColorPickerItem selected)
					{
						temp_textColor = selected.getColor();

						drawable.setColor(temp_textColor);
						AppGlobal.setBackgroundDrawable(textColor, drawable);

						setPreviewText(previewText);
					}
				});

				dlg.show();
			}
		});

		AppGlobal.setBackgroundDrawable(textColor, drawable);

		setPreviewText(previewText);
	}

	private void loadOpacitySettings()
	{
		final TextView opacityValue = (TextView) findViewById(R.id.button_menu_opacity_value);
		opacityValue.setText("" + temp_opacity);

		final SeekBar opacity = (SeekBar) findViewById(R.id.button_menu_opacity);
		opacity.setMax(255);
		opacity.setProgress(temp_opacity);

		opacity.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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
				temp_opacity = progress;
				opacityValue.setText("" + temp_opacity);
				gradBgrColor.setAlpha(temp_opacity);
				AppGlobal.setBackgroundDrawable(backgroundColor, gradBgrColor);
			}
		});

		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
				case R.id.button_menu_opacity_minus:
				{
					if (temp_opacity == 0)
						return;

					temp_opacity--;
					break;
				}
				case R.id.button_menu_opacity_plus:
				{
					if (temp_opacity == 255)
						return;

					temp_opacity++;
					break;
				}
				}

				opacity.setProgress(temp_opacity);
				opacityValue.setText("" + temp_opacity);
			}
		};

		ImageButton zoom = (ImageButton) findViewById(R.id.button_menu_opacity_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_opacity_plus);
		zoom.setOnClickListener(buttonClick);
	}

	private void loadBackgroundImageSettings()
	{
		final ImageView image = (ImageView) findViewById(R.id.button_menu_backgroundtexture);
		final ImageView preview = (ImageView) findViewById(R.id.button_menu_buttonimagebackgroundtexture);

		image.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				uiImageViewer viewer = new uiImageViewer(getContext());
				viewer.setCaption("imgview_caption_choose_backgrimage");
				viewer.loadBackgrounds();

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener()
				{
					@Override
					public void onPick(ImageViewerItem selected)
					{
						Bitmap bitmap = AppGlobal.getBitmapFromImageViewerItem(selected);
						temp_bgrBitmapName = selected.getName();

						image.setImageBitmap(bitmap);
						preview.setImageBitmap(bitmap);
					}
				});

				viewer.show();
			}
		});

		temp_bgrBitmapOpacity = button.getBackgroundBitmapTransparency();
		temp_bgrBitmapName = button.getBackgroundBitmap();
		temp_bgrBitmapEnabled = button.isBackgroundBitmapEnabled();

		Bitmap bitmap = AppGlobal.getBgrBitmapFromWidget(button, true);

		image.setImageBitmap(bitmap);
		preview.setImageBitmap(bitmap);

		AppGlobal.setAlpha(preview, temp_bgrBitmapOpacity);

		CheckBox enabled = (CheckBox)findViewById(R.id.button_menu_backgroundtexture_enabled);
		enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				temp_bgrBitmapEnabled = isChecked;

				preview.setVisibility((temp_bgrBitmapEnabled) ? View.VISIBLE : View.INVISIBLE);
			}
		});

		enabled.setChecked(temp_bgrBitmapEnabled);

		preview.setVisibility((temp_bgrBitmapEnabled) ? View.VISIBLE : View.INVISIBLE);

		final TextView opacityValue = (TextView) findViewById(R.id.button_menu_backgroundimage_opacity_value);
		opacityValue.setText("" + temp_bgrBitmapOpacity);

		final SeekBar seek = (SeekBar) findViewById(R.id.button_menu_backgroundimage_opacity);
		seek.setMax(255);
		seek.setProgress(temp_bgrBitmapOpacity);

		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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
				temp_bgrBitmapOpacity = progress;

				AppGlobal.setAlpha(preview, temp_bgrBitmapOpacity);

				opacityValue.setText("" + temp_bgrBitmapOpacity);
			}
		});

		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
				case R.id.button_menu_backgroundimage_opacity_minus:
				{
					if (temp_bgrBitmapOpacity == 0)
						return;

					temp_bgrBitmapOpacity--;
					break;
				}
				case R.id.button_menu_backgroundimage_opacity_plus:
				{
					if (temp_bgrBitmapOpacity == 255)
						return;

					temp_bgrBitmapOpacity++;
					break;
				}
				}

				seek.setProgress(temp_bgrBitmapOpacity);

				AppGlobal.setAlpha(preview, temp_bgrBitmapOpacity);

				opacityValue.setText("" + temp_bgrBitmapOpacity);
			}
		};

		ImageButton zoom = (ImageButton) findViewById(R.id.button_menu_backgroundimage_opacity_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_backgroundimage_opacity_plus);
		zoom.setOnClickListener(buttonClick);
	}

	private void loadImageSettings()
	{
		final ImageView image = (ImageView) findViewById(R.id.button_menu_buttonimage);

		image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uiImageViewer viewer = new uiImageViewer(getContext());
				viewer.setCaption("imgview_caption_choose_image");
				viewer.loadImages();

				viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
					@Override
					public void onPick(ImageViewerItem selected) {
						Bitmap bitmap = AppGlobal.getBitmapFromImageViewerItem(selected);
						temp_bitmapName = selected.getName();

						image.setImageBitmap(bitmap);
					}
				});

				viewer.show();
			}
		});

		image.setImageBitmap(AppGlobal.getBitmapFromWidget(button, true));

		if (!button.getBitmapName().equals(""))
		{
			temp_bitmapOpacity = button.getBitmap().getTransparency();
			temp_bitmapName = button.getBitmap().getResourceName();
			AppGlobal.setAlpha(image, temp_bitmapOpacity);
		}

		final TextView pickImageInfo = (TextView)findViewById(R.id.button_menu_buttonimage_pickimagetext);

		CheckBox enabled = (CheckBox)findViewById(R.id.button_menu_buttonimage_enabled);
		enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				temp_bitmapEnabled = isChecked;

				if (!temp_bitmapEnabled) {
					image.setVisibility(View.INVISIBLE);
					pickImageInfo.setVisibility(View.INVISIBLE);
				} else {
					image.setVisibility(View.VISIBLE);
					pickImageInfo.setVisibility(View.VISIBLE);
				}
			}
		});

		enabled.setChecked(temp_bitmapEnabled);

		if (!temp_bitmapEnabled)
		{
			image.setVisibility(View.INVISIBLE);
			pickImageInfo.setVisibility(View.INVISIBLE);
		}
		else
		{
			image.setVisibility(View.VISIBLE);
			pickImageInfo.setVisibility(View.VISIBLE);
		}

		final TextView opacityValue = (TextView) findViewById(R.id.button_menu_image_opacity_value);
		opacityValue.setText("" + temp_bitmapOpacity);

		final SeekBar seek = (SeekBar) findViewById(R.id.button_menu_image_opacity);
		seek.setMax(255);
		seek.setProgress(temp_bitmapOpacity);

		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
				// TODO Auto-generated method stub				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
				// TODO Auto-generated method stub				
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				temp_bitmapOpacity = progress;

				AppGlobal.setAlpha(image, temp_bitmapOpacity);

				opacityValue.setText("" + temp_bitmapOpacity);
			}
		});

		View.OnClickListener buttonClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
				case R.id.button_menu_image_opacity_minus:
				{
					if (temp_bitmapOpacity == 0)
						return;

					temp_bitmapOpacity--;
					break;
				}
				case R.id.button_menu_image_opacity_plus:
				{
					if (temp_bitmapOpacity == 255)
						return;

					temp_bitmapOpacity++;
					break;
				}
				}

				seek.setProgress(temp_bitmapOpacity);

				AppGlobal.setAlpha(image, temp_bitmapOpacity);

				opacityValue.setText("" + temp_bitmapOpacity);
			}
		};

		ImageButton zoom = (ImageButton) findViewById(R.id.button_menu_image_opacity_minus);
		zoom.setOnClickListener(buttonClick);

		zoom = (ImageButton) findViewById(R.id.button_menu_image_opacity_plus);
		zoom.setOnClickListener(buttonClick);

		ImageButton customImage = (ImageButton)findViewById(R.id.button_menu_buttonimagecustom);
		customImage.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Storages.onDrivePick(context, new Storages.onDrivePickListener()
				{
					@Override
					public void onPick(String drive)
					{
						final FileBrowser fb = new FileBrowser(context, drive, new String[] {".png", ".jpg", ".jpeg", ".bmp"});
						fb.showPicturesPreview = true;
						fb.setCaption("fb_caption_find_image");

						fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener()
						{
							@Override
							public void onPick(String selected)
							{
								try
								{
									//allow only images <= 192x192							
									BitmapFactory.decodeFile(selected, AppGlobal.imageHeaderOptions);

									if ((AppGlobal.imageHeaderOptions.outWidth > 192) || (AppGlobal.imageHeaderOptions.outHeight > 192))
									{
										MessageInfo.info("msg_too_big_image");
										return;
									}

									File src = new File(selected);
									File dest = new File(AppGlobal.currentGameImagesPath);

									if (!dest.exists())
										dest.mkdir();

									String requiredName = null;

									if (!src.getName().toLowerCase().startsWith("user_"))
									{
										requiredName = "user_" + src.getName();
									}
									else
										requiredName = src.getName();

									dest = new File(AppGlobal.currentGameImagesPath + requiredName);

									//-------
									if (dest.exists())
									{
										dest = new File(AppGlobal.currentGameImagesPath + src.getName());

										fb.dismiss();

										FileBrowserCopyFile cpy = new FileBrowserCopyFile(context, src, dest);
										cpy.setOnCopyFileListener(new FileBrowserCopyFile.OnCopyFileListener()
										{
											@Override
											public boolean onCopy(boolean overwrite, File source, File destination)
											{
												File dest;

												if (!destination.getName().startsWith("user_"))
												{
													dest = new File(AppGlobal.currentGameImagesPath + "user_" + destination.getName());
												}
												else
													dest = destination;

												if ((!overwrite) && (dest.exists()))
												{
													MessageInfo.info("msg_file_exists");
													return false;
												}

												if (Files.fileCopy(source, dest))
												{
													temp_bitmapName = dest.getName();
													image.setImageBitmap(BitmapFactory.decodeFile(dest.getAbsolutePath()));

													MessageInfo.info("msg_image_added_to_collection");

													return true;
												}

												return false;
											}
										});

										cpy.show();
									}
									else
									{
										if (Files.fileCopy(src, dest))
										{
											temp_bitmapName = requiredName;
											image.setImageBitmap(BitmapFactory.decodeFile(selected));

											MessageInfo.info("msg_image_added_to_collection");
										}
									}
								}
								catch (Exception exc)
								{

								}
							}
						});

						fb.setOnFileValidationEvent(new FileBrowser.OnFileValidationListener()
						{
							@Override
							public FileBrowserItem onValidation(File file)
							{
								try
								{
									BitmapFactory.decodeFile(file.getAbsolutePath(), AppGlobal.imageHeaderOptions);

									if ((AppGlobal.imageHeaderOptions.outWidth > 0) &&
										(AppGlobal.imageHeaderOptions.outHeight > 0) &&
										(AppGlobal.imageHeaderOptions.outWidth <= 192) &&
										(AppGlobal.imageHeaderOptions.outHeight <= 192))
									{
										FileBrowserItem item = new FileBrowserItem(file);
										item.isPictureFile = true;

										return item;
									}
								}
								catch(Exception exc){
								}

								return null;
							}
						});

						fb.show();
					}
				});
			}
		});
	}

	private void solveRunAtStartFlag(Widget widget) {
		if (runAtStart) {
			EmuConfig.startupWidgetEnabled = true;
			EmuConfig.startupWidgetID = widget.getName();
		} else {
			if (EmuConfig.startupWidgetID != null && EmuConfig.startupWidgetID.equals(widget.getName())) {
				EmuConfig.startupWidgetEnabled = false;
			}
		}
	}

	private void saveKeySettings()
	{
		KeyWidget key = (KeyWidget) button;

		temp_key1.copyTo(key.getDesignKey(0));
		temp_key2.copyTo(key.getDesignKey(1));

		key.setMultiTapDelay(temp_multiTapDelay);
		key.setToggle(temp_key_toggle);

		key.setActiveKeys();

		solveRunAtStartFlag(key);
	}

	private void saveTouchSettings()
	{
		TouchMouseAction ta = (TouchMouseAction) button;

		temp_touchAction.copyTo(ta);
	}

	private void saveMouseTypeSettings()
	{
		((MouseTypeAction) button).setMouseTypeDuration(temp_mouseTypeAction.mouse_type_duration);
	}

	private void saveDpadSettings()
	{
		temp_dpad.copyTo(button);

		button.update();
	}

	private void saveFolderSettings()
	{
		temp_folder.copyTo(button);
	}

	private void saveJournalSettings()
	{
		/*if (button.getJournal() == null)
		{
			button.setJournal(temp_journal);
		}
		else
		{
			temp_journal.copyTo(button.getJournal());			
		}
		
		button.getJournal().init();	*/
	}

	private void saveWalkthroughSettings()
	{
		temp_walkthrough.copyTo(((Walkthrough) button));
	}

	private void saveComboSettings()
	{
		temp_combo.copyTo(button);

		solveRunAtStartFlag(button);
	}

	private void savePointClickSettings()
	{
		temp_pointclick.copyTo(button);
	}

	private void saveSpecialActionSettings()
	{
		temp_specialAction.copyTo(button);
	}

	private void saveSwitcherSettings()
	{
		temp_switcher.copyTo(button);
	}

	private void saveJoyButtonSetting()
	{
		temp_joybutton.copyTo(button);
	}

	private void saveZoomWidgetSetting()
	{
		temp_zoomWidget.copyTo(button);
	}

	private void loadSettings()
	{
		switch (currentType)
		{
		case key:
		{
			loadKeySettings();
			break;
		}
		case touch_action:
		{
			loadTouchSettings();
			break;
		}
		case mouse_type:
		{
			loadMouseTypeSettings();
			break;
		}
		case dpad:
		{
			loadDpadSettings();
			break;
		}
		case folder:
		{
			loadFolderSettings();
			break;
		}
		case journal:
		{
			loadJournalSettings();
			break;
		}
		case walkthrough:
		{
			loadWalkthroughSettings();
			break;
		}
		case combo:
		{
			loadComboSettings();
			break;
		}
		case point_click:
		{
			loadPointClickSettings();
			break;
		}
		case special:
		{
			loadSpecialActionSettings();
			break;
		}
		case switcher:
		{
			loadSwitcherSettings();
			break;
		}
		case joybutton:
		{
			loadJoyButtonSettings();
			break;
		}
		case zoom:
		{
			loadZoomWidgetSettings();
			break;
		}
		default:
			break;
		}

		//text
		temp_title = button.getText();
		temp_textColor = button.getTextData().getTextColor();
		temp_textAlign = button.getTextData().getTextAlign();
		temp_textOpacity = button.getTextData().getTransparency();
		temp_textAntialiasing = button.getTextData().isAntiAliasOn();
		temp_textFont = button.getTextData().getFont();

		if (temp_textFont != null && ExternalFonts.getFont(temp_textFont) == null)
		{
			temp_textFont = null;
		}

		temp_textEnabled = button.isTextEnabled();
		
		//image
		temp_bitmapEnabled = button.isBitmapEnabled();
				
		//background
		temp_bgrColorEnabled = button.isBackgroundColorEnabled();
		temp_backgroundColor = button.backgroundColor;
		temp_bgrBitmapEnabled = button.isBackgroundBitmapEnabled();
		temp_opacity = button.getTransparency();
		
		//general
		temp_isUndetectable = button.isUndetectable();
		temp_isVisible = button.isVisible();
		temp_isTappableOnly = button.isOnlyTappable();
		temp_deactOnLeave = button.isDeactivatedOnLeave;
		temp_synfeed = button.doSynapticFeedback();
		
		loadTitleSettings();
		loadOpacitySettings();
		loadImageSettings();
		loadBackgroundImageSettings();
		
		gradBgrColor = new GradientDrawable();
		gradBgrColor.setShape(GradientDrawable.RECTANGLE);
		gradBgrColor.setAlpha(temp_opacity);
		gradBgrColor.setColor(temp_backgroundColor);

		backgroundColor = (ImageView) findViewById(R.id.button_menu_buttonimagebackgroundcolor);
		AppGlobal.setBackgroundDrawable(backgroundColor, gradBgrColor);

		final GradientDrawable drawable = new GradientDrawable();
		drawable.setShape(GradientDrawable.RECTANGLE);
		drawable.setColor(temp_backgroundColor);
		drawable.setStroke(2, Color.LTGRAY);

		Button background = (Button) findViewById(R.id.button_menu_backgroundcolor);
		AppGlobal.setBackgroundDrawable(background, drawable);

		background.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ColorPicker dlg = new ColorPicker(getContext(), temp_backgroundColor);
				dlg.setCaption("common_bckcolor");
				dlg.setOnColorPickListener(new ColorPicker.ColorPickListener()
				{
					@Override
					public void onPick(ColorPickerItem selected)
					{
						temp_backgroundColor = selected.getColor();
						drawable.setColor(temp_backgroundColor);
						gradBgrColor.setColor(temp_backgroundColor);
					}
				});

				dlg.show();
			}
		});
			
		CompoundButton.OnCheckedChangeListener cbxListener = new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton v, boolean isChecked)
			{
				switch (v.getId())
				{				
					case R.id.button_menu_backgroundcolor_enabled:
					{
						temp_bgrColorEnabled = isChecked;
						backgroundColor.setVisibility(isChecked?View.VISIBLE:View.INVISIBLE);
						break;
					}
					case R.id.button_menu_undetectable:
					{
						temp_isUndetectable = isChecked;
						break;
					}
					case R.id.button_menu_visible:
					{
						temp_isVisible = isChecked;
						break;
					}
					case R.id.button_menu_onlyTappable:
					{
						temp_isTappableOnly = isChecked;
						break;
					}
					case R.id.button_menu_deactivate_on_leave:
					{
						temp_deactOnLeave = isChecked;
						break;
					}
					case R.id.button_menu_synfeed:
					{
						temp_synfeed = isChecked;
						break;
					}
				}				
			}
		};
		
		CheckBox cbx = (CheckBox)findViewById(R.id.button_menu_backgroundcolor_enabled);
		cbx.setOnCheckedChangeListener(cbxListener);		
		cbx.setChecked(temp_bgrColorEnabled);
				
		cbx = (CheckBox) findViewById(R.id.button_menu_undetectable);
		cbx.setOnCheckedChangeListener(cbxListener);
		cbx.setChecked(temp_isUndetectable);

		cbx = (CheckBox) findViewById(R.id.button_menu_visible);
		cbx.setOnCheckedChangeListener(cbxListener);		
		cbx.setChecked(temp_isVisible);

		cbx = (CheckBox) findViewById(R.id.button_menu_onlyTappable);
		cbx.setOnCheckedChangeListener(cbxListener);		
		cbx.setChecked(temp_isTappableOnly);		

		cbx = (CheckBox) findViewById(R.id.button_menu_deactivate_on_leave);
		cbx.setOnCheckedChangeListener(cbxListener);		
		cbx.setChecked(temp_deactOnLeave);

		cbx = (CheckBox) findViewById(R.id.button_menu_synfeed);
		cbx.setOnCheckedChangeListener(cbxListener);
		cbx.setChecked(temp_synfeed);
		
		ImageButton confirm = (ImageButton) findViewById(R.id.button_menu_confirm);
		confirm.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (currentType)
				{
				case key:
				{
					saveKeySettings();
					break;
				}
				case touch_action:
				{
					saveTouchSettings();
					break;
				}
				case mouse_type:
				{
					saveMouseTypeSettings();
					break;
				}
				case dpad:
				{
					saveDpadSettings();
					break;
				}
				case folder:
				{
					saveFolderSettings();
					break;
				}
				case journal:
				{
					saveJournalSettings();
					break;
				}
				case walkthrough:
				{
					saveWalkthroughSettings();
					break;
				}
				case combo:
				{
					saveComboSettings();
					break;
				}
				case point_click:
				{
					savePointClickSettings();
					break;
				}
				case special:
				{
					saveSpecialActionSettings();
					break;
				}
				case switcher:
				{
					saveSwitcherSettings();
					break;
				}
				case joybutton:
				{
					saveJoyButtonSetting();
					break;
				}
				case zoom:
				{
					saveZoomWidgetSetting();
					break;
				}
				default:
					break;
				}

				button.setType(currentType);
								
				button.setBitmapEnabled(temp_bitmapEnabled);
				button.setBitmap(temp_bitmapName);

				if (button.getBitmap() != null)
				{
					button.getBitmap().setTransparency(temp_bitmapOpacity);
					
					if (!temp_bitmapEnabled)
					{
						button.getBitmap().clear();
					}					
				}

				button.setBackgroundBitmap(temp_bgrBitmapName);
				button.setBackgroundBitmapEnabled(temp_bgrBitmapEnabled);
				button.setBackgroundBitmapTransparency(temp_bgrBitmapOpacity);
				button.setVisible(temp_isVisible);

				button.setUndetectable(temp_isUndetectable);
				button.setNonLayout(temp_isUndetectable);
				button.setDeativateOnLeave(temp_deactOnLeave);
				button.setSynapticFeedback(temp_synfeed);

				button.setOnlyTappable(temp_isTappableOnly);
				button.setBgrColorEnabled(temp_bgrColorEnabled);
				button.setBackgroundColor(temp_backgroundColor);
				button.setTextEnabled(temp_textEnabled);
				button.setTransparency(temp_opacity);
				button.getTextData().setTextColor(temp_textColor);
				button.getTextData().setTextAlign(temp_textAlign);
				button.getTextData().setTransparency(temp_textOpacity);
				button.getTextData().setAntiAlias(temp_textAntialiasing);
				button.getTextData().setFont(temp_textFont);
				button.setText(temp_title);
				button.resetInnerElementList();
				button.update();
			
				self.dismiss();
			}
		});
	}

	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.button_menu_lookandfeel_section_title,   "common_lookandfeel");
		localize(R.id.button_menu_lookandfeel_image_section,   "widget_edit_header_lookandfeel_image_caption");
		
		localize(R.id.button_menu_lookandfeel_image_preview,   "widget_edit_header_lookandfeel_image_preview");
		localize(R.id.button_menu_buttonimage_pickimagetext,   "widget_edit_header_lookandfeel_image_pick");		
		localize(R.id.button_menu_image_background_caption,    "widget_edit_header_lookandfeel_imgbackgroud_caption");		
		localize(R.id.button_menu_image_backgroundimg_caption, "widget_edit_header_lookandfeel_imgbackgroudimg_caption");
		
		localize(R.id.button_menu_image_opacity_text,               "common_opacity");
		localize(R.id.button_menu_lookandfeel_opacity_image,         "widget_edit_header_lookandfeel_opacity_image");
		localize(R.id.button_menu_lookandfeel_opacity_background,    "widget_edit_header_lookandfeel_opacity_background");
		localize(R.id.button_menu_lookandfeel_opacity_backgroundimg, "widget_edit_header_lookandfeel_opacity_backgroundimg");
		
		localize(R.id.button_menu_lookandfeel_text_caption,     "widget_edit_header_lookandfeel_text_caption");
		localize(R.id.button_menu_lookandfeel_general_caption,  "widget_edit_header_lookandfeel_general_caption");
		localize(R.id.button_menu_undetectable,                 "widget_edit_header_lookandfeel_general_undetectable");
		localize(R.id.button_menu_visible,                      "widget_edit_header_lookandfeel_general_visible");
		localize(R.id.button_menu_onlyTappable,                 "widget_edit_header_lookandfeel_general_taponly");		
		localize(R.id.button_menu_deactivate_on_leave,          "widget_edit_header_lookandfeel_general_deactivate_on_leave");
		localize(R.id.button_menu_synfeed,          "common_synapticfeedback");
		localize(R.id.button_menu_lookandfeel_specific_caption, "widget_edit_header_lookandfeel_specific_caption");
	}
	
	public uiButtonMenuDialog(final Context context, Widget widget)
	{
		super(context, widget);

		this.context = context;
		this.self = this;

		currentType = button.getType();

		inflater = LayoutInflater.from(context);

		setContentView(R.layout.button_menu_dialog);

		mainScroll = (ScrollView) findViewById(R.id.button_menu_mainscrollview);

		loadSettings();
	}

	public Widget getButton()
	{
		return button;
	}

	public boolean isButtonDead()
	{
		return deleteButton;
	}

	@Override
	public void show()
	{
		if (currentType == WidgetType.point_click)
		{
			Button button = (Button) findViewById(R.id.button_menu_pointclick_find);

			if (button != null)
			{
				button.setText("[" + (int) temp_pointclick.getX() + "," + (int) temp_pointclick.getY() + "]");
			}
		}

		super.show();
	}
}