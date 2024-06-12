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

import android.graphics.Color;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import magiclib.controls.Dialog;
import magiclib.controls.ZoomButton;
import magiclib.core.EmuConfig;
import magiclib.core.EmuManager;
import magiclib.core.NativeControl;
import magiclib.dosbox.Input;
import magiclib.locales.Localization;
import magiclib.mouse.MouseButton;
import magiclib.mouse.MouseType;

class uiMouseDialog extends Dialog
{
    private TextView mouseButtonDefaultValue;
    private TextView mouseButtonCurrenttValue;
    private CheckBox spenEnabled;
    private TextView spenButtonPrimaryValue;
    private TextView spenButtonSecondaryValue;
    private TextView sensitivityValue;
    private ImageView mouse_disabled;
    private ImageView mouse_absolute;
    private ImageView mouse_relative;
    private ImageView mouse_last_selected;
    private TextView mouse_text;
    private CheckBox physMouseWidgets;

    private MouseType temp_mousetype;
    private MouseButton temp_default_mousebutton;
    private MouseButton temp_current_mousebutton;

    private MouseButton temp_spen_primarybutton;
    private MouseButton temp_spen_secondarybutton;

    private int temp_sensitivity;

    public void onSetLocalizedLayout()
    {
        localize(R.id.mouse_caption, "common_mouse");
        localize(R.id.mouse_spen, "common_spen");
        localize(R.id.mouse_default_button, "common_defbutton");
        localize(R.id.mouse_default_button_starting, "mouse_button_starting");
        localize(R.id.mouse_default_button_current, "mouse_button_current");
        localize(R.id.mouse_spen_button_primary, "mouse_button_primary");
        localize(R.id.mouse_spen_button_secondary, "mouse_button_secondary");
        localize(R.id.mouse_spen_enabled, "common_enabled");
        localize(R.id.mouse_sensitivity, "common_sensitivity");
        localize(R.id.mouse_physical, "common_physmouse");
        localize(R.id.mouse_physmouse_enable_widgets, "mouse_phys_widgets");
    }

    public uiMouseDialog()
    {
        super(AppGlobal.context);

        //mouse
        View.OnClickListener mouseClickEvent = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mouse_last_selected.equals(v))
                    return;

