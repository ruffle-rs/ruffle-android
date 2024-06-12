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
import android.os.Build;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import magiclib.Global;
import magiclib.controls.Dialog;
import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.core.NavigationCursor;
import magiclib.keyboard.Key;
import magiclib.keyboard.KeyAction;
import magiclib.keyboard.KeyCodeInfo;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetType;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;
import magiclib.mapper.Mapper;
import magiclib.mapper.MapperControlType;
import magiclib.mapper.MapperProfile;
import magiclib.mapper.MapperProfileItem;
import magiclib.mapper.MapperProfileItemType;
import magiclib.mouse.MouseButton;

abstract interface MapperEventListener {
    public abstract void onPick(int keyCode);
}

class MapperSettings extends MapperDialog {
    private final int BUTTONS_MENU_ID = -1000;
    private final int DPAD_MENU_ID = -1001;
    private final int LEFTJOY_MENU_ID = -1002;
    private final int RIGHTJOY_MENU_ID = -1003;
    private final int L2R2_MENU_ID = -1004;
    private final int TILT_MENU_ID = -1005;
    // temp
    private List<MapperProfile> temp_profiles;

    private boolean temp_enabled;
    private MapperProfile temp_profile;

    private LinearLayout mainView;
    private CheckBox mapperEnabled;
    private View buttonsMenu;
    private View dpadMenu;
    private View leftJoystickMenu;
    private View rightJoystickMenu;
    private View l2r2Menu;
    private View tiltMenu;

