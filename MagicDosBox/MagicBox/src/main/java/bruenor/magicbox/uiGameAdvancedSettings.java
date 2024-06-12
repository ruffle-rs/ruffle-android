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
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import magiclib.controls.Dialog;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.EmuConfig;
import magiclib.core.NativeCore;
import magiclib.core.ScreenOrientation;
import magiclib.dosbox.AbsoluteMouseFixType;
import magiclib.dosbox.Input;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetFinder;
import magiclib.layout.widgets.WidgetType;

public class uiGameAdvancedSettings extends Dialog
{
	private boolean speedPatchR;
	private boolean speedPatchC;
	private boolean blockAbsoluteMove;
	//private boolean microsoftMouseFix;
	private boolean roundAbsoluteByVideoMode;
	private boolean showGSettOnBackButton;
	private boolean dimNavigationBar;
	private ScreenOrientation orientationLock;
	private String startupWidgetID;
	private boolean startupWidgetEnabled;
	private AbsoluteMouseFixType absFixType;
	
	private boolean absManual = false;
	private boolean absoluteResEnabled;
	private int absoluteResWidth = -1;
	private int absoluteResHeight = -1;
	
	private TextView orientationLockValue;
	private List<ImageViewerItem> widgets;
	private ImageView atStart;
	private WidgetType[] atStartTypes = new WidgetType []
	{
		WidgetType.key,
		WidgetType.combo
	};
	private TextView atStartText;
	private TextView absFixValue;
	
	@Override
	public void onSetLocalizedLayout() 
	{
		localize(R.id.game_advanced_absmouse_caption,                   "advset_absmouse_caption");
		localize(R.id.game_advanced_absmouse_blockmove,                 "advset_absmouse_blockmove");
		localize(R.id.game_advanced_absmousefixes_caption,              "advset_absmouse_absmousefix");
		localize(R.id.game_advanced_mouse_caption,                      "advset_mouse_caption");
		localize(R.id.game_advanced_absmouse_manualcorrection_caption,  "advset_mouse_manualcorrection");
		localize(R.id.game_advanced_patch_caption,                      "advset_patch_caption");
		localize(R.id.game_advanced_speedpatch_r,                       "advset_patch_speedpatch_r");
		localize(R.id.game_advanced_speedpatch_c,                       "advset_patch_speedpatch_c");
		localize(R.id.game_advanced_others_caption,                     "advset_others_caption");
		localize(R.id.game_advanced_show_generalsettings_on_backbutton, "advset_others_showgensettings");
		localize(R.id.game_advanced_show_generalsettings_on_backbutton_hint, "advset_others_showgensettings_hint");
		localize(R.id.game_advanced_dim_navbar,                         "advset_others_dim_navbar");
		localize(R.id.game_advanced_license,                            "common_license");
		localize(R.id.game_advanced_orientation_caption,                "advset_orientation_caption");
		localize(R.id.game_advanced_orientation_other,                  "common_other");
		localize(R.id.game_advanced_atstartup_caption,                  "common_runatstart");
	}
	
	public uiGameAdvancedSettings(Context context)
	{
		super(context);
/*
		if (uiLog.DEBUG)
		{
			Toast.makeText(context, "GLES Ver : " + AppGlobal.glesVersion + "\n" +
		                            "GLSL Ver : " + AppGlobal.glslVersion, Toast.LENGTH_LONG).show();
		}*/
		
		speedPatchR = EmuConfig.speedPatchR;
		speedPatchC = EmuConfig.speedPatchC;
		blockAbsoluteMove = EmuConfig.blockAbsoluteMove;
		//microsoftMouseFix = EmuConfig.microsoftMouseFix;
		roundAbsoluteByVideoMode = EmuConfig.roundAbsoluteByVideoMode;
		showGSettOnBackButton = EmuConfig.showGSettOnBackButton;
		dimNavigationBar = EmuConfig.dimNavigationBar;
		orientationLock = EmuConfig.orientationLock;
		startupWidgetID = EmuConfig.startupWidgetID;
		startupWidgetEnabled = EmuConfig.startupWidgetEnabled;

		absoluteResEnabled = EmuConfig.mouse_max_enabled;
		absoluteResWidth = EmuConfig.mouse_max_width;
		absoluteResHeight = EmuConfig.mouse_max_height;

		absFixType = EmuConfig.absFixType;

		setContentView(R.layout.game_advanced_settings);
		setCaption("advset_caption");
		
		orientationLockValue = (TextView)findViewById(R.id.game_advanced_orientation_value);
		orientationLockValue.setText(orientationLock.getTitle());
		
		CompoundButton.OnCheckedChangeListener cbxListener = new CompoundButton.OnCheckedChangeListener()
		{			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				switch(buttonView.getId())
				{
					case R.id.game_advanced_absmouse_blockmove:
					{
						blockAbsoluteMove = !blockAbsoluteMove;
						break;
					}
					case R.id.game_advanced_speedpatch_c:
					{
						speedPatchC = !speedPatchC;	
						break;
					}
					case R.id.game_advanced_speedpatch_r:
					{
						speedPatchR = !speedPatchR;	
						break;
					}
					case R.id.game_advanced_absmouse_roundbyvideomode:
					{
						roundAbsoluteByVideoMode = !roundAbsoluteByVideoMode;	
						break;
					}	
					case R.id.game_advanced_show_generalsettings_on_backbutton:
					{
						showGSettOnBackButton = !showGSettOnBackButton;	
						break;
					}	
					case R.id.game_advanced_dim_navbar:
					{
						dimNavigationBar = !dimNavigationBar;
						break;
					}
					case R.id.game_advanced_atstartup_enabled:
					{
						startupWidgetEnabled = !startupWidgetEnabled;
						break;
					}
				}
			}
		};		
		
