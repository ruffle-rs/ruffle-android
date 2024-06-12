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

import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.util.List;
import java.util.UUID;

import magiclib.Global;
import magiclib.IO.FileBrowser;
import magiclib.IO.Files;
import magiclib.IO.SAFSupport;
import magiclib.IO.Storages;
import magiclib.collection.CollectionActivity;
import magiclib.collection.CollectionFolder;
import magiclib.collection.CollectionItem;
import magiclib.controls.HelpViewer;
import magiclib.controls.ImageSize;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.CrashTest;
import magiclib.core.EStartProgram;
import magiclib.dosbox.DosboxConfig;
import magiclib.locales.Language;
import magiclib.logging.Log;
import magiclib.logging.MessageInfo;

public class uiGameStarterActivity extends CollectionActivity
{
	private uiGameFolderDialog.CollectionFolderEventListener collectionEvent;
	public GlobalSettings globalSettings;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (!init("MagicDosbox"))
		{
			finish();
			return;
		}

		setCollectionItemEvents();
	}

	@Override
	protected void onLoadCustomResources()
	{
		AppGlobal.isAndroidTV = false;
		AppGlobal.project = new ProjectSpecificData();

		AppGlobal.logo = R.drawable.logo_small;
		AppGlobal.folderImage = R.drawable.icon_elemspellbook;
		AppGlobal.fileImage = R.drawable.icon_spellbookpage;
		AppGlobal.empty_image = AppGlobal.getImageID("icon_noimage");
		AppGlobal.defaultBackgroundImage = "img_wbgr_01";
	}

	@Override
	protected void onLanguageLoad(Language language)
	{
		if (language == Language.german) {
			HelpViewer.setLocalization(Language.german);
		} else {
			HelpViewer.setLocalization(Language.english);
		}
	}

	@Override
	protected void clear()
	{
		super.clear();
	}

	@Override
	protected Intent getApplicationIntent()
	{
		return new Intent(getApplicationContext(), uiGameStarterActivity.class);
	}

	@Override
	protected void onCollectionItemClick(CollectionItem item)
	{
		startDosbox(item);
	}

	@Override
	protected void onCollectionItemLongClick(CollectionItem item)
	{
		switch (item.type)
		{
			case folder:
			{
				showFolderOptions((CollectionFolder)item);
				break;
			}
			case item:
			{
				showItemOptions(item);
				break;
			}
		}
	}

	@Override
	protected void addNewCollectionItem()
	{
		ImageViewer viewer = new ImageViewer();
		viewer.setCaption("common_add");

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				if ((!AppGlobal.isDonated) && (config.items.size() > 0)) {
					images.add(new ImageViewerItem(R.drawable.icon_cmd, "profile", "common_newgame", false, true));
					if (AppGlobal.isDebuggable) {
//						images.add(new ImageViewerItem(R.drawable.img_crate, "gogimport", "GoG import", false, true));
					}
				} else {
					images.add(new ImageViewerItem(R.drawable.icon_cmd, "profile", "common_newgame", false, false));
					if (AppGlobal.isDebuggable) {
//						images.add(new ImageViewerItem(R.drawable.img_crate, "gogimport", "GoG import", false, false));
					}
				}

				images.add(new ImageViewerItem(R.drawable.icon_folder, "folder", "common_collection", false, !Global.isDonated));
				images.add(new ImageViewerItem(R.drawable.img_crate, "import", "gameprof_import_title", false, !Global.isDonated));
				return true;
			}
		});

		viewer.setOnImageViewerDisabledEventListener(new ImageViewer.ImageViewerDisabledEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("profile")) {
					MessageInfo.info("msg_collection_limit");
				} else {
					MessageInfo.info("msg_donated_feature");
				}
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("profile"))
				{
					addProfile();
				}
				else if (selected.getName().equals("folder"))
				{
					addCollection();
				}
				else if (selected.getName().equals("import"))
				{
					importProfile();
				}
				else if (selected.getName().equals("gogimport"))
				{
					importGoGGame();
				}
			}
		});

		viewer.show();
	}

	@Override
	protected void onMainSettings()
	{
		ImageViewer viewer = new ImageViewer();
		viewer.setCaption("common_settings");

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				images.add(new ImageViewerItem(R.drawable.icon_settings2_margined, "global", "gls_caption"));
				images.add(new ImageViewerItem(R.drawable.icon_menu, "main", "common_mainmenu"));
				images.add(new ImageViewerItem(R.drawable.icon_crate, "backup", "common_backup"));
				return true;
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("global")) {
					globalSettings = new GlobalSettings(defaultDriveC, language);
					globalSettings.setOnConfirmEventListener(new GlobalSettings.OnConfirmEventListener() {
						@Override
						public void onSave(String defaultDriveC,
										   Language language,
										   String storage1title, String storagePath1,
										   String storage2title, String storagePath2,
										   boolean debug,
										   boolean navigationCursor,
										   int navigationReservedButton,
										   boolean navigationLeftJoy,
										   ImageSize fbItemSize,
										   ImageSize imgViewerItemSize,
										   String dataDir) {
							saveGlobalConfig(defaultDriveC, language, storage1title, storagePath1, storage2title,
									storagePath2, debug, navigationCursor, navigationReservedButton, navigationLeftJoy, fbItemSize, imgViewerItemSize, dataDir);
						}
					});

					globalSettings.show();
				} else if (selected.getName().equals("main")) {
					showMainMenuSettings();
				} else if (selected.getName().equals("backup")) {
					BackupDialog bcp = new BackupDialog();
					bcp.show();
				}
			}
		});

		viewer.show();
	}

    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
        if (resultCode == RESULT_OK)
		{
			Uri treeUri = resultData.getData();

			if (treeUri == null) {
				MessageInfo.shortInfo("Failed to get permissions");
				return;
			}

			int mode = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
			List<UriPermission> uris = getContentResolver().getPersistedUriPermissions();
			if (uris != null && uris.size() > 0) {
				for (UriPermission u : uris) {
					if (!u.getUri().equals(treeUri)) {
						getContentResolver().releasePersistableUriPermission(u.getUri(), mode);
					}
				}
			}

			if (SAFSupport.set(treeUri))
			{
				getContentResolver().takePersistableUriPermission(treeUri, mode);
				AppGlobal.saveSharedPreferences("sdcarduri", SAFSupport.sdcardUriStringShort);
				globalSettings.sdcardUriChanged();
				Storages.reset();
			}
		}
    }

	private void setCollectionItemEvents()
	{
		itemEvents = new OnItemClickListener()
		{
			@Override
			public void onClick(CollectionItem item)
			{
				onCollectionItemClick(item);
			}

			@Override
			public void onLongClick(CollectionItem item)
			{
				onCollectionItemLongClick(item);
			}
		};
	}

	private void startDosbox(CollectionItem item)
	{
		if (item == null || config == null) {
			finish();
			return;
		}

		Intent intent = new Intent(this, MagicLauncher.class);
		intent.putExtra("intent_msg1", item.getID());

		rememberCurrentRun(item);

		clear();
		finish();
		CrashTest.prepareSecond();

		startActivity(intent);
	}

	public void showItemOptions(final CollectionItem item)
	{
		ImageViewer viewer = new ImageViewer();
		viewer.setCaptionEx(item.description);
		viewer.setIcon(new File(AppGlobal.gamesDataPath + item.getID() + "/" + item.getAvatar()));

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				images.add(new ImageViewerItem(R.drawable.icon_edit, "edit", "common_edit"));
				images.add(new ImageViewerItem(R.drawable.img_boots_of_speed, "shortcut", "common_launcher_shortcut"));
				images.add(new ImageViewerItem(R.drawable.img_crate, "export", "common_export"));

				if (AppGlobal.isDonated) {
					images.add(new ImageViewerItem(R.drawable.icon_water_elemental, "duplicate", "common_duplicate"));
				}

				images.add(new ImageViewerItem(R.drawable.icon_disabled, "delete", "common_delete"));
				return true;
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("edit")) {
					editItem(item);
				} else if (selected.getName().equals("shortcut")) {
					createDesktopShortcut(item);
				} else if (selected.getName().equals("export")) {
					exportItem(item);
				} else if (selected.getName().equals("duplicate")) {
					duplicateItem(item);
				} else if (selected.getName().equals("delete")) {
					deleteItem(item);
				}
			}
		});

		viewer.show();
	}

	protected void showFolderOptions(final CollectionFolder folder)
	{
		ImageViewer viewer = new ImageViewer();
		viewer.setCaptionEx(folder.description);
		viewer.setIcon(new File(AppGlobal.gamesDataPath + folder.getID() + "/" + folder.getAvatar()));

		viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
			@Override
			public boolean onSet(List images) {
				images.add(new ImageViewerItem(R.drawable.icon_edit, "edit", "common_edit"));
				images.add(new ImageViewerItem(R.drawable.icon_disabled, "delete", "common_delete"));
				return true;
			}
		});

		viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
			@Override
			public void onPick(ImageViewerItem selected) {
				if (selected.getName().equals("edit")) {
					editCollection(folder);
				} else if (selected.getName().equals("delete")) {
					if (folder.items != null && folder.items.size() > 0) {
						MessageInfo.shortInfo("msg_collection_not_empty");
					} else {
						deleteItem(folder);
					}
				}
			}
		});

		viewer.show();
	}

	public void editItem(CollectionItem item)
	{
		uiGameProfileSettings d = new uiGameProfileSettings(this, item, defaultDriveC);
		d.setOnGameSettingsEventListener(new GameProfileSettingsEventListener() {
			@Override
			public void onSave(DosboxConfig gameConfig,
							   CollectionItem item,
							   String imageFile,
							   String imageName,
							   String description) {
				if (imageFile != null) {
					imageName = updateAvatar(item, imageFile, imageName);
				}

				config.isChange = true;

				updateItem(item, description, imageName);
				updateCollection();

				saveGame(item, gameConfig);
				saveConfig();
			}

			@Override
			public void onSetupRun(DosboxConfig gameConfig,
								   CollectionItem item,
								   String imageFile,
								   String imageName,
								   String description) {
				if (imageFile != null) {
					imageName = updateAvatar(item, imageFile, imageName);
				}

				config.isChange = true;

				item.setDescription(description);
				item.setAvatar(imageName);

				saveGame(item, gameConfig);
				saveConfig();

				AppGlobal.startProgram = EStartProgram.setup;

				startDosbox(item);
			}
		});

		d.show();
	}

	protected void saveGame(CollectionItem item, DosboxConfig config)
	{
		File f2 = new File(AppGlobal.gamesDataPath);

		if (!f2.exists())
			f2.mkdirs();

		Serializer serializer = new Persister();
		File file = new File(AppGlobal.gamesDataPath + item.getID() + "/config.xml");

		try
		{
			serializer.write(config, file);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if (Log.DEBUG) Log.log("saveConfig2");
		}
	}

	private void exportItem(CollectionItem item)
	{
		uiGameProfileExport export = new uiGameProfileExport(this, item);
		export.show();
	}

	private void addProfile()
	{
		if ((!AppGlobal.isDonated) && (config.items.size() > 0))
		{
			MessageInfo.info("msg_collection_limit");
			return;
		}

		uiGameProfileSettings d = new uiGameProfileSettings(AppGlobal.context, null, defaultDriveC);
		d.setOnGameSettingsEventListener(new GameProfileSettingsEventListener()
		{
			@Override
			public void onSave(DosboxConfig gameConfig,
							   CollectionItem game,
							   String imageFile,
							   String imageName,
							   String description)
			{
				if (imageName == null)
					imageName = "avatar.png";

				CollectionItem i = new CollectionItem(imageName, description, UUID.randomUUID().toString());

				File dest = new File(AppGlobal.gamesDataPath + i.getID() + "/");

				if (dest.mkdirs())
				{
					dest = new File(AppGlobal.gamesDataPath + i.getID() + "/" + i.getAvatar());

					boolean ok;

					if (imageFile == null)
					{
						ok = Files.saveResourceToFile(AppGlobal.context, R.drawable.icon_cmd, dest);
					}
					else
					{
						File src = new File(imageFile);
						ok = Files.fileCopy(src, dest);
					}

					if (ok)
					{
						if (currentFolder == null) {
							config.items.add(i);
						} else {
							currentFolder.addItem(i);
						}

						config.isChange = true;

						saveGame(i, gameConfig);
						saveConfig();

						updateCollection(i);
					}
				}
			}

			@Override
			public void onSetupRun(DosboxConfig gameConfig,
								   CollectionItem game,
								   String imageFile,
								   String imageName,
								   String description)
			{
				if (config == null) {
					return;
				}

				if (imageName == null)
					imageName = "avatar.png";

				CollectionItem i = new CollectionItem(imageName, description, UUID.randomUUID().toString());

				File dest = new File(AppGlobal.gamesDataPath + i.getID() + "/");

				if (dest.mkdirs())
				{
					dest = new File(AppGlobal.gamesDataPath + i.getID() + "/" + i.getAvatar());

					boolean ok;

					if (imageFile == null)
					{
						ok = Files.saveResourceToFile(AppGlobal.context, R.drawable.icon_cmd, dest);
					}
					else
					{
						File src = new File(imageFile);
						ok = Files.fileCopy(src, dest);
					}

					if (ok)
					{
						config.items.add(i);
						config.isChange = true;

						saveGame(i, gameConfig);
						saveConfig();

						AppGlobal.startProgram = EStartProgram.setup;

						startDosbox(i);
					}
				}
			}
		});

		d.show();
	}

	protected uiGameFolderDialog.CollectionFolderEventListener getCollectionEvent()
	{
		if (collectionEvent == null)
			collectionEvent = new uiGameFolderDialog.CollectionFolderEventListener()
			{
				@Override
				public void onConfirm(CollectionFolder item, String imageFile, String imageName, String description)
				{
					if (item == null)
					{
						if (imageName == null)
							imageName = "avatar.png";

						item = new CollectionFolder(imageName, description, UUID.randomUUID().toString());

						File dest = new File(AppGlobal.gamesDataPath + item.getID() + "/");

						if (dest.mkdirs())
						{
							dest = new File(AppGlobal.gamesDataPath + item.getID() + "/" + item.getAvatar());

							boolean ok;

							if (imageFile == null)
							{
								ok = Files.saveResourceToFile(AppGlobal.context, R.drawable.icon_folder, dest);
							}
							else
							{
								File src = new File(imageFile);
								ok = Files.fileCopy(src, dest);
							}

							if (ok)
							{
								if (currentFolder == null) {
									config.items.add(item);
								} else {
									currentFolder.addItem(item);
								}

								config.isChange = true;

								saveConfig();

								updateCollection(item);
							}
						}
					}
					else
					{
						if (imageFile != null) {
							imageName = updateAvatar(item, imageFile, imageName);
						}

						config.isChange = true;

						item.setDescription(description);
						item.setAvatar(imageName);

						saveConfig();

						updateCollection();

						//toolbars.updateLastestItem(game, getLastestIndex(game));
					}
				}
			};

		return collectionEvent;
	}

	private void addCollection()
	{
		uiGameFolderDialog d = new uiGameFolderDialog(this, null);
		d.setOnCollectionFolderEventListener(getCollectionEvent());

		d.show();
	}

	private void editCollection(CollectionFolder folder)
	{
		uiGameFolderDialog d = new uiGameFolderDialog(this, folder);
		d.setOnCollectionFolderEventListener(getCollectionEvent());

		d.show();
	}

	private void importProfile()
	{
		Storages.onDrivePick(AppGlobal.context, new Storages.onDrivePickListener()
		{
			@Override
			public void onPick(String drive)
			{

				FileBrowser fb = new FileBrowser(AppGlobal.context, drive, new String[] { ".mgc" });
				fb.setCaption("fb_caption_find_export");

				fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
					@Override
					public void onPick(String selected) {
						final uiGameProfileImport importProfile = new uiGameProfileImport(AppGlobal.context, new File(selected));
						importProfile.init();

						importProfile.setOnProfileImportEventListener(new ProfileImportEventListener() {
							@Override
							public void onBasicsLoad(boolean success) {
								if (success) {
									importProfile.show();
								} else {
									MessageInfo.info("gameprof_import_failed_load_info");
									importProfile.dismiss();
								}
							}

							@Override
							public void onFinish(CollectionItem item) {
								if (item != null) {
									if (currentFolder == null) {
										config.items.add(item);
									} else {
										currentFolder.items.add(item);
									}

									config.isChange = true;

									saveConfig();

									updateCollection(item);
									MessageInfo.info("gameprof_import_success");
								} else {
									MessageInfo.info("gameprof_import_failed");
								}

								importProfile.dismiss();
							}
						});
					}
				});

				fb.show();
			}
		});
	}

	private void importGoGGame() {
		DosboxConfigImport dbxImport = new DosboxConfigImport();
		dbxImport.show();
	}
}