    private View.OnClickListener onClick;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mapper_enabled, "common_enabled");
    }

    public MapperSettings() {
        super(AppGlobal.context);

        setContentView(R.layout.mapper);
        setCaption("mapper_caption");

        mainView = (LinearLayout) findViewById(R.id.mapper_menu);
        mapperEnabled = (CheckBox) findViewById(R.id.mapper_enabled);

        addSection(li, Localization.getString("common_joystick"), -1);

        buttonsMenu = addButtonsOption();
        dpadMenu = addDpadOption();

        if (Build.VERSION.SDK_INT >= 12) {
            leftJoystickMenu = addLeftStickOption();
            rightJoystickMenu = addRightStickOption();
            l2r2Menu = addL2R2Option();
        }

        addSection(li, Localization.getString("mapper_tilt_title"), -1);
        tiltMenu = addTiltOption();

        createTempMapperValues();
        setButtonsMenuValue();
        setDpadValue();

        if (Build.VERSION.SDK_INT >= 12) {
            setLeftJoystickValue();
            setRightJoystickValue();
            setL2R2Value();
        }

        setTiltValue();

        mapperEnabled.setChecked(temp_enabled);
        findViewById(R.id.mapper_confirm).setOnClickListener(getOnClickEvent());
    }

    private View.OnClickListener getOnClickEvent() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.mapper_confirm: {
                            Mapper.update(temp_profiles, mapperEnabled.isChecked());
                            Mapper.edited = true;

                            dismiss();
                            break;
                        }
                        case BUTTONS_MENU_ID: {
                            showButtonsOptions();
                            break;
                        }
                        case DPAD_MENU_ID: {
                            showDpadOptions();
                            break;
                        }
                        case LEFTJOY_MENU_ID: {
                            showLeftJoyOptions();
                            break;
                        }
                        case RIGHTJOY_MENU_ID: {
                            showRightJoyOptions();
                            break;
                        }
                        case L2R2_MENU_ID: {
                            showL2R2Options();
                            break;
                        }
                        case TILT_MENU_ID: {
                            showTiltOptions();
                            break;
                        }
                    }
                }
            };
        }

        return onClick;
    }

    private void setMenuItemValue(View view, String value) {
        ((TextView) view.findViewById(R.id.mapper_menuitem_value)).setText(value);
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
        RelativeLayout view = (RelativeLayout) li.inflate(R.layout.mapper_menuitem, null);
        view.setId(ID);

        ImageView image = (ImageView) view.findViewById(R.id.mapper_menuitem_image);
        image.setImageResource(imageResource);

        TextView label = (TextView) view.findViewById(R.id.mapper_menuitem_label);
        label.setText(title);

        view.setOnClickListener(getOnClickEvent());

        mainView.addView(view);

        return view;
    }

    private View addButtonsOption() {
        return addMenuItem(BUTTONS_MENU_ID, R.drawable.icon_gamepad_button, Localization.getString("common_gamepad_buttons"));
    }

    private View addDpadOption() {
        return addMenuItem(DPAD_MENU_ID, R.drawable.icon_dpad, Localization.getString("common_dpad"));
    }

    private View addLeftStickOption() {
        return addMenuItem(LEFTJOY_MENU_ID, R.drawable.icon_leftjoy, Localization.getString("mapper_lj_hint"));
    }

    private View addRightStickOption() {
        return addMenuItem(RIGHTJOY_MENU_ID, R.drawable.icon_rightjoy, Localization.getString("mapper_rj_hint"));
    }

    private View addL2R2Option() {
        return addMenuItem(L2R2_MENU_ID, R.drawable.icon_l2r2, Localization.getString("mapper_l2r2_hint"));
    }

    private View addTiltOption() {
        return addMenuItem(TILT_MENU_ID, R.drawable.icon_gamepad_button, Localization.getString("mapper_tilt_title"));
    }

    private void createTempMapperValues() {
        temp_profiles = new LinkedList<MapperProfile>();

        if (Mapper.profiles == null || Mapper.profiles.size() == 0) {
            temp_profiles.add(new MapperProfile());
            temp_enabled = false;
        } else {
            temp_profiles.add(Mapper.profiles.get(0).copyTo(new MapperProfile()));
            temp_enabled = Mapper.enabled;
        }

        temp_profile = temp_profiles.get(0);

//fix for compatibility with older versions (20160806 - for v39 and below)
        if (temp_profile.dpadControlType == null) {
            temp_profile.dpadControlType = !temp_profile.native2AxisDpad ? MapperControlType.keypad : MapperControlType.twoAxis;
        }
        if (temp_profile.rjControlType == null) {
            temp_profile.rjControlType = temp_profile.rjMouseEnabled ? MapperControlType.mouse : MapperControlType.none;
        }
//end fix

        if (temp_profile.ljEvents.size() == 0) {
            temp_profile.dpadEvents.clear();

            for (int i = 0; i < 4; i++) {
                temp_profile.ljEvents.add(new MapperProfileItem());

                // dpad will be initialized
                MapperProfileItem dpadItem = new MapperProfileItem();
                dpadItem.type = MapperProfileItemType.key;

                switch (i) {
                    case 0:
                        dpadItem.dosboxKey = new Key(19);
                        break;// up
                    case 1:
                        dpadItem.dosboxKey = new Key(20);
                        break;// down
                    case 2:
                        dpadItem.dosboxKey = new Key(21);
                        break;// left
                    case 3:
                        dpadItem.dosboxKey = new Key(22);
                        break;// right
                }

                temp_profile.dpadEvents.add(dpadItem);
            }
        }

        if (temp_profile.rjEvents.size() == 0) {
            for (int i = 0; i < 4; i++) {
                temp_profile.rjEvents.add(new MapperProfileItem());
            }
        }

        if (temp_profile.axisEvents.size() == 0) {
            //0 = L2
            //1 = R2
            //...add additional...
            for (int i = 0; i < 2; i++) {
                MapperProfileItem axisItem = new MapperProfileItem();
                axisItem.type = MapperProfileItemType.none;
                temp_profile.axisEvents.add(axisItem);
            }
        }

        if (temp_profile.tiltEvents.size() == 0) {
            for (int i = 0; i < 4; i++) {
                MapperProfileItem tiltItem = new MapperProfileItem();
                tiltItem.type = MapperProfileItemType.key;

                switch (i) {
                    case 0:
                        tiltItem.dosboxKey = new Key(19);
                        break;// up
                    case 1:
                        tiltItem.dosboxKey = new Key(20);
                        break;// down
                    case 2:
                        tiltItem.dosboxKey = new Key(21);
                        break;// left
                    case 3:
                        tiltItem.dosboxKey = new Key(22);
                        break;// right
                }

                temp_profile.tiltEvents.add(tiltItem);
            }
        }
    }

    private void setButtonsMenuValue() {
        int size = temp_profile.keyEvents.size();
        String value = null;
        if (size > 0) {
            int index = 0;
            for (MapperProfileItem item : temp_profile.keyEvents) {
                if (index == 0) {
                    value = KeyCodeInfo.getAndroidKeyInfo(item.key, false);
                } else {
                    value += ", " + KeyCodeInfo.getAndroidKeyInfo(item.key, false);
                }

                index++;
                if (index > 6) {
                    value += ", ...";
                    break;
                }
            }
        }

        if (value == null) {
            value = Localization.getString("mapper_unassigned");
        }

        setMenuItemValue(buttonsMenu, value);
    }

    private void setDpadValue() {
        String value;
        switch (temp_profile.dpadControlType) {
            case keypad: {
                value = Localization.getString("common_keypad");
                break;
            }
            case twoAxis: {
                value = Localization.getString("common_2axis");
                break;
            }
            default: {
                value = Localization.getString("mapper_unassigned");
            }
        }

        setMenuItemValue(dpadMenu, value);
    }

    private void setLeftJoystickValue() {
        String value;
        switch (temp_profile.ljControlType) {
            case keypad: {
                value = Localization.getString("common_keypad");
                break;
            }
            case twoAxis: {
                value = Localization.getString("common_2axis");
                break;
            }
            case mouse: {
                value = Localization.getString("common_mouse");
                break;
            }
            default: {
                value = Localization.getString("mapper_unassigned");
            }
        }

        setMenuItemValue(leftJoystickMenu, value);
    }

    private void setRightJoystickValue() {
        String value;
        switch (temp_profile.rjControlType) {
            case keypad: {
                value = Localization.getString("common_keypad");
                break;
            }
            case twoAxis: {
                value = Localization.getString("common_2axis");
                break;
            }
            case mouse: {
                value = Localization.getString("common_mouse");
                break;
            }
            default: {
                value = Localization.getString("mapper_unassigned");
            }
        }

        setMenuItemValue(rightJoystickMenu, value);
    }

    private void setL2R2Value() {
        int size = temp_profile.axisEvents.size();
        String value = null;
        if (size > 0) {
            int index = -1;
            for (MapperProfileItem item : temp_profile.axisEvents) {
                index++;
                if (item.dosboxKey == null) {
                    continue;
                }

                if (value == null) {
                    value = KeyCodeInfo.getAndroidKeyInfo(index == 0 ? 104 : 105, false);
                } else {
                    value += ", " + KeyCodeInfo.getAndroidKeyInfo(index == 0 ? 104 : 105, false);
                }

                if (index > 6) {
                    value += ", ...";
                    break;
                }
            }
        }

        if (value == null) {
            value = Localization.getString("mapper_unassigned");
        }

        setMenuItemValue(l2r2Menu, value);
    }

    private void setTiltValue() {
        String value;
        switch (temp_profile.tiltControlType) {
            case keypad: {
                value = Localization.getString("common_keypad");
                break;
            }
            case twoAxis: {
                value = Localization.getString("common_2axis");
                break;
            }
            default: {
                value = Localization.getString("mapper_unassigned");
            }
        }

        setMenuItemValue(tiltMenu, value);
    }

    private void showButtonsOptions() {
        hide();

        GamepadButtonsOptions d = new GamepadButtonsOptions(this, temp_profile.keyEvents);
        d.setOnGamepadButtonsDialogEventListener(new GamepadButtonsDialogEventListener() {
            @Override
            public void onPick(List<MapperProfileItem> buttons) {
                temp_profile.keyEvents = buttons;
                setButtonsMenuValue();
            }
        });
        d.show();
    }

    private void showDpadOptions() {
        final Dialog parent = this;
        final uiImageViewer viewer = new uiImageViewer(getContext());
        viewer.setCaption("common_choose");

        viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
            @Override
            public boolean onSet(List images) {
                images.add(new ImageViewerItem(R.drawable.icon_key, "keypad", "common_keypad"));
                images.add(new ImageViewerItem(R.drawable.icon_leftjoy, "2axis", "common_2axis"));
                return true;
            }
        });

        viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
            @Override
            public void onPick(ImageViewerItem selected) {
                if (selected.getName().equals("keypad")) {
                    parent.hide();

                    KeyPadSettings d = new KeyPadSettings(parent, temp_profile.dpadEvents, temp_profile.dpadDiagonals, -1);
                    d.setOnKeyPadDialogEventListener(new KeyPadDialogEventListener() {
                        @Override
                        public void onPick(List<MapperProfileItem> buttons, boolean diagonalsOn, int deadZone) {
                            temp_profile.dpadControlType = MapperControlType.keypad;
                            temp_profile.dpadEvents = buttons;
                            temp_profile.dpadDiagonals = diagonalsOn;
                            setDpadValue();
                        }
                    });
                    d.show();
                } else if (selected.getName().equals("2axis")) {
                    temp_profile.dpadControlType = MapperControlType.twoAxis;
                    setDpadValue();
                }
            }
        });

        viewer.show();
    }

    private void showLeftJoyOptions() {
        final Dialog parent = this;

        uiImageViewer viewer = new uiImageViewer(getContext());
        viewer.setCaption("common_choose");

        viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
            @Override
            public boolean onSet(List images) {
                images.add(new ImageViewerItem(R.drawable.icon_disabled, "unmap", "common_unmap"));
                images.add(new ImageViewerItem(R.drawable.icon_key, "keypad", "common_keypad"));
                images.add(new ImageViewerItem(R.drawable.icon_mouse2, "mouse", "common_mouse"));
                images.add(new ImageViewerItem(R.drawable.icon_leftjoy, "2axis", "common_2axis"));
                return true;
            }
        });

        viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
            @Override
            public void onPick(ImageViewerItem selected) {
                if (selected.getName().equals("unmap")) {
                    temp_profile.ljControlType = MapperControlType.none;
                    setLeftJoystickValue();
                } else if (selected.getName().equals("keypad")) {
                    parent.hide();

                    KeyPadSettings d = new KeyPadSettings(parent, temp_profile.ljEvents, temp_profile.ljDiagonals, temp_profile.ljDeadZone);
                    d.setOnKeyPadDialogEventListener(new KeyPadDialogEventListener() {
                        @Override
                        public void onPick(List<MapperProfileItem> buttons, boolean diagonalsOn, int deadZone) {
                            temp_profile.ljControlType = MapperControlType.keypad;
                            temp_profile.ljEvents = buttons;
                            temp_profile.ljDiagonals = diagonalsOn;
                            temp_profile.ljDeadZone = deadZone;
                            setLeftJoystickValue();
                        }
                    });
                    d.show();
                } else if (selected.getName().equals("mouse")) {
                    parent.hide();

                    MouseSettings d = new MouseSettings(parent, temp_profile.rjMouseRate);
                    d.setOnMouseDialogEventListener(new MouseDialogEventListener() {
                        @Override
                        public void onPick(int rate) {
                            temp_profile.ljControlType = MapperControlType.mouse;
                            temp_profile.rjMouseRate = rate;
                            setLeftJoystickValue();

                            if (temp_profile.rjControlType == MapperControlType.mouse) {
                                temp_profile.rjControlType = MapperControlType.keypad;
                                setRightJoystickValue();
                            }
                        }
                    });
                    d.show();
                } else if (selected.getName().equals("2axis")) {
                    temp_profile.ljControlType = MapperControlType.twoAxis;
                    setLeftJoystickValue();
                }
            }
        });

        viewer.show();
    }

    private void showRightJoyOptions() {
        final Dialog parent = this;

        uiImageViewer viewer = new uiImageViewer(getContext());
        viewer.setCaption("common_choose");

        viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
            @Override
            public boolean onSet(List images) {
                images.add(new ImageViewerItem(R.drawable.icon_disabled, "unmap", "common_unmap"));
                images.add(new ImageViewerItem(R.drawable.icon_key, "keypad", "common_keypad"));
                images.add(new ImageViewerItem(R.drawable.icon_mouse2, "mouse", "common_mouse"));
                images.add(new ImageViewerItem(R.drawable.icon_leftjoy, "2axis", "common_2axis"));
                return true;
            }
        });

        viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
            @Override
            public void onPick(ImageViewerItem selected) {
                if (selected.getName().equals("unmap")) {
                    temp_profile.rjControlType = MapperControlType.none;
                    setRightJoystickValue();
                } else if (selected.getName().equals("keypad")) {
                    parent.hide();

                    KeyPadSettings d = new KeyPadSettings(parent, temp_profile.rjEvents, temp_profile.rjDiagonals, temp_profile.rjDeadZone);
                    d.setOnKeyPadDialogEventListener(new KeyPadDialogEventListener() {
                        @Override
                        public void onPick(List<MapperProfileItem> buttons, boolean diagonalsOn, int deadZone) {
                            temp_profile.rjControlType = MapperControlType.keypad;
                            temp_profile.rjEvents = buttons;
                            temp_profile.rjDiagonals = diagonalsOn;
                            temp_profile.rjDeadZone = deadZone;
                            setRightJoystickValue();
                        }
                    });
                    d.show();
                } else if (selected.getName().equals("mouse")) {
                    parent.hide();

                    MouseSettings d = new MouseSettings(parent, temp_profile.rjMouseRate);
                    d.setOnMouseDialogEventListener(new MouseDialogEventListener() {
                        @Override
                        public void onPick(int rate) {
                            temp_profile.rjControlType = MapperControlType.mouse;
                            temp_profile.rjMouseRate = rate;
                            setRightJoystickValue();

                            if (temp_profile.ljControlType == MapperControlType.mouse) {
                                temp_profile.ljControlType = MapperControlType.keypad;
                                setLeftJoystickValue();
                            }
                        }
                    });
                    d.show();
                } else if (selected.getName().equals("2axis")) {
                    temp_profile.rjControlType = MapperControlType.twoAxis;
                    setRightJoystickValue();
                }
            }
        });

        viewer.show();
    }

    private void showL2R2Options() {
        this.hide();

        L2R2Settings d = new L2R2Settings(this, temp_profile.axisEvents);
        d.setOnGamepadButtonsDialogEventListener(new GamepadButtonsDialogEventListener() {
            @Override
            public void onPick(List<MapperProfileItem> buttons) {
                temp_profile.axisEvents = buttons;
                setL2R2Value();
            }
        });
        d.show();
    }

    private void showTiltOptions() {
        final Dialog parent = this;

        uiImageViewer viewer = new uiImageViewer(getContext());
        viewer.setCaption("common_choose");

        viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
            @Override
            public boolean onSet(List images) {
                images.add(new ImageViewerItem(R.drawable.icon_disabled, "unmap", "common_unmap"));
                images.add(new ImageViewerItem(R.drawable.icon_key, "keypad", "common_keypad"));
                images.add(new ImageViewerItem(R.drawable.icon_leftjoy, "2axis", "common_2axis"));
                return true;
            }
        });

        viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
            @Override
            public void onPick(ImageViewerItem selected) {
                if (selected.getName().equals("unmap")) {
                    temp_profile.tiltControlType = MapperControlType.none;
                    setTiltValue();
                } else if (selected.getName().equals("keypad")) {
                    parent.hide();

                    TiltSettings d = new TiltSettings(parent, temp_profile.tiltEvents, temp_profile.tiltDeadZone, temp_profile.updownEnabled);
                    d.setOnTiltDialogEventListener(new TiltDialogEventListener() {
                        @Override
                        public void onPick(List<MapperProfileItem> buttons, double deadZone, boolean updown, int activeAngle, int activeAngleStart, int activeAngleMiddle, int activeAngleEnd) {
                            temp_profile.tiltControlType = MapperControlType.keypad;
                            temp_profile.tiltEvents = buttons;
                            temp_profile.tiltDeadZone = deadZone;
                            temp_profile.updownEnabled = updown;

                            setTiltValue();
                        }
                    });
                    d.show();
                } else if (selected.getName().equals("2axis")) {
                    parent.hide();

                    Tilt2AxisSettings d = new Tilt2AxisSettings(parent, temp_profile.tiltLRActiveAngle, temp_profile.tilt2AxisUpDownEnabled,
                            temp_profile.tilt2AxisUDActiveAngleStart, temp_profile.tilt2AxisUDActiveAngleMiddle, temp_profile.tilt2AxisUDActiveAngleEnd);
                    d.setOnTiltDialogEventListener(new TiltDialogEventListener() {
                        @Override
                        public void onPick(List<MapperProfileItem> buttons, double deadZone, boolean updown, int activeAngle, int activeAngleStart, int activeAngleMiddle, int activeAngleEnd) {
                            temp_profile.tiltControlType = MapperControlType.twoAxis;
                            temp_profile.tilt2AxisUpDownEnabled = updown;
                            temp_profile.tiltLRActiveAngle = activeAngle;
                            temp_profile.tilt2AxisUDActiveAngleStart = activeAngleStart;
                            temp_profile.tilt2AxisUDActiveAngleMiddle = activeAngleMiddle;
                            temp_profile.tilt2AxisUDActiveAngleEnd = activeAngleEnd;

                            setTiltValue();
                        }
                    });
                    d.show();
                }
            }
        });

        viewer.show();
    }
}

