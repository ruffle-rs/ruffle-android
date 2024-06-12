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

import magiclib.core.Align;
import magiclib.keyboard.VirtualKeyboardType;

public class VirtualKeyboard extends magiclib.keyboard.VirtualKeyboard {
    private VirtualKeyboardOptions keyboardOptions;

    public VirtualKeyboard(View parentView, int opacity, int backgroundColor, Align align, VirtualKeyboardType landscapeLayout, VirtualKeyboardType portraitLayout) {
        super(parentView, opacity, backgroundColor, align, landscapeLayout, portraitLayout);
        keyboardOptions = null;
    }

    @Override
    public boolean hide() {
        if (isShown && keyboardOptions != null) {
            keyboardOptions.dismiss();
            keyboardOptions = null;
        }

        return super.hide();
    }

    @Override
    public void dispose() {
        if (keyboardOptions != null) {
            keyboardOptions.dismiss();
            keyboardOptions = null;
        }

        super.dispose();
    }

    @Override
    protected void onMenu() {
        keyboardOptions = new VirtualKeyboardOptions(opacity, backgroundColor, align, landscapeLayout, portraitLayout);
        keyboardOptions.setOnKeyboardOptionsEvent(new VirtualKeyboardOptions.onKeyboardOptionsEvent() {
            @Override
            public void onConfirm(int opacityValue, int backgroundColorValue, Align alignment, VirtualKeyboardType landsType, VirtualKeyboardType portrType) {
                update(opacityValue, backgroundColorValue, alignment, landsType, portrType);
            }

            @Override
            public void onBackgroundColorChange(int opacity, int color) {
                setBackgroundColor(opacity, color);
            }

            public void onCancel(int origOpacity, int origColor) {
                setBackgroundColor(origOpacity, origColor);
                keyboardOptions = null;
            }
        });

        keyboardOptions.show();
    }
}
