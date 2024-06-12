/*
 *  Copyright (C) 2011-2012 Locnet (android.locnet@gmail.com)
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Locale;

import magiclib.CrossSettings;
import magiclib.Global;
import magiclib.IO.SAFSupport;
import magiclib.controls.Dialog;
import magiclib.controls.HelpViewer;
import magiclib.core.CrashTest;
import magiclib.core.EmuResources;
import magiclib.core.EmuConfig;
import magiclib.core.EmuManager;
import magiclib.core.EmuSignal;
import magiclib.core.NativeOption;
import magiclib.core.NavigationCursor;
import magiclib.core.Screen;
import magiclib.core.Align;
import magiclib.dosbox.AbsoluteMouseFixType;
import magiclib.dosbox.DosboxConfig;
import magiclib.dosbox.FrameSkipDialog;
import magiclib.dosbox.Input;
import magiclib.dosbox.SpecialKeysDialog;
import magiclib.graphics.EmuVideo;
import magiclib.gui_modes.WidgetConfigurationDialog;
import magiclib.keyboard.*;
import magiclib.layout.widgets.Widget;
import magiclib.locales.Localization;
import magiclib.logging.Log;
import magiclib.mapper.Mapper;
import magiclib.mapper.Tilt;
import magiclib.mouse.MouseType;

public class MagicLauncher extends Activity
{		
	private boolean not_permitted_start = false;
    private boolean firstResume = true;
	private boolean dbxKeyboardShown;
	private boolean isNavigationCursorShown;
	
	public static native void nativeMouseMax(boolean enabled, int max_width, int max_height);
	public static native void nativeSetAbsoluteMouseType(int type);
	public static native void nativeMouseRoundMaxByVideoMode(boolean enabled);
	public static native int nativeGetMouseVideoWidth();
	public static native int nativeGetMouseVideoHeight();	
	public static native void nativeSaveState(String path);
	public static native void nativeLoadState(String path);	
	public static native void nativeInit();
	public static native void nativeShutDown();
	public static native void nativeSetOption(int option, int value, String value2);
	public native void nativeStart(Buffer videoBuffer, int width, int height, String safPath, int version);
	public static native void nativePause(int state);
	public static native void nativeStop();
	public static native int nativeGetLibArchitecture();
	public static native void nativeForceStop();
	
	public uiAudio mAudioDevice = null;
	public DosBoxThread mDosBoxThread = null;		
	public boolean mTurboOn = false;

	private static SpecialKeysDialog special_keys = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		if (!CrashTest.isSecondPrepared())
		{
			not_permitted_start = true;
			return;
		}
		
		AppGlobal.context = this;
		dbxKeyboardShown = false;
		
		//System.loadLibrary("magiclib");

		if (AppGlobal.isArmNeon())
		{
			System.loadLibrary("dosbox_neon");
			//Toast.makeText(this, "loading x86", Toast.LENGTH_LONG).show();
		}
		else
		{
			System.loadLibrary("dosbox");
		}

		DosboxVideo surface = new DosboxVideo(this);
		surface.setKeepScreenOn(true);
		setContentView(surface);
		registerForContextMenu(surface);

		EmuVideo.surface = surface;

		String gameID = getIntent().getStringExtra("intent_msg1");
		AppGlobal.setupCurrentGame(this, gameID);

		BitmapDrawable splash = (BitmapDrawable) getResources().getDrawable(R.drawable.splash);
		splash.setTargetDensity(120);
		splash.setGravity(Gravity.CENTER);		
		AppGlobal.setBackgroundDrawable(EmuVideo.surface, splash);

		EmuManager.init(getOnLayoutInitEvent());
	}

	private EmuManager.onEmuManagerEventListener getOnLayoutInitEvent() {
		return new EmuManager.onEmuManagerEventListener() {
			@Override
			public void onInit() {
				mAudioDevice = new uiAudio(MagicLauncher.this);
				nativeInit();

//				Log.log("MagicLauncher thread [" + Thread.currentThread().getId() + "]");
				DosboxConfig.load();
				final DosboxConfig config = DosboxConfig.config;
				mAudioDevice.useAndroidAudioHack = config==null?true:config.useAndroidAudioHack();
				loadCustomResources();

				if (config!=null && config.ipxEnabled && config.ipxAskAtStart) {
					IPXSettings d = new IPXSettings(true, config.ipxEnabled, true, config.ipxClientOn, config.ipxServerPort, config.ipxClientToIP, config.ipxClientToPort);
					d.setOnIPXStartEventListener(new IPXSettings.OnIPXStartEventListener() {
						@Override
						public void onSave(boolean enabled, boolean changed, boolean skip, boolean ask, boolean clientOn, int serverPort, String clientToIp, int clientToPort) {
							if (!skip && changed) {
								config.ipxEnabled = enabled;
								config.ipxAskAtStart = ask;
								config.ipxClientOn = clientOn;
								config.ipxServerPort = serverPort;
								config.ipxClientToIP = clientToIp;
								config.ipxClientToPort = clientToPort;

								Serializer serializer = new Persister();
								File file = new File(AppGlobal.currentGameConfigFile);

								try {
									serializer.write(config, file);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							config.ipxSkipped = skip;

							setupAndStartDosbox();
							resumeDosbox();
						}
					});

					d.show();
				} else {
					setupAndStartDosbox();
				}
			}

			@Override
			public boolean onPause() {
				if (!isDosboxPaused())
				{
					pauseDosBox(true);
				}

				return true;
			}

			@Override
			public boolean onUnPause() {
				if (isDosboxPaused())
				{
					pauseDosBox(false);
				}
				return  true;
			}

			@Override
			public void onQuit() {
				if (EmuSignal.isEmuQuitStarted()) {
					return;
				}

				final Dialog d = new Dialog(Global.context);
				d.setContentView(R.layout.quit);
				d.setCaption("Magic DOSBox");
				d.setSize(260, ViewGroup.LayoutParams.WRAP_CONTENT);

				View.OnClickListener onClick = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						d.dismiss();
						if (v.getId() == R.id.confirm) {
							stopDosBox();
						}
					}
				};

				((TextView)d.getView().findViewById(R.id.message)).setText(Localization.getString("msg_turn_off"));
				d.getView().findViewById(R.id.confirm).setOnClickListener(onClick);
				d.getView().findViewById(R.id.close).setOnClickListener(onClick);
				d.show();
			}

			@Override
			public void onForceQuit() {
				doExit();
				nativeForceStop();
			}

			@Override
			public void onNativeOption(NativeOption option, int value, String value2) {
				switch (option) {
					case swapInNextDisk:{
						nativeSetOption(15, value, null);
						break;
					}
					case cycles:{
						nativeSetOption(10, value, null);
						break;
					}
					case frameskip:{
						nativeSetOption(11, value, null);
						break;
					}
					case unlockSpeed:{
						unlockSpeed();
						break;
					}
					case showSpecialKeys:{
						showSpecialKeys();
						break;
					}
				}
			}

			@Override
			public void onMouseTypeChange(MouseType type) {
				if (type == MouseType.absolute) {
					nativeMouseRoundMaxByVideoMode(EmuConfig.roundAbsoluteByVideoMode);
				} else
					nativeMouseRoundMaxByVideoMode(false);
			}

			@Override
			public void onNewWidget(float x, float y) {
				uiAddWidget dialog = new uiAddWidget(Global.context, x, y);
				dialog.show();
			}

			@Override
			public WidgetConfigurationDialog onGetWidgetConfigurationWindow(Widget widget, boolean multi) {
				if (multi) {
					return new WidgetsMultiEditDialog(Global.context, widget);
				}
				return new uiButtonMenuDialog(Global.context, widget);
			}

			@Override
			public void onMainMenuShow() {
				uiSettingsDialog d = new uiSettingsDialog();
				d.show();
			}

			@Override
			public VirtualKeyboard onCreateSystemKeyboard() {
				return createDosboxKeyboard();
			}
		};
	}

	private void loadCustomResources() {
		EmuResources.clear();
		EmuResources.add("img_empty", R.drawable.img_empty);
		EmuResources.add("img_key", R.drawable.img_key);
		EmuResources.add("img_campfire", R.drawable.img_campfire);
		EmuResources.add("img_pouch", R.drawable.img_pouch);
		EmuResources.add("img_bag", R.drawable.img_bag);
		EmuResources.add("img_ghost", R.drawable.img_ghost);
		EmuResources.add("img_claws", R.drawable.img_claws);
		EmuResources.add("img_magic", R.drawable.img_magic);
		EmuResources.add("img_cross_02", R.drawable.img_cross_02);
		EmuResources.add("img_dummytarget", R.drawable.img_dummytarget);
		EmuResources.add("img_navigation", R.drawable.img_navigation);

		EmuResources.add("img_arrow_up", R.drawable.img_arrow_up);
		EmuResources.add("img_arrow_down", R.drawable.img_arrow_down);
		EmuResources.add("img_arrow_left", R.drawable.img_arrow_left);
		EmuResources.add("img_arrow_right", R.drawable.img_arrow_right);

		EmuResources.add("icon_level1", R.drawable.icon_level1);
		EmuResources.add("icon_level2", R.drawable.icon_level2);
		EmuResources.add("icon_widget_move", R.drawable.icon_widget_move);
		EmuResources.add("icon_widget_resize", R.drawable.icon_widget_resize);
		EmuResources.add("icon_innerselect", R.drawable.icon_innerselect);
		EmuResources.add("icon_align_top", R.drawable.icon_align_top);
		EmuResources.add("icon_align_left", R.drawable.icon_align_left);
		EmuResources.add("icon_align_width", R.drawable.icon_align_width);
		EmuResources.add("icon_align_height", R.drawable.icon_align_height);
		EmuResources.add("icon_align_horizontal_space", R.drawable.icon_align_horizontal_space);
		EmuResources.add("icon_align_vertical_space", R.drawable.icon_align_vertical_space);
		EmuResources.add("icon_clone_widget", R.drawable.icon_clone_widget);
		EmuResources.add("icon_multiple_select_disabled", R.drawable.icon_multiple_select_disabled);
		EmuResources.add("icon_cancel", R.drawable.icon_cancel);
		EmuResources.add("icon_edit", R.drawable.icon_edit);
		EmuResources.add("icon_multi_edit", R.drawable.icon_multi_edit);
		EmuResources.add("icon_multiple_select_enabled", R.drawable.icon_multiple_select_enabled);
		EmuResources.add("img_bag", R.drawable.img_bag);
		EmuResources.add("icon_drag", R.drawable.icon_drag);
		EmuResources.add("mtbar_bckr", R.drawable.img_wbgr_19);
		EmuResources.add("img_help", R.drawable.img_help);
	}

	private void showSpecialKeys() {
		if (special_keys == null)
		{
			special_keys =  new SpecialKeysDialog();
		}

		special_keys.showAtLocation(Screen.screenWidth >> 1 , Screen.screenHeight >> 1);
	}

	private VirtualKeyboard createDosboxKeyboard() {
		VirtualKeyboard pcKeyboard = new VirtualKeyboard(EmuVideo.surface,
				EmuConfig.dbxKeyboardOpacity,
				EmuConfig.dbxKeyboardBackgroundColor,
				EmuConfig.dbxKeyboardAlign,
				EmuConfig.dbxLandscapeType,
				EmuConfig.dbxPortraitType);
		pcKeyboard.setOnKeyEventListener(new VirtualKeyboard.onKeyEvent() {
			@Override
			public void onKeyDown(int keyCode) {
				Keyboard.sendEvent(keyCode, true, false, false, false);
			}

			@Override
			public void onKeyUp(int keyCode) {
				Keyboard.sendEvent(keyCode, false, false, false, false);
			}

			@Override
			public void onSettingsChanged(int opacity, int backgroundColor, Align align, VirtualKeyboardType landscapeLayout, VirtualKeyboardType portraitLayout) {
				EmuConfig.dbxKeyboardOpacity = opacity;
				EmuConfig.dbxKeyboardBackgroundColor = backgroundColor;
				EmuConfig.dbxKeyboardAlign = align;
				EmuConfig.dbxLandscapeType = landscapeLayout;
				EmuConfig.dbxPortraitType = portraitLayout;
			}
		});

		return pcKeyboard;
	}

    private void showHelp()
    {
		HelpViewer hlp = new HelpViewer("common_help", "help/tips/ingame/index.html", null, CrossSettings.showInGameHelp, false, true);
        hlp.setOnHelpEventListener(new HelpViewer.HelpEventListener() {
			@Override
			public void onStartEnable(boolean enabled) {
				CrossSettings.showInGameHelp = enabled;
				CrossSettings.save();
			}
		});
        hlp.show();
    }

	@Override
	protected void onDestroy() 
	{
		if (Log.DEBUG)  Log.log("onDestroy");

		if (!not_permitted_start)
		{
			stopDosBox();
			shutDownDosBox();
			EmuVideo.surface = null;
			/*if (mSurfaceView != null) {
				mSurfaceView.onPause();
				mSurfaceView = null;
			}*/
		}
		
		CrashTest.stopSecond();

		super.onDestroy();

		if (Log.DEBUG)  Log.log("/onDestroy");
	}

	private boolean tmpIsPaused = false;
	public volatile boolean isExiting = false;

	@Override
	protected void onPause() 
	{
		if (Log.DEBUG) Log.log("onPause " + Thread.currentThread().getId() + ", isExiting=" + isExiting);
		try {
			if (!not_permitted_start) {
				if (isExiting) {
					if (mAudioDevice != null) {
						mAudioDevice.pause();
						mAudioDevice = null;
					}

					if (EmuVideo.surface!=null) {
						EmuVideo.surface.onPause();
						EmuVideo.surface = null;
					}

					EmuManager.dispose();
				} else {
					tmpIsPaused = isDosboxPaused();

					if (!tmpIsPaused) {
						pauseDosBox(true);
					}
				}

				Input.stop();
				dbxKeyboardShown = EmuManager.isSystemKeyboardShown();
				EmuManager.disposeSystemKeyboard();

				if (NavigationCursor.enabled) {
					isNavigationCursorShown = NavigationCursor.isCursorShown;
					NavigationCursor.dispose();
				}
			}
		} catch (Exception exc) {
			if (Log.DEBUG)  Log.log("/onPause exception : " + ((exc == null)?"":exc.getMessage()));
		}

		super.onPause();
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		
		if (not_permitted_start)
		{		
			Intent intent = new Intent(this, uiGameStarterActivity.class);
			intent.putExtra("intent_msg1", getIntent().getStringExtra("intent_msg1"));

			finish();
			startActivity(intent);
		}
		else
		{
			Global.context = this;

			resumeDosbox();

			if (dbxKeyboardShown) {
				EmuSignal.sendShowInbuiltKeyboardMessage(1);
			}

			if (NavigationCursor.enabled && isNavigationCursorShown) {
				NavigationCursor.show();
			}
//			EmuSignal.sendAutomatedTestMessage();
		}
	}

	private void resumeDosbox()
	{
		if (mDosBoxThread == null)
			return;

		if (!tmpIsPaused)
			pauseDosBox(false);

		AppGlobal.lockScreenOrientation();
		AppGlobal.dimNavigationBar();

		if (firstResume) {
			firstResume = false;

			if (CrossSettings.showInGameHelp)
				showHelp();
		}

		if (EmuVideo.surface != null) {
			EmuVideo.surface.requestFocus();
			EmuVideo.surface.requestFocusFromTouch();
		}
	}

	public static void unlockSpeed() {
		MagicLauncher activity = ((MagicLauncher) AppGlobal.context);
		activity.mTurboOn = !activity.mTurboOn;
		MagicLauncher.nativeSetOption(17, activity.mTurboOn ? 1 : 0, null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);

		//AppGlobal.onDbxKeyboardConfigurationChanged();

		EmuManager.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		
		return AppGlobal.createOptionsMenu(menu);
	}
			
	@Override
	public boolean onOptionsItemSelected(MenuItem item)	
	{
		if (AppGlobal.optionsItemSelected(this, item))
			return true;
		
	    return super.onOptionsItemSelected(item);	    
	}	

	public boolean isDosboxPaused()
	{
		return (mDosBoxThread != null && !mDosBoxThread.mDosBoxRunning);
	}
	
	public void pauseDosBox(boolean pause) {
		if (pause) {
			mDosBoxThread.mDosBoxRunning = false;
			nativePause(1);
			if (mAudioDevice != null)
				mAudioDevice.pause();			
		}
		else {
			nativePause(0);
			mDosBoxThread.mDosBoxRunning = true;
			//will auto play audio when have data
			//if (mAudioDevice != null)
			//	mAudioDevice.play();		
		}
		
		if (Tilt.runs())
		{
			Tilt.isPaused = (mDosBoxThread==null || !mDosBoxThread.mDosBoxRunning);
		}
	}

	private void setupAndStartDosbox()
	{
		String dosbox_config = AppGlobal.currentGameTempPath + "dosbox.config";

		DosboxConfig.saveToFile(dosbox_config);
		MagicLauncher.nativeSetOption(5, 0, dosbox_config);

		Mapper.load();
		Keyboard.init();

		nativeSetOption(12, (EmuConfig.speedPatchR) ? 1 : 0, null);
		nativeSetOption(13, (EmuConfig.speedPatchC) ? 1 : 0, null);
		nativeSetOption(16, EmuConfig.mouse_type == MouseType.absolute ? 100 : EmuConfig.mouse_sensitivity, null);

		nativeMouseRoundMaxByVideoMode(EmuConfig.roundAbsoluteByVideoMode);
		nativeMouseMax(EmuConfig.mouse_max_enabled,
				EmuConfig.mouse_max_width,
				EmuConfig.mouse_max_height);

		if (EmuConfig.absFixType == null) {
			if (EmuConfig.microsoftMouseFix) {
				EmuConfig.absFixType = AbsoluteMouseFixType.msmouse;
			} else {
				EmuConfig.absFixType = AbsoluteMouseFixType.predefined;
			}
		}

		switch (EmuConfig.absFixType) {
			case msmouse: {
				nativeSetAbsoluteMouseType(1);
				break;
			}
			case settlr1: {
				nativeSetAbsoluteMouseType(2);
				break;
			}
			default:{
				nativeSetAbsoluteMouseType(0);
			}
		}

		mDosBoxThread = new DosBoxThread(this);
		//mDosBoxThread.setPriority(Thread.MAX_PRIORITY);

		startDosBox();

		//don't know whether one more handler will hurt, so abuse key handler
		EmuSignal.sendSplashMessage(1000);

		if (EmuConfig.startupWidgetEnabled && EmuConfig.startupWidgetID != null)
		{
			EmuSignal.sendStartWidgetMessage(2000);
		}
	}

	void shutDownDosBox() 
	{
		if (Log.DEBUG)  Log.log("shutDownDosBox");

		if (mDosBoxThread != null) {
			boolean retry;
			retry = true;
			while (retry) {
				try {
					mDosBoxThread.join();
					retry =	false;
				}
				catch (InterruptedException e) { // try again shutting down the thread
				}
			}

			nativeShutDown();

			if (mAudioDevice != null) {
				mAudioDevice.shutDownAudio();
				mAudioDevice = null;
			}

			mDosBoxThread = null;
			Input.stop();
		}


		if (Log.DEBUG)  Log.log("/shutDownDosBox");
	}	

	void startDosBox() 
	{
		if (mDosBoxThread != null)
			mDosBoxThread.start();
	}
	
	public void stopDosBox()
	{
		if (Log.DEBUG)  Log.log("stopDosBox " + Thread.currentThread().getId());
		try
		{
			nativePause(0);//it won't die if not running

			//stop audio AFTER above
			if (mAudioDevice != null) {
				mAudioDevice.pause();
				mAudioDevice = null;
			}
			Input.stop();
			if (EmuVideo.surface != null) {
				EmuVideo.surface.onPause();
				EmuVideo.surface = null;
			}

			EmuManager.dispose();

			EmuSignal.sendQuitMessage(2000);
			nativeStop();
		}
		catch(Exception exc) 
		{
			if (Log.DEBUG)
				Log.log("stopDosBox 1 exc : " + ((exc == null)? "" : exc.getMessage()));
		}

		if (Log.DEBUG)  Log.log("/stopDosBox");
	}

	public int callbackMkDir(String path, int checkDir)
	{
		return SAFSupport.mkdir(path, checkDir);
	}

	public int callbackRmDir(String path)
	{
		return SAFSupport.rmdir(path);
	}

	public int callbackRename(String oldName, String newName)
	{
		return SAFSupport.rename(oldName, newName);
	}

	public int callbackDeleteFile(String path, int fileExists)
	{
		return SAFSupport.delete(path, fileExists);
	}

	public int callbackFTruncateFile(String path, int size)
	{
		return -1;
	}

	public int callbackGetFileDescriptor(String path, boolean modeW, int fileExists)
	{
		return SAFSupport.getFD(path, modeW, fileExists);
	}

	public int callbackFileExists(String path, int param)
	{
		return SAFSupport.fileExists(path, param);
	}

	public void callbackKeybChanged(String keyb)
	{
		Keyboard.setLayout(keyb);
	}

	public void callbackExit()
	{
		EmuSignal.sendQuitMessage(1);
	}

	public void doExit() {
		if (Log.DEBUG)  Log.log("doExit " + Thread.currentThread().getId());

		EmuSignal.removeQuitMessage();

		if (mDosBoxThread != null) {
			mDosBoxThread.mDosBoxRunning = false;
		}

		if (EmuVideo.surface != null) {
			InputMethodManager imm = (InputMethodManager) AppGlobal.context.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(EmuVideo.surface.getWindowToken(), 0);
			}
		}

		if (AppGlobal.backToCollection())
		{
			if (Log.DEBUG)  Log.log("doExit back to collection");
			Intent intent = new Intent(MagicLauncher.this, uiGameStarterActivity.class);
			//intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}

		finish();

		if (Log.DEBUG)  Log.log("/doExit");
	}

	//video
	public void callbackVideoRedraw( int w, int h, int s, int e) 
	{
		//if (Log.DEBUG) Log.log("callbackVideoRedraw w,h[" + w + "," + h + "] s,e[" + s + "," + e + "]");
		try {
			if ((EmuVideo.surface == null) || (EmuVideo.surface.renderer == null)) {
				if (Log.DEBUG) Log.log("renderer is null 1");
				return;
			}

			EmuVideo surface = EmuVideo.surface;

			surface.renderer.mSrc_width = w;
			surface.renderer.mSrc_height = h;

			synchronized (surface.renderer.mDirty) {
				if (surface.renderer.mDirty) {
					surface.renderer.mStartLine = Math.min(surface.renderer.mStartLine, s);
					surface.renderer.mEndLine = Math.max(surface.renderer.mEndLine, e);
				} else {
					surface.renderer.mStartLine = s;
					surface.renderer.mEndLine = e;
				}

				//if (Log.DEBUG) Log.log("callbackVideoRedraw w,h[" + w + "," + h + "] s,e[" + s + "," + e + "][" +
				//		mSurfaceView.renderer.mStartLine + "," + mSurfaceView.renderer.mEndLine +
				//		"] diff[" + (mSurfaceView.renderer.mEndLine - mSurfaceView.renderer.mStartLine)  + "," + (e-s) + "]");

				surface.renderer.mDirty = true;
			}
			surface.requestRender();
		} catch (Exception exc) {
			if (Log.DEBUG)
				Log.log("callbackVideoRedraw exception : " + ((exc == null) ? "" : exc.getMessage()));
		}
	}
	
	public Bitmap callbackVideoSetMode( int w, int h) 
	{
		//if (Log.DEBUG) Log.log("callbackVideoSetMode w,h[" + w + "," + h + "]");
		if ((EmuVideo.surface == null) || (EmuVideo.surface.renderer == null))
		{
			if (Log.DEBUG) Log.log("renderer is null 2");
			return null;
		}

		EmuVideo surface = EmuVideo.surface;

		surface.renderer.mSrc_width = w;
		surface.renderer.mSrc_height = h;
		surface.renderer.resetScreen();

		surface.renderer.videoBuffer = null;
		surface.renderer.videoBuffer = ByteBuffer.allocateDirect(w * h * 2);
		surface.renderer.resize = true;

		return null;
	}	
	
	public void callbackMouseSetVideoMode(int mode, int width, int height)
	{
		if (Log.DEBUG) Log.log(String.format(Locale.getDefault(), "callbackMouseSetVideoMode %d,%d,%d ", mode, width, height));

		if (EmuManager.getMouseType() == MouseType.absolute)
		{
			EmuSignal.calibrateAbsoluteMouse(8000);
		}
	}

	public Buffer callbackVideoGetBuffer() 
	{		
		if ((EmuVideo.surface == null) || (EmuVideo.surface.renderer == null))
		{
			if (Log.DEBUG) Log.log("renderer is null 3");
			return null;
		}
		
		if (EmuVideo.surface != null)
		{
			return EmuVideo.surface.renderer.videoBuffer;
		}
		else
		{
			return null;
		}
	}
	
	//audio
	public int callbackAudioInit(int rate, int channels, int encoding, int bufSize) 
	{
		if (mAudioDevice != null)
			return mAudioDevice.initAudio(rate, channels, encoding, bufSize);
		else
			return 0;
	}
	
	public void callbackAudioWriteBuffer(int size) {
		if (mAudioDevice != null){			
			mAudioDevice.AudioWriteBuffer(size);
		}
	}

	public short[] callbackAudioGetBuffer() {
		if (mAudioDevice != null)
			return mAudioDevice.mAudioBuffer;
		else
			return null;
	}

	//dosbox
	class DosBoxThread extends Thread {
		MagicLauncher mParent;
		public boolean	mDosBoxRunning = false;

		DosBoxThread(MagicLauncher parent) {
			mParent =  parent;
		}
		
		public void run() 
		{
			mDosBoxRunning = true;
			
			int w = 640;
			int h = 400;

			int ver = 0;
			try {
				String version = AppGlobal.context.getPackageManager().getPackageInfo(AppGlobal.context.getPackageName(), 0).versionName;
				ver = Integer.parseInt(version.split("1.0.")[1]);
			} catch(Exception e) {
				if (Log.DEBUG) Log.log("Start failed parse application version");
			}
			nativeStart(EmuVideo.surface.renderer.videoBuffer, w, h, SAFSupport.isEnabled() ? SAFSupport.sdcardUriRealPath : null, ver);
			//nativeStart(mSurfaceView.renderer.videoBuffer, w, h, null);
			//will never return to here;
		}
	}
}