interface GamepadButtonsDialogEventListener {
    void onPick(List<MapperProfileItem> buttons);
}

class GamepadButtonsOptions extends MapperDialog {
    private Dialog parent;

    private final int INFORMATION_ID = -1000;
    private List<MapperProfileItem> buttons;

    private TextView information;
    private LinearLayout buttonsLayout;
    private ImageButton buttonAdd;
    private ImageButton buttonDel;
    private boolean isDeleteState = false;
    private GamepadButtonsDialogEventListener event;

    public GamepadButtonsOptions(Dialog parent, List<MapperProfileItem> buttons) {
        super(AppGlobal.context);

        this.parent = parent;
        this.buttons = duplicateMapperProfileItemArray(buttons);

        setContentView(R.layout.mapper_buttons);
        setCaption("common_gamepad_buttons");

        buttonsLayout = (LinearLayout) findViewById(R.id.mapper_buttons);
        buttonAdd = (ImageButton) findViewById(R.id.mapper_addbutton);
        buttonAdd.setOnClickListener(onClick());

        buttonDel = (ImageButton) findViewById(R.id.mapper_delbutton);
        buttonDel.setOnClickListener(onClick());

        findViewById(R.id.mapper_confirm).setOnClickListener(onClick());

        int size = this.buttons.size();

        if (size == 0) {
            addEmptyListInformation();
        } else {
            for (int i = 0; i < size; i++) {
                buttonsLayout.addView(getButton(li, "", this.buttons.get(i)));
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        parent.show();
    }

    private void addEmptyListInformation() {
        if (information == null) {
            information = new TextView(getContext());
            information.setId(INFORMATION_ID);
            //information.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            information.setText(Html.fromHtml("<html><head><meta charset=\"UTF-8\"></head><body>" + Localization.getString("msg_press_plus_add_buttons") + "</body></html>"));
            information.setGravity(Gravity.CENTER);
        } else {
            View v = findViewById(INFORMATION_ID);
            if (v != null) {
                return;
            }
        }

        buttonsLayout.addView(information);
    }

    private void removeEmptyListInformation() {
        if (information == null) {
            return;
        }

        buttonsLayout.removeView(information);
    }

    private View.OnClickListener onClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.mapper_addbutton: {
                        finishDeleteState();

                        MapperDetector mapper = new MapperDetector(getContext(), buttons);
                        mapper.setOnMapperEventListener(onKeyDetected());
                        mapper.show();
                        break;
                    }
                    case R.id.mapper_delbutton: {
                        isDeleteState = !isDeleteState;
                        switchCheckers();
                        break;
                    }
                    case R.id.mapper_confirm: {
                        if (event != null) {
                            event.onPick(buttons);
                        }

                        dismiss();
                        break;
                    }
                }
            }
        };
    }

    private MapperEventListener onKeyDetected() {
        return new MapperEventListener() {
            @Override
            public void onPick(int keyCode) {
                MapperProfileItem item = new MapperProfileItem(keyCode);
                buttonsLayout.addView(getButton(li, "", item));
                buttons.add(item);

                removeEmptyListInformation();
//TODO FIXME
/*
                scroll_buttons.post(new Runnable() {
                    public void run() {
                        scroll_buttons.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });*/
            }
        };
    }

    private void finishDeleteState() {
        if (isDeleteState) {
            isDeleteState = false;
            switchCheckers();
        }
    }

    private void switchCheckers() {
        if (buttonsLayout.getChildCount() == 1 && buttonsLayout.getChildAt(0) instanceof TextView) {
            return;
        }

        List<LinearLayout> deletedViews = null;

        for (int i = 0; i < buttonsLayout.getChildCount(); i++) {
            LinearLayout view = (LinearLayout) buttonsLayout.getChildAt(i);
            CheckBox cbx = (CheckBox) view.findViewById(R.id.mapper_item_checker);

            if (isDeleteState) {
                cbx.setChecked(false);
                cbx.setVisibility(View.VISIBLE);
            } else {
                cbx.setVisibility(View.INVISIBLE);

                if (deletedViews == null)
                    deletedViews = new LinkedList<>();

                if (cbx.isChecked()) {
                    deletedViews.add(view);
                }
            }
        }

        if (deletedViews != null) {
            for (View view : deletedViews) {
                buttonsLayout.removeView(view);
                buttons.remove(view.getTag());
            }

            if (buttons.size() == 0) {
                addEmptyListInformation();
            }
        }
    }

    @Override
    protected void onButtonTypeConfigurationClick(View v) {
        finishDeleteState();
        super.onButtonTypeConfigurationClick(v);
    }

    @Override
    protected void onButtonSettingsConfiguration(View v) {
        finishDeleteState();
        super.onButtonSettingsConfiguration(v);
    }

    public void setOnGamepadButtonsDialogEventListener(GamepadButtonsDialogEventListener event) {
        this.event = event;
    }
}

