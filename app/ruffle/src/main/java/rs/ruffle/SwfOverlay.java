package rs.ruffle;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class SwfOverlay extends Dialog {
    private static native void touchdown(double posXPattern, double posYPattern);
    private static native void touchmove(double posXPattern, double posYPattern);
    private static native void touchup(double posXPattern, double posYPattern);
    private static native void keydown(byte key_code, char key_char);
    private static native void keyup(byte key_code, char key_char);

    public SwfOverlay(@NonNull Context context) {
        super(context);
        hideSystemUI();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
//        Log.d("Touch", ev.toString());
        double posX = ev.getRawX();
        double posY = ev.getRawY();
        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            touchdown(posX, posY);
        if (ev.getAction() == MotionEvent.ACTION_MOVE)
            touchmove(posX, posY);
        if (ev.getAction() == MotionEvent.ACTION_UP)
            touchup(posX, posY);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent key_event) {
        Log.d("DialogDispatchKey", key_event.toString());
        if (key_event.getAction() == KeyEvent.ACTION_DOWN) {
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                keydown((byte)87, 'W');
                return true;
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                keydown((byte)83, 'S');
                return true;
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                keydown((byte)65, 'A');
                return true;
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                keydown((byte)68, 'D');
                return true;
            }
        }
        if (key_event.getAction() == KeyEvent.ACTION_UP) {
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                keyup((byte)87, 'W');
                return true;
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                keyup((byte)83, 'S');
                return true;
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                keyup((byte)65, 'A');
                return true;
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                keyup((byte)68, 'D');
                return true;
            }
        }
        return false;
    }

    private void hideSystemUI() {
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
