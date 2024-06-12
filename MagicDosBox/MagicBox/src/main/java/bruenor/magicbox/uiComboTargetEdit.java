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
import android.widget.ImageButton;
import android.widget.TextView;

import magiclib.controls.Dialog;
import magiclib.locales.Localization;
import magiclib.layout.widgets.PointClickAction;
import magiclib.layout.widgets.TargetComboAction;
import magiclib.mouse.MouseButton;

class uiComboTargetEdit extends Dialog
{
    abstract interface ConfirmEventListener
    {
        public abstract void onConfirm(PointClickAction action, MouseButton button);
    }

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.combo_target_action_caption,   "common_action");
        localize(R.id.combo_target_button_caption, "common_button");
    }

    private PointClickAction action;
    private MouseButton button;
    private TextView actionTitle;
    private TextView buttonTitle;
    private ConfirmEventListener event;

    public uiComboTargetEdit(Context context, TargetComboAction action, ConfirmEventListener event) {
        super(context);

        setContentView(R.layout.combo_target_edit);
        setCaption("widget_edit_combo_menu_target");

        this.action = action.action;
        this.button = action.mouseButton;
        this.event = event;

        View.OnClickListener clickEvent = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.combo_target_action_minus:
                    {
                        minusAction();
                        break;
                    }
                    case R.id.combo_target_action_plus:
                    {
                        plusAction();
                        break;
                    }
                    case R.id.combo_target_button_minus:
                    {
                        minusButton();
                        break;
                    }
                    case R.id.combo_target_button_plus:
                    {
                        plusButton();
                        break;
                    }
                    case R.id.combo_target_confirm: {
                        confirm();
                        break;
                    }
                }
            }
        };

        ImageButton changeButton = (ImageButton)getView().findViewById(R.id.combo_target_action_minus);
        changeButton.setOnClickListener(clickEvent);

        changeButton = (ImageButton)getView().findViewById(R.id.combo_target_action_plus);
        changeButton.setOnClickListener(clickEvent);

        changeButton = (ImageButton)getView().findViewById(R.id.combo_target_button_minus);
        changeButton.setOnClickListener(clickEvent);

        changeButton = (ImageButton)getView().findViewById(R.id.combo_target_button_plus);
        changeButton.setOnClickListener(clickEvent);

        changeButton = (ImageButton)getView().findViewById(R.id.combo_target_confirm);
        changeButton.setOnClickListener(clickEvent);

        actionTitle = (TextView)findViewById(R.id.combo_target_action_value);
        buttonTitle = (TextView)findViewById(R.id.combo_target_button_value);

        setActionTitle();
        setButtonTitle();
    }

    private void confirm() {
        if (event!=null) {
            event.onConfirm(action, button);
        }

        dismiss();
    }

    private void minusAction() {
        if (action == PointClickAction.click) {
            return;
        }

        if (action == PointClickAction.up) {
            action = PointClickAction.down;
        } else if (action == PointClickAction.down) {
            action = PointClickAction.move;
        } else {
            action = PointClickAction.click;
        }

        setActionTitle();
    }

    private void plusAction() {
        if (action == PointClickAction.up) {
            return;
        }

        if (action == PointClickAction.click) {
            action = PointClickAction.move;
        } else if (action == PointClickAction.move) {
            action = PointClickAction.down;
        } else {
            action = PointClickAction.up;
        }

        setActionTitle();
    }

    private void minusButton() {
        if (button == MouseButton.left) {
            return;
        }

        if (button == MouseButton.middle) {
            button = MouseButton.right;
        } else {
            button = MouseButton.left;
        }

        setButtonTitle();
    }

    private void plusButton() {
        if (button == MouseButton.middle) {
            return;
        }

        if (button == MouseButton.left) {
            button = MouseButton.right;
        } else {
            button = MouseButton.middle;
        }

        setButtonTitle();
    }

    private void setActionTitle() {
        String text;
        switch (action) {
            case click: {
                text = Localization.getString("common_click");
                break;
            }
            case move: {
                text = Localization.getString("common_move");
                break;
            }
            case down: {
                text = Localization.getString("direction_down");
                break;
            }
            case up: {
                text = Localization.getString("direction_up");
                break;
            }
            default: {
                text = "";
            }
        }

        actionTitle.setText(text);
    }

    private void setButtonTitle() {
        String text;
        switch (button) {
            case left: {
                text = Localization.getString("mouse_button_sleft");
                break;
            }
            case middle: {
                text = Localization.getString("mouse_button_smiddle");
                break;
            }
            case right: {
                text = Localization.getString("mouse_button_sright");
                break;
            }
            default: {
                text = "";
            }
        }

        buttonTitle.setText(text);
    }
}