class MapperDetector extends Dialog {
    private TextView keyCodeText;
    private TextView keyCodeInfo;
    private int detectedKeyCode = -1;
    private MapperEventListener event;
    private List<MapperProfileItem> buttons;
    private View toolBar;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mapper_detector_hint, "mapper_detector_hint");
        localize(R.id.mapper_detector_keycode_title, "mapper_detector_keycode_title");
    }

    public MapperDetector(Context context, List<MapperProfileItem> buttons) {
        this(context, buttons, true);
    }

    public MapperDetector(Context context, List<MapperProfileItem> buttons, boolean showKeyboard) {
        super(context);

        this.buttons = buttons;

        setContentView(R.layout.mapper_detector);
        setCaption("mapper_detector_caption");

        if (NavigationCursor.enabled) {
            toolBar = (View) findViewById(R.id.mapper_detector_panel);
        }

        keyCodeText = (TextView) findViewById(R.id.mapper_detector_keycode);
        keyCodeText.setText("");

        keyCodeInfo = (TextView) findViewById(R.id.mapper_detector_kecode_info);
        keyCodeInfo.setText("");

        ImageButton showkeyboard = (ImageButton) findViewById(R.id.mapper_detector_showvirtualkeyboard);
        if (NavigationCursor.enabled || !showKeyboard) {
            showkeyboard.setVisibility(View.GONE);
        } else {
            showkeyboard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                }
            });
        }

        ImageButton confirm = (ImageButton) findViewById(R.id.mapper_detector_confirm);
        confirm.setOnClickListener(confirmEvent());
    }

    @Override
    protected boolean onKeyEvent(int keyCode, KeyEvent event) {
        if (NavigationCursor.enabled) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    dismiss();
                }
                return true;
            }

            int position[] = new int[2];
            toolBar.getLocationOnScreen(position);
            float x = position[0];
            float y = position[1];

            float dw = toolBar.getWidth();
            float dh = toolBar.getHeight();

            if (NavigationCursor.positionX > x && NavigationCursor.positionX < (x + dw) && NavigationCursor.positionY > y && NavigationCursor.positionY < (y + dh)) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    return detectKey(keyCode, event);
                }
            } else {
                super.onKeyEvent(keyCode, event);
            }

            return true;
        } else {
            return detectKey(keyCode, event);
        }
    }

    private boolean detectKey(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                //case KeyEvent.KEYCODE_VOLUME_UP:
                //case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_HOME:
                case KeyEvent.KEYCODE_SEARCH:
                case KeyEvent.KEYCODE_UNKNOWN: {
                    return false;
                }
            }

            keyCodeText.setText("" + keyCode);
            keyCodeInfo.setText(KeyCodeInfo.getAndroidKeyInfo(keyCode, true));
            detectedKeyCode = keyCode;
        }
        return true;
    }

