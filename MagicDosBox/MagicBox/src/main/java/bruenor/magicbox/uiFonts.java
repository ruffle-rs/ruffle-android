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

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import magiclib.CrossSettings;
import magiclib.Global;
import magiclib.IO.FileBrowser;
import magiclib.IO.Files;
import magiclib.IO.Storages;
import magiclib.controls.HelpViewer;
import magiclib.core.RandomStringGenerator;
import magiclib.controls.Dialog;
import magiclib.fonts.ExternalFontItem;
import magiclib.fonts.ExternalFonts;
import magiclib.locales.Localization;

class FontTitleSettings extends Dialog
{
    abstract interface FontTitleSettingsEventListener
    {
        public abstract void onPick(String title);
    }

    private FontTitleSettingsEventListener event;
    private EditText titleText;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.fonts_title_title, "common_title");
    }

    public FontTitleSettings(String title)
    {
        super(AppGlobal.context);
        setContentView(R.layout.fonts_title);
        setCaption("common_title");

        titleText = (EditText)findViewById(R.id.fonts_title_titlevalue);
        titleText.setText(title);

        ImageView confirm = (ImageView)findViewById(R.id.fonts_title_confirm);
        confirm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (event != null)
                {
                    event.onPick(titleText.getText().toString());
                }

                dismiss();
            }
        });
    }

    public void setOnFontTitleSettingsEventListener(FontTitleSettingsEventListener event)
    {
        this.event = event;
    }
}

class FontsSettings extends Dialog
{
    class ExternalFontItemEdit extends ExternalFontItem
    {
        public File file;
        public boolean isNew = true;
        public boolean isTitleModified = false;

        public ExternalFontItemEdit()
        {
            super();
        }

        public ExternalFontItemEdit(String title)
        {
            super(title);
        }
    }

    private ImageButton addFont;
    private ImageButton delFont;
    private ImageButton helpButton;
    private LinearLayout fontItemsLayout;
    private View.OnClickListener onClickEvent;
    private boolean isSomethingChanged = false;
    private boolean isDeleteState = false;

    private static List<ExternalFontItemEdit> list;

    public FontsSettings()
    {
        super(AppGlobal.context);

        setContentView(R.layout.fonts);
        setCaption("fonts_caption");

        fontItemsLayout = (LinearLayout) findViewById(R.id.fonts_items);

        addFont = (ImageButton)findViewById(R.id.fonts_addbutton);
        addFont.setOnClickListener(getClickEvent());

        delFont = (ImageButton)findViewById(R.id.fonts_delbutton);
        delFont.setOnClickListener(getClickEvent());

        helpButton = (ImageButton)findViewById(R.id.fonts_help);
        helpButton.setOnClickListener(getClickEvent());

        ImageView confirm = (ImageView)findViewById(R.id.fonts_confirm);
        confirm.setOnClickListener(getClickEvent());

        list = new ArrayList<ExternalFontItemEdit>();

        if (ExternalFonts.fonts != null)
        {
            for(ExternalFontItem fnt : ExternalFonts.fonts)
            {
                ExternalFontItemEdit ei = new ExternalFontItemEdit();
                ei.isNew = false;
                fnt.copyTo(ei);

                list.add(ei);

                fontItemsLayout.addView(getFontItemView(ei));
            }
        }
    }

