package rs.ruffle;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class SwfOverlay extends Dialog {
    private static native void touchdown(double posXPattern, double posYPattern);
    private static native void touchmove(double posXPattern, double posYPattern);
    private static native void touchup(double posXPattern, double posYPattern);
    private static native void keydown(byte key_code, char key_char);
    private static native void keyup(byte key_code, char key_char);
    private static native void loadswf(byte[] bytes);

    private boolean isMenuOpen;
    private int clearBG = Color.parseColor("#00000000");
    private int menuBG = Color.parseColor("#FE007FFF");
    public View overlayMenu;
    public View inGameMenu;

    public FullscreenNativeActivity currentSWFPlayer;

    public SwfOverlay(@NonNull Context context) {
        super(context, android.R.style.ThemeOverlay);
        setContentView(R.layout.overlay_layout);
//        hideSystemUI();

        overlayMenu = findViewById(R.id.overlay_menu);
        inGameMenu = findViewById(R.id.in_game_menu);

        setMenuVisible(false);
        findViewById(R.id.open_menu_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMenuVisible(true);
            }
        });
    }

    public void loadSWFFromBytes(byte[] bytes) {
        loadswf(bytes);
    }

    public void setMenuVisible(boolean onOff) {
        int bgColor;
        if (onOff)
            bgColor = menuBG;
        else
            bgColor = clearBG;
        setBackgroundColor(bgColor);

        overlayMenu.setVisibility(onOff ? View.VISIBLE : View.GONE);
        inGameMenu.setVisibility(onOff ? View.GONE : View.VISIBLE);
        isMenuOpen = onOff;
    }
    public void setBackgroundColor(int bgColor) {
        final Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundDrawable(new ColorDrawable(bgColor));
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
//        Log.d("Touch", ev.toString());
        if (!isMenuOpen) {
            double posX = ev.getRawX();
            double posY = ev.getRawY();
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
                touchdown(posX, posY);
            if (ev.getAction() == MotionEvent.ACTION_MOVE)
                touchmove(posX, posY);
            if (ev.getAction() == MotionEvent.ACTION_UP)
                touchup(posX, posY);
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent key_event) {
        Log.d("DialogDispatchKey", key_event.toString());
        if (!isMenuOpen) {
            if (key_event.getAction() == KeyEvent.ACTION_DOWN) {
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    keydown((byte) 87, 'W');
                    return true;
                }
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    keydown((byte) 83, 'S');
                    return true;
                }
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    keydown((byte) 65, 'A');
                    return true;
                }
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    keydown((byte) 68, 'D');
                    return true;
                }
            }
            if (key_event.getAction() == KeyEvent.ACTION_UP) {
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    keyup((byte) 87, 'W');
                    return true;
                }
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    keyup((byte) 83, 'S');
                    return true;
                }
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    keyup((byte) 65, 'A');
                    return true;
                }
                if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    keyup((byte) 68, 'D');
                    return true;
                }
            }
        }
        return false;
    }

    public void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