/*    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                dismiss();
                return true;
            }
            //case KeyEvent.KEYCODE_VOLUME_UP:
            //case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_UNKNOWN: {
                return false;
            }
        }

        keyCodeText.setText("" + keyCode);
        keyCodeInfo.setText(KeyCodeInfo.getAndroidKeyInfo(keyCode, true));
        detectedKeyCode = keyCode;

        return true;
    }
*/
    /*
    @Override
	public boolean onGenericMotionEvent(MotionEvent event)
	{
		return true;
	}*/

    public void setOnMapperEventListener(MapperEventListener event) {
        this.event = event;
    }

    private View.OnClickListener confirmEvent() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (event == null || detectedKeyCode == -1) {
                    return;
                }
                if (buttons != null) {
                    for (MapperProfileItem item : buttons) {
                        if (item.key == detectedKeyCode) {
                            MessageInfo.info("mapper_detector_already_mapped");
                            return;
                        }
                    }
                }
                event.onPick(detectedKeyCode);
                dismiss();
            }
        };
    }
}

class MapperDialog extends Dialog {
    protected View.OnClickListener buttonEvent;
    protected View.OnClickListener itemSettingsEvent;
    protected LayoutInflater li;
    private List<ImageViewerItem> boundableWidgets;

    public MapperDialog(Context context) {
        super(context);
        li = getLayoutInflater();
    }

    protected List getBoundableWidgets() {
        if (boundableWidgets == null) {
            boundableWidgets = new LinkedList<>();

            AppGlobal.addAvailableMappings(boundableWidgets,
                    new WidgetType[]{
                            WidgetType.combo
                    },
                    true);
        }
        return boundableWidgets;
    }

    protected LinkedList<MapperProfileItem> duplicateMapperProfileItemArray(List<MapperProfileItem> arrayFrom) {
        LinkedList<MapperProfileItem> arrayTo = new LinkedList<>();

        for (MapperProfileItem b : arrayFrom) {
            MapperProfileItem button = new MapperProfileItem();
            b.copyTo(button);

            arrayTo.add(button);
        }

        return arrayTo;
    }

    protected View getButton(LayoutInflater li, String title, MapperProfileItem item) {
        View v = li.inflate(R.layout.mapper_item, null);
        v.setTag(item);

        TextView buttonTitle = (TextView) v.findViewById(R.id.mapper_item_keycode_title);

        if (title.equals("")) {
            buttonTitle.setText(KeyCodeInfo.getAndroidKeyInfo(item.key, true));
        } else {
            buttonTitle.setText(title);
        }

        CheckBox cbx = (CheckBox) v.findViewById(R.id.mapper_item_checker);
        cbx.setTag(item);

        Button button = (Button) v.findViewById(R.id.mapper_item_button);

        switch (item.type) {
            case none: {
                button.setText(Localization.getString("common_none"));
                break;
            }
            case key: {
                button.setText(KeyCodeInfo.getDosboxKeyInfo(item.dosboxKey.getKeyCode(), item.dosboxKey.shift));
                break;
            }
            case mouse: {
                switch (item.mouseButton) {
                    case left: {
                        button.setText(Localization.getString("common_click_left"));
                        break;
                    }
                    case right: {
                        button.setText(Localization.getString("common_click_right"));
                        break;
                    }
                    default:
                        break;
                }
                break;
            }
            case keyboard: {
                button.setText(Localization.getString("common_show_keyboard"));
                break;
            }
            case gamepadButton: {
                button.setText(Localization.getString("common_button" + (item.gamepadButton + 1)));
                break;
            }
            case widget: {
                button.setText(item.widget.getText());
            }
        }

        button.setOnClickListener(getButtonEvent());
        button.setTag(item);

        ImageButton settingsButton = (ImageButton) v.findViewById(R.id.mapper_item_settings);
        settingsButton.setOnClickListener(getItemSettingsEvent());
        settingsButton.setTag(item);

        return v;
    }

