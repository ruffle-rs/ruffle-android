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
import android.widget.RadioGroup;

import magiclib.controls.Dialog;
import magiclib.layout.widgets.SpecialAction.Action;
import magiclib.layout.widgets.SpecialComboAction;

abstract interface ComboSpecialActionEventListener
{
    public abstract void onPick(Action action);
}

class ComboSpecialActionDialog extends Dialog
{
    private ComboSpecialActionEventListener event;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(magiclib.R.id.specialcombo_radio_showkeyboard, "common_show_keyboard");
        localize(magiclib.R.id.specialcombo_radio_showbuiltinkeyboard, "widget_edit_special_action_dbxkeyboard");
        localize(magiclib.R.id.specialcombo_radio_mousecalibrate, "widget_edit_special_action_mousereset");
        localize(magiclib.R.id.specialcombo_radio_hideallbuttons, "widget_edit_special_action_hidebuttons");
    }

    public ComboSpecialActionDialog(Context context, SpecialComboAction action)
    {
        super(context);

        setContentView(magiclib.R.layout.special_combo_dialog);
        setCaption("widget_edit_combo_menu_special");

        final RadioGroup rg = (RadioGroup) findViewById(magiclib.R.id.specialcombo_radio);

        switch (action.getAction())
        {
            case show_keyboard:
            {
                rg.check(magiclib.R.id.specialcombo_radio_showkeyboard);
                break;
            }
            case show_built_in_keyboard:
            {
                rg.check(magiclib.R.id.specialcombo_radio_showbuiltinkeyboard);
                break;
            }
            case reset_mouse_position:
            {
                rg.check(magiclib.R.id.specialcombo_radio_mousecalibrate);
                break;
            }
            case hide_buttons:
            {
                rg.check(magiclib.R.id.specialcombo_radio_hideallbuttons);
                break;
            }
        }

        ImageButton confirm = (ImageButton)findViewById(magiclib.R.id.specialcombo_confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (event!=null)
                {
                    Action a;

                    int id = rg.getCheckedRadioButtonId();

                    if (id == magiclib.R.id.specialcombo_radio_showkeyboard) {
                        event.onPick(Action.show_keyboard);
                    } else if (id == magiclib.R.id.specialcombo_radio_showbuiltinkeyboard) {
                        event.onPick(Action.show_built_in_keyboard);
                    } else if (id == magiclib.R.id.specialcombo_radio_mousecalibrate) {
                        event.onPick(Action.reset_mouse_position);
                    } else if (id == magiclib.R.id.specialcombo_radio_hideallbuttons) {
                        event.onPick(Action.hide_buttons);
                    }
                }

                dismiss();
            }
        });
    }

    public void setOnComboSpecialActionEventListener(ComboSpecialActionEventListener event)
    {
        this.event = event;
    }
}