                selectMouse(mouse_last_selected, (ImageView)v);
            }
        };

        setContentView(R.layout.mouse);
        setCaption("genset_menu_mouse");

        temp_sensitivity = EmuConfig.mouse_sensitivity;

        temp_mousetype = EmuConfig.mouse_type;
        temp_default_mousebutton = EmuConfig.mouse_button;
        temp_current_mousebutton = EmuManager.mouse_button;

        temp_spen_primarybutton = EmuConfig.spen_primary_button;
        temp_spen_secondarybutton = EmuConfig.spen_secondary_button;

        spenEnabled = (CheckBox)findViewById(R.id.mouse_spen_enabled);
        spenEnabled.setChecked(EmuConfig.spen_custom_enabled);

        mouseButtonDefaultValue = (TextView)findViewById(R.id.mouse_default_button_starting_value);
        setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);

        mouseButtonCurrenttValue = (TextView)findViewById(R.id.mouse_default_button_current_value);
        setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);

        spenButtonPrimaryValue = (TextView)findViewById(R.id.mouse_spen_button_primary_value);
        setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);

        spenButtonSecondaryValue = (TextView)findViewById(R.id.mouse_spen_button_secondary_value);
        setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);

        sensitivityValue = (TextView)findViewById(R.id.mouse_sensitivity_value);
        sensitivityValue.setText("" + temp_sensitivity);
        higlightMouseSensitivity();

        mouse_disabled = (ImageView)findViewById(R.id.mouse_disabled);
        mouse_absolute = (ImageView)findViewById(R.id.mouse_absolute);
        mouse_relative = (ImageView)findViewById(R.id.mouse_relative);
        mouse_text = (TextView)findViewById(R.id.mouse_title);

        mouse_disabled.setOnClickListener(mouseClickEvent);
        mouse_absolute.setOnClickListener(mouseClickEvent);
        mouse_relative.setOnClickListener(mouseClickEvent);

        switch (temp_mousetype) {
            case disabled: {
                mouse_last_selected = mouse_disabled;
                break;
            }
            case absolute: {
                mouse_last_selected = mouse_absolute;
                break;
            }
            case relative: {
                mouse_last_selected = mouse_relative;
                break;
            }
            default:
                break;
        }

        selectMouse(mouse_last_selected, mouse_last_selected);

        View.OnClickListener buttonTypeEvent = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.mouse_default_button_starting_minus:
                    {
                        minusStartingMouseButton();
                        break;
                    }
                    case R.id.mouse_default_button_starting_plus:
                    {
                        plusStartingMouseButton();
                        break;
                    }
                    case R.id.mouse_default_button_current_minus:
                    {
                        minusCurrentMouseButton();
                        break;
                    }
                    case R.id.mouse_default_button_current_plus:
                    {
                        plusCurrentMouseButton();
                        break;
                    }
                    case R.id.mouse_spen_button_primary_minus:
                    {
                        minusSpenPrimaryButton();
                        break;
                    }
                    case R.id.mouse_spen_button_primary_plus:
                    {
                        plusSpenPrimaryButton();
                        break;
                    }
                    case R.id.mouse_spen_button_secondary_minus:
                    {
                        minusSpenSecondaryButton();
                        break;
                    }
                    case R.id.mouse_spen_button_secondary_plus:
                    {
                        plusSpenSecondaryButton();
                        break;
                    }
                    case R.id.mouse_sensitivity_minus:
                    {
                        minusSensitivity();
                        break;
                    }
                    case R.id.mouse_sensitivity_plus:
                    {
                        plusSensitivity();
                        break;
                    }
                }
            }
        };

        ImageButton buttonMinus;
        ImageButton buttonPlus;

        buttonMinus = (ImageButton)findViewById(R.id.mouse_default_button_starting_minus);
        buttonMinus.setOnClickListener(buttonTypeEvent);

        buttonPlus = (ImageButton)findViewById(R.id.mouse_default_button_starting_plus);
        buttonPlus.setOnClickListener(buttonTypeEvent);

        buttonMinus = (ImageButton)findViewById(R.id.mouse_default_button_current_minus);
        buttonMinus.setOnClickListener(buttonTypeEvent);

        buttonPlus = (ImageButton)findViewById(R.id.mouse_default_button_current_plus);
        buttonPlus.setOnClickListener(buttonTypeEvent);

        buttonPlus = (ImageButton)findViewById(R.id.mouse_sensitivity_minus);
        buttonPlus.setOnClickListener(buttonTypeEvent);
        ((ZoomButton)buttonPlus).setZoomSpeed(1);

        buttonPlus = (ImageButton)findViewById(R.id.mouse_sensitivity_plus);
        buttonPlus.setOnClickListener(buttonTypeEvent);
        ((ZoomButton)buttonPlus).setZoomSpeed(1);

        //spen
        buttonMinus = (ImageButton)findViewById(R.id.mouse_spen_button_primary_minus);
        buttonMinus.setOnClickListener(buttonTypeEvent);

        buttonPlus = (ImageButton)findViewById(R.id.mouse_spen_button_primary_plus);
        buttonPlus.setOnClickListener(buttonTypeEvent);

        buttonMinus = (ImageButton)findViewById(R.id.mouse_spen_button_secondary_minus);
        buttonMinus.setOnClickListener(buttonTypeEvent);

        buttonPlus = (ImageButton)findViewById(R.id.mouse_spen_button_secondary_plus);
        buttonPlus.setOnClickListener(buttonTypeEvent);

        ImageButton confirm = (ImageButton)findViewById(R.id.confirm);
        confirm.setOnClickListener(confirmEvent());

        physMouseWidgets = (CheckBox)findViewById(R.id.mouse_physmouse_enable_widgets);
        physMouseWidgets.setChecked(EmuConfig.physMouseWorksWithWidgets);
    }

    private void selectMouse(ImageView lastSelected, ImageView newSelected)
    {
        lastSelected.setBackgroundResource(0);
        newSelected.setBackgroundResource(R.layout.layout_border_white);

        mouse_last_selected = newSelected;

        switch (mouse_last_selected.getId())
        {
            case R.id.mouse_disabled:
            {
                mouse_text.setText(Localization.getString("mouse_type_disabled"));
                mouse_text.setTextColor(Color.RED);
                temp_mousetype = MouseType.disabled;
                break;
            }
            case R.id.mouse_absolute:
            {
                mouse_text.setText(Localization.getString("mouse_type_absolute"));
                mouse_text.setTextColor(Color.CYAN);
                temp_mousetype = MouseType.absolute;
                break;
            }
            case R.id.mouse_relative:
            {
                mouse_text.setText(Localization.getString("mouse_type_relative"));
                mouse_text.setTextColor(Color.GREEN);
                temp_mousetype = MouseType.relative;
                break;
            }
        }
    }

    private View.OnClickListener confirmEvent()
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EmuConfig.mouse_type = temp_mousetype;
                EmuConfig.mouse_button = temp_default_mousebutton;
                EmuManager.mouse_button = temp_current_mousebutton;

                EmuConfig.spen_custom_enabled = spenEnabled.isChecked();
                EmuConfig.spen_primary_button = temp_spen_primarybutton;
                EmuConfig.spen_secondary_button = temp_spen_secondarybutton;
                EmuConfig.physMouseWorksWithWidgets = physMouseWidgets.isChecked();

                if (EmuConfig.mouse_sensitivity != temp_sensitivity) {
                    EmuConfig.mouse_sensitivity = temp_sensitivity;

                    MagicLauncher.nativeSetOption(16, EmuConfig.mouse_type == MouseType.absolute ? 100 : temp_sensitivity, null);
                }

                dismiss();

                if (EmuConfig.mouse_type == MouseType.absolute) {
                    Input.mouseCalibration();
                }
            }
        };
    }

    private void minusStartingMouseButton()
    {
        if (temp_default_mousebutton == MouseButton.left)
            return;

        if (temp_default_mousebutton == MouseButton.middle)
        {
            temp_default_mousebutton = MouseButton.left;
            setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);
            return;
        }

        if (temp_default_mousebutton == MouseButton.right)
        {
            temp_default_mousebutton = MouseButton.middle;
            setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);
            return;
        }

        if (temp_default_mousebutton == MouseButton.none)
        {
            temp_default_mousebutton = MouseButton.right;
            setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);
            return;
        }
    }

    private void plusStartingMouseButton()
    {
        if (temp_default_mousebutton == MouseButton.none)
            return;

        if (temp_default_mousebutton == MouseButton.left)
        {
            temp_default_mousebutton = MouseButton.middle;
            setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);
            return;
        }

        if (temp_default_mousebutton == MouseButton.middle)
        {
            temp_default_mousebutton = MouseButton.right;
            setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);
            return;
        }

        if (temp_default_mousebutton == MouseButton.right)
        {
            temp_default_mousebutton = MouseButton.none;
            setMouseButtonInfo(mouseButtonDefaultValue, temp_default_mousebutton);
            return;
        }
    }

    private void minusCurrentMouseButton()
    {
        if (temp_current_mousebutton == MouseButton.left)
            return;

        if (temp_current_mousebutton == MouseButton.middle)
        {
            temp_current_mousebutton = MouseButton.left;
            setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);
            return;
        }

        if (temp_current_mousebutton == MouseButton.right)
        {
            temp_current_mousebutton = MouseButton.middle;
            setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);
            return;
        }

        if (temp_current_mousebutton == MouseButton.none)
        {
            temp_current_mousebutton = MouseButton.right;
            setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);
            return;
        }
    }

    private void plusCurrentMouseButton()
    {
        if (temp_current_mousebutton == MouseButton.none)
            return;

        if (temp_current_mousebutton == MouseButton.left)
        {
            temp_current_mousebutton = MouseButton.middle;
            setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);
            return;
        }

        if (temp_current_mousebutton == MouseButton.middle)
        {
            temp_current_mousebutton = MouseButton.right;
            setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);
            return;
        }

        if (temp_current_mousebutton == MouseButton.right)
        {
            temp_current_mousebutton = MouseButton.none;
            setMouseButtonInfo(mouseButtonCurrenttValue, temp_current_mousebutton);
            return;
        }
    }

    private void minusSpenPrimaryButton()
    {
        if (temp_spen_primarybutton == MouseButton.left)
            return;

        if (temp_spen_primarybutton == MouseButton.middle)
        {
            temp_spen_primarybutton = MouseButton.left;
            setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);
            return;
        }

        if (temp_spen_primarybutton == MouseButton.right)
        {
            temp_spen_primarybutton = MouseButton.middle;
            setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);
            return;
        }

        if (temp_spen_primarybutton == MouseButton.none)
        {
            temp_spen_primarybutton = MouseButton.right;
            setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);
            return;
        }
    }

    private void plusSpenPrimaryButton()
    {
        if (temp_spen_primarybutton == MouseButton.none)
            return;

        if (temp_spen_primarybutton == MouseButton.left)
        {
            temp_spen_primarybutton = MouseButton.middle;
            setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);
            return;
        }

        if (temp_spen_primarybutton == MouseButton.middle)
        {
            temp_spen_primarybutton = MouseButton.right;
            setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);
            return;
        }

        if (temp_spen_primarybutton == MouseButton.right)
        {
            temp_spen_primarybutton = MouseButton.none;
            setMouseButtonInfo(spenButtonPrimaryValue, temp_spen_primarybutton);
            return;
        }
    }

    private void minusSpenSecondaryButton()
    {
        if (temp_spen_secondarybutton == MouseButton.left)
            return;

        if (temp_spen_secondarybutton == MouseButton.middle)
        {
            temp_spen_secondarybutton = MouseButton.left;
            setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);
            return;
        }

        if (temp_spen_secondarybutton == MouseButton.right)
        {
            temp_spen_secondarybutton = MouseButton.middle;
            setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);
            return;
        }

        if (temp_spen_secondarybutton == MouseButton.none)
        {
            temp_spen_secondarybutton = MouseButton.right;
            setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);
            return;
        }
    }

    private void plusSpenSecondaryButton()
    {
        if (temp_spen_secondarybutton == MouseButton.none)
            return;

        if (temp_spen_secondarybutton == MouseButton.left)
        {
            temp_spen_secondarybutton = MouseButton.middle;
            setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);
            return;
        }

        if (temp_spen_secondarybutton == MouseButton.middle)
        {
            temp_spen_secondarybutton = MouseButton.right;
            setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);
            return;
        }

        if (temp_spen_secondarybutton == MouseButton.right)
        {
            temp_spen_secondarybutton = MouseButton.none;
            setMouseButtonInfo(spenButtonSecondaryValue, temp_spen_secondarybutton);
            return;
        }
    }

    private void setMouseButtonInfo(TextView textView, MouseButton button)
    {
        switch (button)
        {
            case left:
            {
                textView.setText(Localization.getString("mouse_button_sleft"));
                break;
            }
            case middle:
            {
                textView.setText(Localization.getString("mouse_button_smiddle"));
                break;
            }
            case right:
            {
                textView.setText(Localization.getString("mouse_button_sright"));
                break;
            }
            case none:
            {
                textView.setText(Localization.getString("common_move"));
                break;
            }
            default:
                break;
        }
    }

    private void minusSensitivity()
    {
        if (temp_sensitivity == 1)
            return;

        temp_sensitivity--;
        sensitivityValue.setText("" + temp_sensitivity);

        higlightMouseSensitivity();
    }

    private void plusSensitivity()
    {
        if (temp_sensitivity == 1000)
            return;

        temp_sensitivity++;
        sensitivityValue.setText("" + temp_sensitivity);
        higlightMouseSensitivity();
    }

    private void higlightMouseSensitivity()
    {
        if (temp_sensitivity == 100) {
            sensitivityValue.setTextColor(Color.GREEN);
        } else {
            sensitivityValue.setTextColor(AppGlobal.textColor1);
        }
    }
}