    protected void onButtonTypeConfigurationClick(View v) {
        final Button clickedButton = (Button) v;
        final MapperProfileItem clickedItem = (MapperProfileItem) clickedButton.getTag();
        final uiImageViewer viewer = new uiImageViewer(getContext());

        viewer.setCaption("imgview_caption_what_map");

        viewer.setOnImageViewerDisabledEventListener(new ImageViewer.ImageViewerDisabledEventListener() {
            @Override
            public void onPick(ImageViewerItem selected) {
                MessageInfo.info("msg_donated_feature");
            }
        });

        viewer.setOnImageViewerItemsSetter(new ImageViewer.ImageViewerItemsSetter() {
            @Override
            public boolean onSet(List images) {
                images.add(new ImageViewerItem(R.drawable.icon_cancel, "unmap", "common_unmap"));
                images.add(new ImageViewerItem(R.drawable.img_key, "key", "common_key"));

                if (clickedItem.key > -1) {
                    images.add(new ImageViewerItem(R.drawable.img_leftmouse, "left_click", "common_click_left"));
                    images.add(new ImageViewerItem(R.drawable.img_rightmouse, "right_click", "common_click_right"));
                    images.add(new ImageViewerItem(R.drawable.icon_gamepad_button, "nativejoybut", "common_gamepad_buttons"));
                    images.add(new ImageViewerItem(R.drawable.img_keyboard, "keyboard", "common_show_keyboard"));
                    images.add(new ImageViewerItem(R.drawable.icon_widgets, "widget", "common_widget", false, !Global.isDonated));
                }

                return true;
            }
        });

        viewer.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
            @Override
            public void onPick(ImageViewerItem selected) {
                if (selected.getName().equals("unmap")) {
                    clickedButton.setText(Localization.getString("common_none"));

                    clickedItem.type = MapperProfileItemType.none;
                    clickedItem.dosboxKey = null;
                    clickedItem.mouseButton = MouseButton.none;
                    clickedItem.dblClick = false;
                } else if (selected.getName().equals("key")) {
                    viewer.dismiss();

                    uiKeyCodesDialog d = new uiKeyCodesDialog(getContext());
                    d.setOnKeyCodeListener(new KeyCodeListener() {
                        @Override
                        public void onPick(KeyCodeItem selected) {
                            //clickedButton.setText(selected.getText());

                            clickedItem.type = MapperProfileItemType.key;

                            if (clickedItem.dosboxKey == null) {
                                clickedItem.dosboxKey = new Key(selected.getKeyCode());
                            } else {
                                clickedItem.dosboxKey.setKeyCode(selected.getKeyCode());
                                //clickedItem.dosboxKey.setCtrl(false);
                                //clickedItem.dosboxKey.setAlt(false);
                                //clickedItem.dosboxKey.setShift(false);
                            }

                            clickedItem.mouseButton = MouseButton.none;
                            clickedButton.setText(KeyCodeInfo.getDosboxKeyInfo(
                                            clickedItem.dosboxKey.keyCode,
                                            clickedItem.dosboxKey.shift)
                            );
                        }
                    });

                    d.show();
                } else if (selected.getName().equals("left_click")) {
                    clickedButton.setText(Localization.getString("common_click_left"));

                    clickedItem.type = MapperProfileItemType.mouse;
                    clickedItem.dosboxKey = null;
                    clickedItem.mouseButton = MouseButton.left;
                } else if (selected.getName().equals("right_click")) {
                    clickedButton.setText(Localization.getString("common_click_right"));

                    clickedItem.type = MapperProfileItemType.mouse;
                    clickedItem.dosboxKey = null;
                    clickedItem.mouseButton = MouseButton.right;
                } else if (selected.getName().equals("keyboard")) {
                    clickedButton.setText(Localization.getString("common_show_keyboard"));

                    clickedItem.type = MapperProfileItemType.keyboard;
                    clickedItem.dosboxKey = null;
                    clickedItem.mouseButton = MouseButton.none;
                } else if (selected.getName().equals("widget")) {
                    viewer.dismiss();

                    List widgets = getBoundableWidgets();

                    if (widgets == null || widgets.size() == 0) {
                        MessageInfo.info("msg_no_widgets_to_bind");
                    } else {
                        uiImageViewer widgetPicker = new uiImageViewer(getContext());
                        widgetPicker.setCaption("common_buttons");
                        widgetPicker.useItemBackground = true;
                        widgetPicker.initAdapter(widgets);

                        widgetPicker.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
                            @Override
                            public void onPick(ImageViewerItem selected) {
                                clickedItem.type = MapperProfileItemType.widget;
                                clickedItem.widget = (Widget) selected.getTag();
                                clickedItem.widgetID = clickedItem.widget.getName();
                                clickedItem.dosboxKey = null;
                                clickedItem.mouseButton = MouseButton.none;

                                clickedButton.setText(selected.getDescription());
                            }
                        });
                        widgetPicker.show();
                    }
                } else if (selected.getName().equals("nativejoybut")) {
                    //clickedButton.setText("Gamepad button");
                    viewer.dismiss();

                    uiImageViewer buttonPicker = new uiImageViewer(getContext());
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

                    buttonPicker.setOnImageViewerEventListener(new ImageViewer.ImageViewerEventListener() {
                        @Override
                        public void onPick(ImageViewerItem selected) {
                            clickedItem.type = MapperProfileItemType.gamepadButton;
                            clickedItem.dosboxKey = null;
                            clickedItem.mouseButton = MouseButton.none;

                            clickedButton.setText(selected.getDescription());
                            clickedItem.gamepadButton = Integer.parseInt(selected.getName());
                        }
                    });
                    buttonPicker.show();
                }
            }
        });

        viewer.show();
    }

    protected View.OnClickListener getButtonEvent() {
        if (buttonEvent != null) {
            return buttonEvent;
        }

        buttonEvent = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonTypeConfigurationClick(v);
            }
        };

        return buttonEvent;
    }

    protected void onButtonSettingsConfiguration(View v) {
        final ImageButton b = (ImageButton) v;
        final MapperProfileItem item = (MapperProfileItem) b.getTag();

        if (item.type == MapperProfileItemType.key) {
            KeySettings keySettings = new KeySettings(getContext(), item.dosboxKey, null, true, false);
            keySettings.setKeySettingsEventListener(new KeySettingsEventListener() {
                @Override
                public void onChange(boolean ctrl, boolean alt, boolean shift, KeyAction action) {
                    item.dosboxKey.ctrl = ctrl;
                    item.dosboxKey.alt = alt;

                    if (item.dosboxKey.shift != shift) {
                        item.dosboxKey.shift = shift;

                        ((Button) ((View) b.getParent()).findViewById(R.id.mapper_item_button)).setText(
                                KeyCodeInfo.getDosboxKeyInfo(item.dosboxKey.keyCode,
                                        item.dosboxKey.shift)
                        );
                    }
                }
            });

            keySettings.show();
        } else if (item.type == MapperProfileItemType.mouse) {
            MouseMapperSettings mouseSettings = new MouseMapperSettings(getContext(), item);
            mouseSettings.show();
        }
    }

    protected View.OnClickListener getItemSettingsEvent() {
        if (itemSettingsEvent != null) {
            return itemSettingsEvent;
        }

        itemSettingsEvent = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonSettingsConfiguration(v);
            }
        };

        return itemSettingsEvent;
    }
}

class MouseMapperSettings extends Dialog {
    private MapperProfileItem mouseSettings;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mouse_mapper_settings_dblclick, "common_click_double");
    }

    public MouseMapperSettings(Context context, MapperProfileItem settings) {
        super(context);

        setContentView(R.layout.mouse_mapper_settings);
        setCaption("common_settings");

        this.mouseSettings = settings;

        final CheckBox cbx = (CheckBox) findViewById(R.id.mouse_mapper_settings_dblclick);
        cbx.setChecked(mouseSettings.dblClick);

        ImageButton confirm = (ImageButton) findViewById(R.id.mouse_mapper_settings_confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mouseSettings.dblClick = cbx.isChecked();
                dismiss();
            }
        });
    }

}

interface KeyPadDialogEventListener {
    void onPick(List<MapperProfileItem> buttons, boolean diagonalsOn, int deadZone);
}

class KeyPadSettings extends MapperDialog {
    private KeyPadDialogEventListener event;
    private List<MapperProfileItem> buttons;
    private LinearLayout buttonsLayout;
    private CheckBox diagonals;
    private TextView deadZoneView;
    private Dialog parent;

    private int deadZone;

    @Override
    protected void onStop() {
        super.onStop();
        parent.show();
    }

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mapper_diagonals, "common_diagonals_on");
        localize(R.id.mapper_buttons_title, "directions");
        localize(R.id.mapper_deadzone_title, "common_deadzone");
    }

    public KeyPadSettings(Dialog parent, List<MapperProfileItem> buttons, boolean diagonalsOn, int deadZone) {
        super(AppGlobal.context);

        setContentView(R.layout.mapper_keypad);
        setCaption("common_keypad");

        this.parent = parent;
        buttonsLayout = (LinearLayout) findViewById(R.id.mapper_buttons);

        this.deadZone = deadZone;
        this.diagonals = (CheckBox) findViewById(R.id.mapper_diagonals);
        this.diagonals.setChecked(diagonalsOn);

        if (deadZone == -1) {
            findViewById(R.id.mapper_deadzone_title).setVisibility(View.GONE);
            findViewById(R.id.mapper_deadzone_panel).setVisibility(View.GONE);
        } else {
            this.deadZoneView = (TextView) findViewById(R.id.mapper_deadzone_value);
            this.deadZoneView.setText("" + ((float) deadZone) / 100);

            findViewById(R.id.mapper_deadzone_minus).setOnClickListener(onClick());
            findViewById(R.id.mapper_deadzone_plus).setOnClickListener(onClick());
        }

        this.buttons = duplicateMapperProfileItemArray(buttons);
        int size = this.buttons.size();
        for (int i = 0; i < size; i++) {
            String buttonTitle;

            switch (i) {
                case 0:
                    buttonTitle = Localization.getString("direction_up");
                    break;
                case 1:
                    buttonTitle = Localization.getString("direction_down");
                    break;
                case 2:
                    buttonTitle = Localization.getString("direction_left");
                    break;
                case 3:
                    buttonTitle = Localization.getString("direction_right");
                    break;
                default:
                    buttonTitle = "";
            }

            buttonsLayout.addView(getButton(li, buttonTitle, this.buttons.get(i)));
        }

        findViewById(R.id.mapper_confirm).setOnClickListener(onClick());
    }

    private View.OnClickListener onClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.mapper_confirm: {
                        dismiss();

                        if (event != null) {
                            event.onPick(buttons, diagonals.isChecked(), deadZone);
                        }
                        break;
                    }
                    case R.id.mapper_deadzone_minus: {
                        deadZone -= 1;

                        if (deadZone < 15)
                            deadZone = 15;

                        deadZoneView.setText("" + ((float) deadZone) / 100);
                        break;
                    }
                    case R.id.mapper_deadzone_plus: {
                        deadZone += 1.0f;

                        if (deadZone > 35)
                            deadZone = 35;

                        deadZoneView.setText("" + ((float) deadZone) / 100);
                        break;
                    }
                }
            }
        };
    }

    public void setOnKeyPadDialogEventListener(KeyPadDialogEventListener event) {
        this.event = event;
    }
}