		CheckBox cbx = (CheckBox)findViewById(R.id.game_advanced_absmouse_blockmove);
		cbx.setChecked(blockAbsoluteMove);
		cbx.setOnCheckedChangeListener(cbxListener);
		
		cbx = (CheckBox)findViewById(R.id.game_advanced_speedpatch_c);		
		cbx.setChecked(speedPatchC);
		cbx.setOnCheckedChangeListener(cbxListener);
		
		cbx = (CheckBox)findViewById(R.id.game_advanced_speedpatch_r);		
		cbx.setChecked(speedPatchR);		
		cbx.setOnCheckedChangeListener(cbxListener);
			
		cbx = (CheckBox)findViewById(R.id.game_advanced_absmouse_roundbyvideomode);		
		cbx.setChecked(roundAbsoluteByVideoMode);		
		cbx.setOnCheckedChangeListener(cbxListener);		
		
		cbx = (CheckBox)findViewById(R.id.game_advanced_show_generalsettings_on_backbutton);		
		cbx.setChecked(showGSettOnBackButton);		
		cbx.setOnCheckedChangeListener(cbxListener);

		cbx = (CheckBox)findViewById(R.id.game_advanced_atstartup_enabled);
		cbx.setChecked(startupWidgetEnabled);
		cbx.setOnCheckedChangeListener(cbxListener);

		cbx = (CheckBox)findViewById(R.id.game_advanced_dim_navbar);		
		cbx.setChecked(dimNavigationBar);		
		cbx.setOnCheckedChangeListener(cbxListener);

		if (Build.VERSION.SDK_INT < 14)
		{
			cbx.setVisibility(View.GONE);
		}
		