    private View.OnClickListener getClickEvent()
    {
        if (onClickEvent != null)
            return onClickEvent;

        return onClickEvent = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.fonts_addbutton:
                    {
                        ExternalFontItemEdit item = new ExternalFontItemEdit(Localization.getString("fonts_new_tune"));
                        list.add(item);
                        fontItemsLayout.addView(getFontItemView(item));
                        break;
                    }
                    case R.id.fonts_delbutton:
                    {
                        isDeleteState = !isDeleteState;
                        switchCheckers();
                        break;
                    }
                    case R.id.fonts_help:
                    {
                        HelpViewer hlp = new HelpViewer("common_help", null, "help/tips/ingame/general-settings/runecrafting.html", CrossSettings.showInGameHelp, true, false);
                        hlp.hideNavigationPanel();
                        hlp.show();
                        break;
                    }
                    case R.id.fonts_confirm:
                    {
                        confirmChanges();
                        dismiss();
                        break;
                    }
                    case R.id.fonts_item_title:
                    {
                        editFontTitle((View)v.getTag());
                        break;
                    }
                    case R.id.fonts_item_settings:
                    {
                        loadFontFromDisk((View)v.getTag());
                        break;
                    }
                }
            }
        };
    }

    private void finishDeleteState()
    {
        if (isDeleteState)
        {
            isDeleteState = false;
            switchCheckers();
        }
    }

    private void switchCheckers()
    {
        List<RelativeLayout> deletedViews = null;

        for (int i = 0; i < fontItemsLayout.getChildCount(); i++)
        {
            RelativeLayout view = (RelativeLayout) fontItemsLayout.getChildAt(i);
            CheckBox cbx = (CheckBox) view.findViewById(R.id.fonts_item_checker);

            ImageView icon = (ImageView)view.findViewById(R.id.fonts_item_icon);

            if (isDeleteState)
            {
                cbx.setChecked(false);
                cbx.setVisibility(View.VISIBLE);
                icon.setVisibility(View.INVISIBLE);
            }
            else
            {
                cbx.setVisibility(View.INVISIBLE);
                icon.setVisibility(View.VISIBLE);

                if (deletedViews == null)
                    deletedViews = new ArrayList<RelativeLayout>();

                if (cbx.isChecked())
                {
                    deletedViews.add(view);
                    view.setTag(list.get(i));
                }
            }
        }

        if (deletedViews != null)
        {
            for (View view : deletedViews)
            {
                ExternalFontItemEdit ei = (ExternalFontItemEdit)view.getTag();

                fontItemsLayout.removeView(view);
                list.remove(view.getTag());

                if (!ei.isNew)
                    isSomethingChanged = true;
            }
        }
    }

    private void loadFontFromDisk(final View v)
    {
        Storages.onDrivePick(AppGlobal.context, new Storages.onDrivePickListener() {
            @Override
            public void onPick(String drive) {
                FileBrowser fb = new FileBrowser(Global.context, drive, new String[]{".ttf", ".TTF"});
                fb.setCaption("fb_pick_ttf");
                fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
                    @Override
                    public void onPick(String selected) {
                        File f = new File(selected);

                        ExternalFontItemEdit item = (ExternalFontItemEdit) v.getTag();

                        if (item.isNew && !item.isTitleModified) {
                            item.title = f.getName();
                            item.isTitleModified = true;
                            item.typeface = null;

                            ((TextView) v.findViewById(R.id.fonts_item_title)).setText(item.title);
                        }

                        item.file = f;

                        isSomethingChanged = true;
                    }
                });
                fb.show();
            }
        });
    }

    public View getFontItemView(ExternalFontItemEdit item)
    {
        View v = getLayoutInflater().inflate(R.layout.fonts_item, null);
        v.setTag(item);

        TextView fontName = (TextView)v.findViewById(R.id.fonts_item_title);
        fontName.setText(item.title);
        fontName.setTag(v);
        fontName.setOnClickListener(getClickEvent());

        ImageButton b = (ImageButton)v.findViewById(R.id.fonts_item_settings);
        b.setTag(v);
        b.setOnClickListener(getClickEvent());

        return v;
    }

    public void editFontTitle(final View v)
    {
        FontTitleSettings d = new FontTitleSettings(((ExternalFontItemEdit)v.getTag()).title);
        d.setOnFontTitleSettingsEventListener(new FontTitleSettings.FontTitleSettingsEventListener() {
            @Override
            public void onPick(String title) {
                ExternalFontItemEdit item = (ExternalFontItemEdit) v.getTag();
                item.isTitleModified = true;
                item.title = title;

                ((TextView) v.findViewById(R.id.fonts_item_title)).setText(title);

                isSomethingChanged = true;
            }
        });
        d.show();
    }

    private boolean containsCode(String code)
    {
        for (ExternalFontItemEdit ei : list)
        {
            if (ei.fileName.equals(code))
            {
                return true;
            }
        }

        return false;
    }

    private String getUniqueName()
    {
        String code;
        while (containsCode(code = RandomStringGenerator.generateRandomString(5, RandomStringGenerator.Mode.ALPHA)));
        return code;
    }

    private void confirmChanges()
    {
        if (!isSomethingChanged)
        {
            dismiss();
        }

        File fld = new File(AppGlobal.currentGameFontsPath);

        if (!fld.exists())
            fld.mkdirs();

        List<ExternalFontItem> newList = new ArrayList<ExternalFontItem>();

        for (ExternalFontItemEdit ei : list)
        {
            if (ei.isNew && ei.file == null)
                continue;

            if (ei.file != null)
            {
                if (ei.fileName.equals(""))
                    ei.fileName = getUniqueName();

                Files.fileCopy(ei.file, new File(fld, ei.fileName));
            }

            ExternalFontItem item = new ExternalFontItem();
            ei.copyTo(item);

            newList.add(item);
        }

        if (ExternalFonts.fonts != null && ExternalFonts.fonts.size() > 0)
        {
            boolean found;
            for(ExternalFontItem older : ExternalFonts.fonts)
            {
                found = false;

                for(ExternalFontItem newer : newList)
                {
                    if (newer.fileName.equals(older.fileName))
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    new File(fld, older.fileName).delete();
                }
            }
        }

        ExternalFonts.update(newList);
        ExternalFonts.save();
    }
}