interface MouseDialogEventListener {
    void onPick(int rate);
}

class MouseSettings extends MapperDialog {
    private MouseDialogEventListener event;
    private int rate;
    private View.OnClickListener onClick;
    private TextView rateView;
    private Dialog parent;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mapper_rj_rate_title, "mapper_rj_rate_title");
    }

    @Override
    protected void onStop() {
        super.onStop();
        parent.show();
    }

    public MouseSettings(Dialog parent, int rate) {
        super(AppGlobal.context);

        setContentView(R.layout.mapper_mouse);
        setCaption("common_mouse");

        this.rate = rate;
        this.parent = parent;
        rateView = (TextView) findViewById(R.id.mapper_mouse_rate_value);
        rateView.setText("" + this.rate);

        findViewById(R.id.mapper_confirm).setOnClickListener(onClick());
        findViewById(R.id.mapper_mouse_rate_minus).setOnClickListener(onClick());
        findViewById(R.id.mapper_mouse_rate_plus).setOnClickListener(onClick());
    }

    private View.OnClickListener onClick() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.mapper_mouse_rate_minus: {
                            rate--;

                            if (rate < 3)
                                rate = 3;

                            rateView.setText("" + rate);
                            break;
                        }
                        case R.id.mapper_mouse_rate_plus: {
                            rate++;

                            if (rate > 120)
                                rate = 120;

                            rateView.setText("" + rate);
                            break;
                        }
                        case R.id.mapper_confirm: {
                            dismiss();

                            if (event != null) {
                                event.onPick(rate);
                            }
                            break;
                        }
                    }
                }
            };
        }

        return onClick;
    }

    public void setOnMouseDialogEventListener(MouseDialogEventListener event) {
        this.event = event;
    }
}

class L2R2Settings extends MapperDialog {
    private List<MapperProfileItem> buttons;
    private LinearLayout buttonsLayout;
    private GamepadButtonsDialogEventListener event;
    private View.OnClickListener onClick;
    private Dialog parent;

    @Override
    protected void onStop() {
        super.onStop();
        parent.show();
    }

    public L2R2Settings(Dialog parent, List<MapperProfileItem> buttons) {
        super(AppGlobal.context);

        setContentView(R.layout.mapper_l2r2);
        setCaption("mapper_axis_hint");

        buttonsLayout = (LinearLayout) findViewById(R.id.mapper_buttons);

        this.parent = parent;
        this.buttons = duplicateMapperProfileItemArray(buttons);

        int size = this.buttons.size();
        String buttonTitle;

        for (int i = 0; i < size; i++) {
            switch (i) {
                case 0:
                    buttonTitle = KeyCodeInfo.getAndroidKeyInfo(104, false);
                    break;
                case 1:
                    buttonTitle = KeyCodeInfo.getAndroidKeyInfo(105, false);
                    break;
                default:
                    buttonTitle = "";
            }

            buttonsLayout.addView(getButton(li, buttonTitle, this.buttons.get(i)));
        }
        findViewById(R.id.mapper_confirm).setOnClickListener(onClick());

    }

    public void setOnGamepadButtonsDialogEventListener(GamepadButtonsDialogEventListener event) {
        this.event = event;
    }

    private View.OnClickListener onClick() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.mapper_confirm: {
                            dismiss();

                            if (event != null) {
                                event.onPick(buttons);
                            }
                            break;
                        }
                    }
                }
            };
        }

        return onClick;
    }
}

interface TiltDialogEventListener {
    void onPick(List<MapperProfileItem> buttons, double deadZone, boolean updown, int activeAngle, int activeAngleStart, int activeAngleMiddle, int activeAngleEnd);
}

class TiltSettings extends MapperDialog {
    private List<MapperProfileItem> buttons;
    private double deadZone;
    private CheckBox upDown;
    private TextView deadZoneView;
    private Dialog parent;

    private LinearLayout buttonsLayout;
    private TiltDialogEventListener event;
    private View.OnClickListener onClick;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mapper_deadzone_title, "common_deadzone");
        localize(R.id.mapper_tiltupdown_enabled, "mapper_tilt_updown_enabled");
    }

    @Override
    protected void onStop() {
        super.onStop();
        parent.show();
    }

    public TiltSettings(Dialog parent, List<MapperProfileItem> buttons, double deadZone, boolean updown) {
        super(AppGlobal.context);

        setContentView(R.layout.mapper_tilt);
        setCaption("mapper_tilt_title");

        this.parent = parent;

        upDown = (CheckBox) findViewById(R.id.mapper_tiltupdown_enabled);
        upDown.setChecked(updown);

        deadZoneView = (TextView) findViewById(R.id.mapper_deadzone_value);
        deadZoneView.setText("" + deadZone);

        buttonsLayout = (LinearLayout) findViewById(R.id.mapper_buttons);
        this.buttons = duplicateMapperProfileItemArray(buttons);
        this.deadZone = deadZone;

        String buttonTitle;

        for (int i = 0; i < 4; i++) {
            switch (i) {
                case 0:
                    buttonTitle = Localization.getString("direction_up");
                    break;
                case 1:
                    buttonTitle = Localization.getString("direction_down");
                    break;
                case 2:
                    buttonTitle = Localization.getString("direction_left");
                    break;
                case 3:
                    buttonTitle = Localization.getString("direction_right");
                    break;
                default:
                    buttonTitle = "";
            }

            buttonsLayout.addView(getButton(li, buttonTitle, this.buttons.get(i)));
        }

        findViewById(R.id.mapper_confirm).setOnClickListener(onClick());
        findViewById(R.id.mapper_deadzone_minus).setOnClickListener(onClick());
        findViewById(R.id.mapper_deadzone_plus).setOnClickListener(onClick());
    }

    public void setOnTiltDialogEventListener(TiltDialogEventListener event) {
        this.event = event;
    }

    private View.OnClickListener onClick() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.mapper_confirm: {
                            dismiss();

                            if (event != null) {
                                event.onPick(buttons, deadZone, upDown.isChecked(), 0, 0, 0, 0);
                            }
                            break;
                        }
                        case R.id.mapper_deadzone_minus: {
                            deadZone = (deadZone * 10 - 1) / 10;

                            if (deadZone < 0)
                                deadZone = 0;

                            deadZoneView.setText("" + deadZone);
                            break;
                        }
                        case R.id.mapper_deadzone_plus: {
                            deadZone = (deadZone * 10 + 1) / 10;

                            if (deadZone > 10)
                                deadZone = 10;

                            deadZoneView.setText("" + deadZone);
                            break;
                        }
                    }
                }
            };
        }

        return onClick;
    }
}