		View.OnClickListener clickEvent = new View.OnClickListener()
		{					
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.game_advanced_orientation_minus:
					{
						minusOrientationLock();
						break;
					}
					case R.id.game_advanced_orientation_plus:
					{
						plusOrientationLock();
						break;
					}
					case R.id.game_advanced_absmousefixes_minus:
					{
						minusAbsFix();
						break;
					}
					case R.id.game_advanced_absmousefixes_plus:
					{
						plusAbsFix();
						break;
					}
				}
			}
		};
		
		ImageButton plusEvent;
		ImageButton minusEvent;

		minusEvent = (ImageButton)getView().findViewById(R.id.game_advanced_orientation_minus);
		minusEvent.setOnClickListener(clickEvent);

		plusEvent = (ImageButton)getView().findViewById(R.id.game_advanced_orientation_plus);
		plusEvent.setOnClickListener(clickEvent);

		minusEvent = (ImageButton)getView().findViewById(R.id.game_advanced_absmousefixes_minus);
		minusEvent.setOnClickListener(clickEvent);

		plusEvent = (ImageButton)getView().findViewById(R.id.game_advanced_absmousefixes_plus);
		plusEvent.setOnClickListener(clickEvent);

		View.OnClickListener onClick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.game_advanced_absmouse_manualcorrection:
					{
						uiMouseCorrection dlg = new uiMouseCorrection(getContext(), absoluteResEnabled, absoluteResWidth, absoluteResHeight);
						dlg.setOnMouseCorrectionEventListener(new MouseCorrectionEventListener()
						{
							@Override
							public void onChange(boolean enabled, int width, int height)
							{
								absManual = true;

								absoluteResEnabled = enabled;
								absoluteResWidth = width;
								absoluteResHeight = height;
							}
						});

						dlg.show();
						break;
					}
					case R.id.game_advanced_atstartup:
					{
						setStartupWidget();
						break;
					}
					case R.id.game_advanced_license:
					{
						uiLicense dlg = new uiLicense(getContext());
						dlg.load();
						dlg.show();
						break;
					}
					case R.id.game_advanced_ip:
					{
						MessageInfo.infoEx(AppGlobal.getIPAddress(true));
						break;
					}
				}
			}
		};

		ImageButton btn = (ImageButton)findViewById(R.id.game_advanced_absmouse_manualcorrection);
		btn.setOnClickListener(onClick);

		absFixValue = (TextView)findViewById(R.id.game_advanced_absmousefixes_value);
		setAbsFixTitle();

		atStartText = (TextView)findViewById(R.id.game_advanced_atstartup_text);

		atStart = (ImageView)findViewById(R.id.game_advanced_atstartup);
		atStart.setOnClickListener(onClick);

		if (startupWidgetID!=null) {
			WidgetFinder finder = new WidgetFinder();
			finder.setOnWidgetFinderEventListener(new WidgetFinder.WidgetFinderEventListener() {
				@Override
				public boolean onFind(Widget widget) {
					if (widget.getName().equals(startupWidgetID)) {
						updateStartupWidget(widget);
						return true;
					}

					return false;
				}
			});

			finder.get(atStartTypes, true, true);
		}

		Button button = (Button)findViewById(R.id.game_advanced_license);
		button.setOnClickListener(onClick);

		button = (Button)findViewById(R.id.game_advanced_ip);
		button.setOnClickListener(onClick);
		
		ImageButton save = (ImageButton)findViewById(R.id.game_advanced_confirm);
		save.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				EmuConfig.blockAbsoluteMove = blockAbsoluteMove;				
				
				if (absFixType != EmuConfig.absFixType)
				{
					EmuConfig.absFixType = absFixType;
					//MagicLauncher.nativeSetAbsoluteMouseType(EmuConfig.microsoftMouseFix ? 1 : 0);
					switch (EmuConfig.absFixType) {
						case msmouse: {
							MagicLauncher.nativeSetAbsoluteMouseType(1);
							break;
						}
						case settlr1: {
							MagicLauncher.nativeSetAbsoluteMouseType(2);
							break;
						}
						default:{
							MagicLauncher.nativeSetAbsoluteMouseType(0);
						}
					}
					//uiMouse.absoluteAclibrate();
				}				
				
				if (speedPatchR != EmuConfig.speedPatchR)
				{
					EmuConfig.speedPatchR = speedPatchR;
					MagicLauncher.nativeSetOption(12, (speedPatchR) ? 1 : 0, null);
				}
				
				if (speedPatchC != EmuConfig.speedPatchC)
				{
					EmuConfig.speedPatchC = speedPatchC;
					MagicLauncher.nativeSetOption(13, (speedPatchC) ? 1 : 0, null);
				}
				
				if ((absManual) || (roundAbsoluteByVideoMode != EmuConfig.roundAbsoluteByVideoMode))
				{
					EmuConfig.mouse_max_enabled = absoluteResEnabled;
					EmuConfig.mouse_max_width = absoluteResWidth;
					EmuConfig.mouse_max_height = absoluteResHeight;
					EmuConfig.roundAbsoluteByVideoMode = roundAbsoluteByVideoMode;
									
					MagicLauncher.nativeMouseMax(EmuConfig.mouse_max_enabled, 
							                     EmuConfig.mouse_max_width, 
							                     EmuConfig.mouse_max_height);
					
					MagicLauncher.nativeMouseRoundMaxByVideoMode(EmuConfig.roundAbsoluteByVideoMode);
				}
				/*
				if (roundAbsoluteByVideoMode != EmuConfig.roundAbsoluteByVideoMode)
				{
					EmuConfig.roundAbsoluteByVideoMode = roundAbsoluteByVideoMode;
					
					if (uiLayoutManager.getMouseType() == uiMouseType.absolute)
					{
						MagicLauncher.nativeMouseRoundMaxByVideoMode(EmuConfig.roundAbsoluteByVideoMode);
					}
					else
						MagicLauncher.nativeMouseRoundMaxByVideoMode(false);
				}*/
				
				if (showGSettOnBackButton != EmuConfig.showGSettOnBackButton)
				{
					EmuConfig.showGSettOnBackButton = showGSettOnBackButton;				
				}
				
				if (dimNavigationBar != EmuConfig.dimNavigationBar)
				{
					EmuConfig.dimNavigationBar = dimNavigationBar;				
				}
				
				boolean changedOrientationLock = (orientationLock != EmuConfig.orientationLock);
				
				if (changedOrientationLock)
				{
					EmuConfig.orientationLock = orientationLock;						
				}

				//if (startupWidgetID != EmuConfig.startupWidgetID)
				{
					EmuConfig.startupWidgetID = startupWidgetID;
				}

				if (startupWidgetEnabled != EmuConfig.startupWidgetEnabled)
				{
					EmuConfig.startupWidgetEnabled = startupWidgetEnabled;
				}

				dismiss();
				
				if (changedOrientationLock)
				{
					AppGlobal.lockScreenOrientation();
				}

				Input.mouseCalibration();
				AppGlobal.dimNavigationBar();
			}
		});
		
		TextView archType = (TextView)findViewById(R.id.game_advanced_archtype);
		String txt;
		
		
		switch (MagicLauncher.nativeGetLibArchitecture())
		{
			case 0: {txt = "ARM";break;}
			case 1: {
				txt = (NativeCore.nativeHasNEON()==1)?"ARM-Neon":"ARM-V7";
				break;
			}
			case 2: {txt = "X86";break;}
			default : {txt = "???";}
		}
		
		archType.setText(txt + " lib");

		TextView version = (TextView)findViewById(R.id.game_advanced_version);
		try {
			version.setText("v" + AppGlobal.context.getPackageManager().getPackageInfo(AppGlobal.context.getPackageName(), 0).versionName);
		} catch (Exception e) {
			e.printStackTrace();
			version.setText("");
		}
	}
	
	private void minusOrientationLock()
	{
		if (orientationLock == ScreenOrientation.rotate)
		{
			return;
		}
		
		if (orientationLock == ScreenOrientation.portrait)
		{
			orientationLock = ScreenOrientation.landscape;
		}
		else if (orientationLock == ScreenOrientation.landscape)
		{
			orientationLock = ScreenOrientation.rotate;
		}
		
		orientationLockValue.setText(orientationLock.getTitle());
	}
	
	private void plusOrientationLock()
	{
		if (orientationLock == ScreenOrientation.portrait)
		{
			return;
		}
		
		if (orientationLock == ScreenOrientation.rotate)
		{
			orientationLock = ScreenOrientation.landscape;
		}
		else if (orientationLock == ScreenOrientation.landscape)
		{
			orientationLock = ScreenOrientation.portrait;
		}
		
		orientationLockValue.setText(orientationLock.getTitle());		
	}

	private void minusAbsFix() {
		if (absFixType == AbsoluteMouseFixType.predefined) {
			return;
		}

		if (absFixType == AbsoluteMouseFixType.settlr1) {
			absFixType = AbsoluteMouseFixType.msmouse;
		} else {
			absFixType = AbsoluteMouseFixType.predefined;
		}

		setAbsFixTitle();
	}

	private void plusAbsFix() {
		if (absFixType == AbsoluteMouseFixType.settlr1) {
			return;
		}

		if (absFixType == AbsoluteMouseFixType.predefined) {
			absFixType = AbsoluteMouseFixType.msmouse;
		} else {
			absFixType = AbsoluteMouseFixType.settlr1;
		}

		setAbsFixTitle();
	}

	private void setAbsFixTitle() {
		switch (absFixType) {
			case predefined: {
				absFixValue.setText(Localization.getString("common_default"));
				break;
			}
			case msmouse:{
				absFixValue.setText("msmouse");
				break;
			}
			case settlr1:{
				absFixValue.setText("settlr1");
				break;
			}
		}
	}

	private void buildWidgetsList()
	{
		if (widgets == null)
		{
			widgets = new LinkedList<>();
			AppGlobal.addAvailableMappings(widgets, atStartTypes, true);
		}
	}

	private void setStartupWidget()
	{
		buildWidgetsList();

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
			public void onPick(ImageViewerItem selected) {
				updateStartupWidget(selected);
				startupWidgetID = ((Widget) selected.getTag()).getName();
			}
		});

		viewer.show();
	}

	private void updateStartupWidget(Widget widget)
	{
		updateStartupWidget(uiImageViewer.getImageViewerItemFromWidget(widget));
	}

	private void updateStartupWidget(ImageViewerItem item) {
		atStart.setImageBitmap(item.getImageBitmap());
		AppGlobal.setBackgroundDrawable(atStart, new BitmapDrawable(AppGlobal.context.getResources(), item.getBackgroundImageBitmap()));

		atStartText.setText(item.getDescription());
	}
}
