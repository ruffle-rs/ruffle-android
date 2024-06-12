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
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

import magiclib.IO.FileBrowser;
import magiclib.IO.FileBrowserItem;
import magiclib.IO.Files;
import magiclib.IO.Storages;
import magiclib.collection.CollectionFolder;
import magiclib.controls.Dialog;
import magiclib.logging.MessageInfo;

class uiGameFolderDialog extends Dialog
{
    public abstract interface CollectionFolderEventListener
    {
        public abstract void onConfirm(CollectionFolder item, String imageFile, String imageName, String description);
    }

    private EditText name;
    private ImageButton avatar;
    private CollectionFolder item;
    private String imageName = null;
    private String imageFile = null;
    private ImageButton butConfirm;
    private CollectionFolderEventListener eventListener = null;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.collection_folder_edit_title, "collection_title");
    }

    public uiGameFolderDialog(Context context, CollectionFolder item)
    {
        super(context);

        setContentView(R.layout.collection_folder_edit);
        setCaption("common_settings");

        this.item = item;

        butConfirm = (ImageButton)findViewById(R.id.collection_folder_edit_confirm);
        avatar = (ImageButton)findViewById(R.id.collection_folder_edit_image);
        name = (EditText)getView().findViewById(R.id.collection_folder_edit_name);

        if (item != null)
        {
            imageName = item.getAvatar();
            name.setText(item.description);
        }

        View.OnClickListener onClick = new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (view.getId() == R.id.collection_folder_edit_image)
                {
                    editAvatar();
                    return;
                }

                if (view.getId() == R.id.collection_folder_edit_confirm)
                {
                    confirmChanges();
                    return;
                }
            }
        };

        avatar.setOnClickListener(onClick);
        butConfirm.setOnClickListener(onClick);

        if (item!=null && !item.getAvatar().equals(""))
        {
            File imgFile = new File(AppGlobal.gamesDataPath + item.getID() + "/" + item.getAvatar());

            if(imgFile.exists())
            {
                Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                avatar.setImageBitmap(bmp);
                avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        }
    }

    private void editAvatar()
    {
        Storages.onDrivePick(getContext(), new Storages.onDrivePickListener() {
            @Override
            public void onPick(String drive) {
                final FileBrowser fb = new FileBrowser(getContext(), drive, new String[]{".png", ".jpg", ".jpeg", ".bmp", ".PNG", ".JPG", ".JPEG", ".BMP"});
                fb.setCaption("fb_caption_find_image");
                fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
                    @Override
                    public void onPick(String selected) {
                        imageFile = selected;

                        Bitmap bmp = BitmapFactory.decodeFile(imageFile);

                        avatar.setImageBitmap(bmp);
                        avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                        try {
                            imageName = "avatar." + Files.getFileExtension(selected);
                        } catch (Exception exc) {

                        }
                    }
                });

                fb.setOnFileValidationEvent(new FileBrowser.OnFileValidationListener() {
                    @Override
                    public FileBrowserItem onValidation(File file) {
                        try {
                            BitmapFactory.decodeFile(file.getAbsolutePath(), AppGlobal.imageHeaderOptions);

                            if (AppGlobal.imageHeaderOptions.outWidth < 0 || AppGlobal.imageHeaderOptions.outHeight < 0) {
                                return null;
                            }

                            FileBrowserItem item = new FileBrowserItem(file);

                            if ((AppGlobal.imageHeaderOptions.outWidth > 0) &&
                                    (AppGlobal.imageHeaderOptions.outHeight > 0) &&
                                    (AppGlobal.imageHeaderOptions.outWidth <= 1024) &&
                                    (AppGlobal.imageHeaderOptions.outHeight <= 1024)) {
                                item.isPictureFile = true;
                            } else {
                                item.isPictureFile = false;//will not show preview
                            }

                            return item;
                        } catch (Exception exc) {
                        }

                        return null;
                    }
                });

                fb.show();
            }
        });
    }

    public void setOnCollectionFolderEventListener(CollectionFolderEventListener event)
    {
        this.eventListener = event;
    }

    private void confirmChanges()
    {
        String title = name.getText().toString().trim();

        if (title.equals("")) {
            MessageInfo.info("msg_title_required");
            return;
        }

        if (eventListener != null)
        {
            eventListener.onConfirm(item, imageFile, imageName, name.getText().toString());
        }

        dismiss();
    }
}