class Tilt2AxisSettings extends MapperDialog {
    private Dialog parent;
    private TiltDialogEventListener event;
    private View.OnClickListener onClick;
    private TextView activeAngleView;
    private TextView activeAngleStartView;
    private TextView activeAngleMiddleView;
    private TextView activeAngleEndView;
    private CheckBox udEnabledView;

    private int activeAngle;
    private int activeAngleStart;
    private int activeAngleMiddle;
    private int activeAngleEnd;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.mapper_tilt_lr_title, "mapper_tilt_leftright");
        localize(R.id.mapper_tilt_activeangle_title, "mapper_tilt_activeangle");
        localize(R.id.mapper_tilt_ud_title, "mapper_tilt_activeangle");
        localize(R.id.mapper_tiltupdown_enabled, "common_enabled");
        localize(R.id.mapper_tilt_ud_title, "mapper_tilt_ud");
        localize(R.id.mapper_tilt_udactiveanglestart_title, "mapper_tilt_activeanglestart");
        localize(R.id.mapper_tilt_udactiveanglemiddle_title, "mapper_tilt_activeanglemiddle");
        localize(R.id.mapper_tilt_udactiveangleend_title, "mapper_tilt_activeangleend");
    }

    @Override
    protected void onStop() {
        super.onStop();
        parent.show();
    }

    public Tilt2AxisSettings(Dialog parent, int activeAngle, boolean udEnabled, int activeAngleStart, int activeAngleMiddle, int activeAngleEnd) {
        super(AppGlobal.context);

        setContentView(R.layout.mapper_tilt_2axis);
        setCaption("mapper_tilt_title");

        this.parent = parent;

        this.activeAngle = activeAngle;
        this.activeAngleStart = activeAngleStart;
        this.activeAngleMiddle = activeAngleMiddle;
        this.activeAngleEnd = activeAngleEnd;

        activeAngleView = (TextView)findViewById(R.id.mapper_tilt_activeangle_value);
        activeAngleStartView = (TextView)findViewById(R.id.mapper_tilt_activeanglestart_value);
        activeAngleMiddleView = (TextView)findViewById(R.id.mapper_tilt_activeanglemiddle_value);
        activeAngleEndView = (TextView)findViewById(R.id.mapper_tilt_activeangleend_value);

        activeAngleView.setText("" + activeAngle);
        activeAngleStartView.setText("" + activeAngleStart);
        activeAngleMiddleView.setText("" + activeAngleMiddle);
        activeAngleEndView.setText("" + activeAngleEnd);

        udEnabledView = (CheckBox)findViewById(R.id.mapper_tiltupdown_enabled);
        udEnabledView.setChecked(udEnabled);

        findViewById(R.id.mapper_tilt_activeangle_minus).setOnClickListener(onClick());
        findViewById(R.id.mapper_tilt_activeangle_plus).setOnClickListener(onClick());

        findViewById(R.id.mapper_tilt_activeanglestart_minus).setOnClickListener(onClick());
        findViewById(R.id.mapper_tilt_activeanglestart_plus).setOnClickListener(onClick());

        findViewById(R.id.mapper_tilt_activeanglemiddle_minus).setOnClickListener(onClick());
        findViewById(R.id.mapper_tilt_activeanglemiddle_plus).setOnClickListener(onClick());

        findViewById(R.id.mapper_tilt_activeangleend_minus).setOnClickListener(onClick());
        findViewById(R.id.mapper_tilt_activeangleend_plus).setOnClickListener(onClick());

        findViewById(R.id.mapper_confirm).setOnClickListener(onClick());
    }

    public void setOnTiltDialogEventListener(TiltDialogEventListener event) {
        this.event = event;
    }

    private View.OnClickListener onClick() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.mapper_confirm: {
                            dismiss();

                            if (event != null) {
                                event.onPick(null, -1, udEnabledView.isChecked(), activeAngle, activeAngleStart, activeAngleMiddle, activeAngleEnd);
                            }
                            break;
                        }
                        case R.id.mapper_tilt_activeangle_minus: {
                            activeAngle -= 5;

                            if (activeAngle < 35)
                                activeAngle = 35;

                            activeAngleView.setText("" + activeAngle);
                            break;
                        }
                        case R.id.mapper_tilt_activeangle_plus: {
                            activeAngle += 5;

                            if (activeAngle < 75)
                                activeAngle = 75;

                            activeAngleView.setText("" + activeAngle);
                            break;
                        }
                        //start up down
                        case R.id.mapper_tilt_activeanglestart_minus: {
                            activeAngleStart -= 5;

                            if (activeAngleStart < 35) {
                                activeAngleStart = 35;
                            }

                            activeAngleStartView.setText("" + activeAngleStart);
                            break;
                        }
                        case R.id.mapper_tilt_activeanglestart_plus: {
                            activeAngleStart += 5;

                            if (activeAngleStart >= activeAngleMiddle - 10) {
                                activeAngleStart = activeAngleMiddle - 10;
                            }

                            activeAngleStartView.setText("" + activeAngleStart);
                            break;
                        }
                        //middle
                        case R.id.mapper_tilt_activeanglemiddle_minus: {
                            activeAngleMiddle -= 5;

                            if (activeAngleMiddle < activeAngleStart + 10)
                                activeAngleMiddle = activeAngleStart + 10;

                            activeAngleMiddleView.setText("" + activeAngleMiddle);
                            break;
                        }
                        case R.id.mapper_tilt_activeanglemiddle_plus: {
                            activeAngleMiddle += 5;

                            if (activeAngleMiddle > activeAngleEnd - 10)
                                activeAngleMiddle = activeAngleEnd - 10;

                            activeAngleMiddleView.setText("" + activeAngleMiddle);
                            break;
                        }
                        //end
                        case R.id.mapper_tilt_activeangleend_minus: {
                            activeAngleEnd -= 5;

                            if (activeAngleEnd < activeAngleMiddle + 10)
                                activeAngleEnd = activeAngleMiddle + 10;

                            activeAngleEndView.setText("" + activeAngleEnd);
                            break;
                        }
                        case R.id.mapper_tilt_activeangleend_plus: {
                            activeAngleEnd += 5;

                            if (activeAngleEnd > 90)
                                activeAngleEnd = 90;

                            activeAngleEndView.setText("" + activeAngleEnd);
                            break;
                        }
                    }
                }
            };
        }

        return onClick;
    }
}