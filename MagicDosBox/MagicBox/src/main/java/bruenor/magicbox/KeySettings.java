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
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import magiclib.controls.Dialog;
import magiclib.keyboard.Key;
import magiclib.keyboard.KeyAction;

abstract interface KeySettingsEventListener {
    public abstract void onChange(boolean ctrl, boolean alt, boolean shift, KeyAction action);
}


class KeySettings extends Dialog {
    private CheckBox ctrl;
    private CheckBox alt;
    private CheckBox shift;
    private KeySettingsEventListener event;
    private KeyAction action = KeyAction.down_up;
    private TextView actionTitle;
    boolean showModifier;
    boolean showAction;

    @Override
    public void onSetLocalizedLayout() {
        localize(R.id.key_settings_ctrl, "common_ctrl");
        localize(R.id.key_settings_alt, "common_alt");
        localize(R.id.key_settings_shift, "common_shift");
        localize(R.id.key_settings_modifier_title, "common_modifier");
        localize(R.id.key_settings_action_title, "common_action");
    }

    public KeySettings(Context context, Key key, KeyAction action, boolean showModifier, boolean showAction) {
        super(context);

        setContentView(R.layout.key_settings);
        setCaption("keyset_caption");

        this.showAction = showAction;

        if (showModifier) {
            findViewById(R.id.key_settings_modifier_panel).setVisibility(View.VISIBLE);

            ctrl = (CheckBox) findViewById(R.id.key_settings_ctrl);
            ctrl.setChecked(key.isCtrl());

            alt = (CheckBox) findViewById(R.id.key_settings_alt);
            alt.setChecked(key.isAlt());

            shift = (CheckBox) findViewById(R.id.key_settings_shift);
            shift.setChecked(key.isShift());
        } else {
            findViewById(R.id.key_settings_modifier_panel).setVisibility(View.GONE);
        }

        if (showAction) {
            this.action = action;

            findViewById(R.id.key_settings_action_panel).setVisibility(View.VISIBLE);
            actionTitle = (TextView) findViewById(R.id.key_settings_action_value);

            View.OnClickListener actionEvent = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.key_settings_action_minus: {
                            minusAction();
                            break;
                        }
                        case R.id.key_settings_action_plus: {
                            plusAction();
                            break;
                        }
                    }
                }
            };

            ImageButton actionPlus;
            ImageButton actionMinus;

            actionMinus = (ImageButton) getView().findViewById(R.id.key_settings_action_minus);
            actionMinus.setOnClickListener(actionEvent);

            actionPlus = (ImageButton) getView().findViewById(R.id.key_settings_action_plus);
            actionPlus.setOnClickListener(actionEvent);

            setActionTitle();
        } else {
            findViewById(R.id.key_settings_action_panel).setVisibility(View.GONE);
        }

        ImageButton confirmButton = (ImageButton) findViewById(R.id.key_settings_confirm);
        confirmButton.setOnClickListener(confirmEvent());
    }

    private View.OnClickListener confirmEvent() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (event != null) {
                    if (showAction) {
                        event.onChange(ctrl.isChecked(), alt.isChecked(), shift.isChecked(), action);
                    } else {
                        event.onChange(ctrl.isChecked(), alt.isChecked(), shift.isChecked(), null);
                    }
                }

                dismiss();
            }
        };
    }

    public void setKeySettingsEventListener(KeySettingsEventListener event) {
        this.event = event;
    }

    private void minusAction() {
        if (action == KeyAction.down_up) {
            return;
        }

        if (action == KeyAction.up) {
            action = KeyAction.down;
        } else if (action == KeyAction.down) {
            action = KeyAction.down_up;
        }

        setActionTitle();
    }

    private void plusAction() {
        if (action == KeyAction.up) {
            return;
        }

        if (action == KeyAction.down_up) {
            action = KeyAction.down;
        } else if (action == KeyAction.down) {
            action = KeyAction.up;
        }

        setActionTitle();
    }

    private void setActionTitle() {
        switch (action) {
            case down_up: {
                actionTitle.setText(getLocaleString("arrow_down") + getLocaleString("arrow_up"));
                break;
            }
            case down: {
                actionTitle.setText(getLocaleString("arrow_down"));
                break;
            }
            case up: {
                actionTitle.setText(getLocaleString("arrow_up"));
                break;
            }
        }
    }
}