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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import magiclib.Global;
import magiclib.controls.Dialog;
import magiclib.core.Align;
import magiclib.core.ColorPicker;
import magiclib.core.ColorPickerItem;
import magiclib.keyboard.VirtualKeyboardType;
import magiclib.locales.Localization;

class VirtualKeyboardOptions extends Dialog {
    public abstract interface onKeyboardOptionsEvent
    {
        public abstract void onConfirm(int opacity, int backgroundColor, Align align, VirtualKeyboardType landscapeLayout, VirtualKeyboardType portraitLayout);
        public abstract void onBackgroundColorChange(int opacity, int color);
        public abstract void onCancel(int origOpacity, int origColor);
    }

    private TextView opacityTextValue;
    private int opacityValue;
    private int backgroundColor;
    private int origOpacityValue;
    private int origBackgroundColor;
    private SeekBar seekBar;
    private onKeyboardOptionsEvent event;
    private View.OnClickListener onClick;
    private boolean confirmed = false;
    private Button backgroundColorPicker;
    private TextView alignTextValue;
    private Align alignValue;
    private VirtualKeyboardType landscapeLayout;
    private VirtualKeyboardType portraitLayout;
    private TextView landscapeKeyboard;
    private TextView portraitKeyboard;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.keyboard_options_keyboard_caption, "common_keyboard");
        localize(R.id.keyboard_options_landscape_caption, "common_landscape");
        localize(R.id.keyboard_options_portrait_caption, "common_portrait");
        localize(R.id.keyboard_options_lookandfeel_caption, "common_lookandfeel");
        localize(R.id.keyboard_options_align_caption, "common_alignment");
        localize(R.id.keyboard_options_opacity_title, "common_opacity");
        localize(R.id.keyboard_options_color_caption, "common_bckcolor");
    }

    public VirtualKeyboardOptions(int transpValue, int backgrColor, Align align, VirtualKeyboardType landscapeLayout, VirtualKeyboardType portraitLayout) {
        super(Global.context);

        this.origOpacityValue = this.opacityValue = transpValue;
        this.origBackgroundColor = this.backgroundColor = backgrColor;
        this.alignValue = align;

        setContentView(R.layout.pckeyboard_options);
        setCaption("common_settings");
        ImageButton b = (ImageButton)findViewById(R.id.keyboard_options_opacity_minus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_opacity_plus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_align_minus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_align_plus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_landscape_minus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_landscape_plus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_portrait_minus);
        b.setOnClickListener(getOnClickEvent());

        b = (ImageButton)findViewById(R.id.keyboard_options_portrait_plus);
        b.setOnClickListener(getOnClickEvent());

        this.landscapeLayout = landscapeLayout;
        this.portraitLayout = portraitLayout;

        landscapeKeyboard = (TextView)findViewById(R.id.keyboard_options_landscape_value);
        portraitKeyboard = (TextView)findViewById(R.id.keyboard_options_portrait_value);

        setKeyboardLayoutTitle(true);
        setKeyboardLayoutTitle(false);

        alignTextValue = (TextView)findViewById(R.id.keyboard_options_align_value);
        setAlignValueTitle();

        opacityTextValue = (TextView)findViewById(R.id.keyboard_options_opacity_value);
        opacityTextValue.setText("" + opacityValue);

        seekBar = (SeekBar) findViewById(R.id.keyboard_options_opacityseek);
        seekBar.setMax(255);
        seekBar.setProgress(opacityValue);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityValue = progress;
                opacityTextValue.setText("" + opacityValue);
                event.onBackgroundColorChange(opacityValue, backgroundColor);

            }
        });

        backgroundColorPicker = (Button)findViewById(R.id.keyboard_options_color);
        backgroundColorPicker.setOnClickListener(getOnClickEvent());
        backgroundColorPicker.setBackgroundColor(backgroundColor);

        ImageButton confirm = (ImageButton)findViewById(R.id.keyboard_options_confirm);
        confirm.setOnClickListener(getOnClickEvent());
    }

    private void setAlignValueTitle() {
        if (alignValue == Align.top) {
            alignTextValue.setText(Localization.getString("align_top"));
        } else if (alignValue == Align.middle) {
            alignTextValue.setText(Localization.getString("align_middle"));
        } if (alignValue == Align.bottom) {
            alignTextValue.setText(Localization.getString("align_bottom"));
        }
    }

    private View.OnClickListener getOnClickEvent() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.keyboard_options_opacity_minus) {
                        minusOpacity();
                    } else if (v.getId() == R.id.keyboard_options_opacity_plus) {
                        plusOpacity();
                    } else if (v.getId() == R.id.keyboard_options_confirm) {
                        if (event!=null) {
                            event.onConfirm(opacityValue, backgroundColor, alignValue, landscapeLayout, portraitLayout);
                            confirmed = true;
                        }
                        dismiss();
                    } else if (v.getId() == R.id.keyboard_options_color) {
                        ColorPicker dlg = new ColorPicker(getContext(), backgroundColor);
                        dlg.setCaption("common_bckcolor");
                        dlg.setOnColorPickListener(new ColorPicker.ColorPickListener()
                        {
                            @Override
                            public void onPick(ColorPickerItem selected)
                            {
                                backgroundColor = selected.getColor();
                                backgroundColorPicker.setBackgroundColor(backgroundColor);
                                event.onBackgroundColorChange(opacityValue, backgroundColor);
                            }
                        });

                        dlg.show();
                    } else if (v.getId() == R.id.keyboard_options_align_minus) {
                        minusAlign();
                    } else if (v.getId() == R.id.keyboard_options_align_plus) {
                        plusAlign();
                    } else if (v.getId() == R.id.keyboard_options_landscape_minus) {
                        minusKeyboardLayout(true);
                    } else if (v.getId() == R.id.keyboard_options_landscape_plus) {
                        plusKeyboardLayout(true);
                    } else if (v.getId() == R.id.keyboard_options_portrait_minus) {
                        minusKeyboardLayout(false);
                    } else if (v.getId() == R.id.keyboard_options_portrait_plus) {
                        plusKeyboardLayout(false);
                    }
                }
            };
        }

        return onClick;
    }

    private void minusKeyboardLayout(boolean landscape) {
        VirtualKeyboardType type = (landscape)?landscapeLayout:portraitLayout;

        if (type == VirtualKeyboardType.full) {
            return;
        }

        if (type == VirtualKeyboardType.simple_ru) {
            type = VirtualKeyboardType.full_ru;
        } else if (type == VirtualKeyboardType.full_ru) {
            type = VirtualKeyboardType.simple;
        } else {
            type = VirtualKeyboardType.full;
        }

        if (landscape) {
            landscapeLayout = type;
        } else {
            portraitLayout = type;
        }

        setKeyboardLayoutTitle(landscape);
    }

    private void plusKeyboardLayout(boolean landscape) {
        VirtualKeyboardType type = (landscape)?landscapeLayout:portraitLayout;

        if (type == VirtualKeyboardType.simple_ru) {
            return;
        }

        if (type == VirtualKeyboardType.full) {
            type = VirtualKeyboardType.simple;
        } else if (type == VirtualKeyboardType.simple) {
            type = VirtualKeyboardType.full_ru;
        } else if (type == VirtualKeyboardType.full_ru) {
            type = VirtualKeyboardType.simple_ru;
        }

        if (landscape) {
            landscapeLayout = type;
        } else {
            portraitLayout = type;
        }

        setKeyboardLayoutTitle(landscape);
    }

    private void setKeyboardLayoutTitle(boolean landscape) {
        TextView title = landscape?landscapeKeyboard:portraitKeyboard;
        VirtualKeyboardType type = (landscape)?landscapeLayout:portraitLayout;

        if (type == VirtualKeyboardType.full) {
            title.setText(Localization.getString("keyboard_type_full"));
        } else if (type == VirtualKeyboardType.simple) {
            title.setText(Localization.getString("keyboard_type_simple"));
        } else if (type == VirtualKeyboardType.full_ru) {
             title.setText(Localization.getString("keyboard_type_full_ru"));
        } else if (type == VirtualKeyboardType.simple_ru) {
            title.setText(Localization.getString("keyboard_type_simple_ru"));
        }
    }

    private void minusAlign() {
        if (alignValue == Align.top)
            return;

        if (alignValue == Align.bottom) {
            alignValue = Align.middle;
        } else {
            alignValue = Align.top;
        }

        setAlignValueTitle();
    }

    private void plusAlign() {
        if (alignValue == Align.bottom)
            return;

        if (alignValue == Align.top) {
            alignValue = Align.middle;
        } else {
            alignValue = Align.bottom;
        }

        setAlignValueTitle();
    }

    @Override
    protected void onStop()
    {
        if (event!=null && !confirmed) {
            event.onCancel(origOpacityValue, origBackgroundColor);
        }

        super.onStop();
    }

    private void minusOpacity() {
        if (opacityValue <= 0) {
            return;
        }

        opacityValue--;
        seekBar.setProgress(opacityValue);
        opacityTextValue.setText("" + opacityValue);
        event.onBackgroundColorChange(opacityValue, backgroundColor);
    }

    private void plusOpacity() {
        if (opacityValue >= 255) {
            return;
        }

        opacityValue++;
        seekBar.setProgress(opacityValue);
        opacityTextValue.setText("" + opacityValue);
        event.onBackgroundColorChange(opacityValue, backgroundColor);
    }

    public void setOnKeyboardOptionsEvent(onKeyboardOptionsEvent event){
        this.event = event;
    }
}
