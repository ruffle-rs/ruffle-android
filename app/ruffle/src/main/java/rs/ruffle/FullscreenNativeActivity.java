package rs.ruffle;


import com.google.androidgamesdk.GameActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;


import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.List;

public class FullscreenNativeActivity extends GameActivity {

    static {
        // load the native activity
        System.loadLibrary("ruffle_android");
    }
    protected static byte[] SWF_BYTES;

    protected byte[] getSwfBytes() {
        return SWF_BYTES;
    }

    protected void navigateToUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    int[] loc = new int[2];


    /** @noinspection FieldMayBeFinal, unused */
    // Handle of an EventLoopProxy over in rust-land
    private long eventLoopHandle = 0;

    protected int[] getLocOnScreen() {
        mSurfaceView.getLocationOnScreen(loc);
        return loc;
    }

    protected int getSurfaceWidth() {
        return mSurfaceView.getWidth();
    }

    protected int getSurfaceHeight() {
        return mSurfaceView.getHeight();
    }

    private native void keydown(byte key_code, char key_char);
    private native void keyup(byte key_code, char key_char);

    private native void requestContextMenu();
    private native void runContextMenuCallback(int index);
    private native void clearContextMenu();

    private void showContextMenu(String[] items) {
        runOnUiThread(() -> {
            PopupMenu popup = new PopupMenu(this, findViewById(R.id.button_cm));
            Menu menu = popup.getMenu();
            menu.setGroupDividerEnabled(true);
            int group = 1;
            for (int i = 0; i < items.length; ++i) {
                String[] elements = items[i].split(" ", 4);
                boolean enabled = Boolean.parseBoolean(elements[0]);
                boolean separatorBefore = Boolean.parseBoolean(elements[1]);
                boolean checked = Boolean.parseBoolean(elements[2]);
                String caption = elements[3];

                if (separatorBefore)
                    group += 1;

                MenuItem item = menu.add(group, i, Menu.NONE, caption);
                item.setEnabled(enabled);
                if (checked) {
                    item.setCheckable(true);
                    item.setChecked(true);
                }

            }
            popup.setOnMenuItemClickListener((item) -> {
                runContextMenuCallback(item.getItemId());
                return true;
            });
            popup.setOnDismissListener((pm) -> {
                clearContextMenu();
            });
            popup.show();
        });
    }

    private static <T> List<T> gatherAllDescendantsOfType(View v, Class t) {
        List<T> result = new ArrayList<T>();
        if (t.isInstance(v))
            result.add((T)v);
        if (v instanceof  ViewGroup) {
            ViewGroup vg = (ViewGroup)v;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                result.addAll(gatherAllDescendantsOfType(vg.getChildAt(i), t));
            }
        }
        return result;
    }

    @Override
    protected void onCreateSurfaceView() {
        LayoutInflater inflater = getLayoutInflater();
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.keyboard, null);
        this.contentViewId = ViewCompat.generateViewId();
        layout.setId(this.contentViewId);
        setContentView(layout);

        this.mSurfaceView = new InputEnabledSurfaceView(this);

        View placeholder = findViewById(R.id.placeholder);

        ConstraintLayout.LayoutParams pars = (ConstraintLayout.LayoutParams)placeholder.getLayoutParams();

        ViewGroup parent = (ViewGroup)placeholder.getParent();
        int index = parent.indexOfChild(placeholder);
        parent.removeView(placeholder);
        parent.addView(this.mSurfaceView, index);
        this.mSurfaceView.setLayoutParams(pars);


        List<Button> keys = gatherAllDescendantsOfType(layout.getViewById(R.id.keyboard), Button.class);

        for (Button b : keys) {
            b.setOnTouchListener((View view, MotionEvent motionEvent) -> {
                String tag = (String)view.getTag();
                if (tag != null) {
                    String[] spl = tag.split(" ", 2);
                    byte by = Byte.parseByte(spl[0]);
                    char c = spl.length > 1 ? spl[1].charAt(0) : 0;

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                        keydown(by, c);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                        keyup(by, c);
                }
                return false;
            });
        }

        layout.findViewById(R.id.button_kb) .setOnClickListener((view) -> {
            View keyboard = layout.getViewById(R.id.keyboard);
            if (keyboard.getVisibility() == View.VISIBLE) {
                keyboard.setVisibility(View.GONE);
            } else {
                keyboard.setVisibility(View.VISIBLE);
            }
        });

        layout.findViewById(R.id.button_cm).setOnClickListener((view) -> {
            requestContextMenu();
        });

        layout.requestLayout();
        layout.requestFocus();
        this.mSurfaceView.getHolder().addCallback(this);
        ViewCompat.setOnApplyWindowInsetsListener(this.mSurfaceView, this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        View keyboard = findViewById(R.id.keyboard);
        boolean isLandscape = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        keyboard.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
    }

    private void hideSystemUI() {
        // This will put the game behind any cutouts and waterfalls on devices which have
        // them, so the corresponding insets will be non-zero.
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode
                    = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        // From API 30 onwards, this is the recommended way to hide the system UI, rather than
        // using View.setSystemUiVisibility.
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(),
                decorView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.hide(WindowInsetsCompat.Type.displayCutout());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // When true, the app will fit inside any system UI windows.
        // When false, we render behind any system UI windows.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemUI();
        // You can set IME fields here or in native code using GameActivity_setImeEditorInfoFields.
        // We set the fields in native_engine.cpp.
        // super.setImeEditorInfoFields(InputType.TYPE_CLASS_TEXT,
        //     IME_ACTION_NONE, IME_FLAG_NO_FULLSCREEN );
        requestNoStatusBarFeature();
        super.onCreate(savedInstanceState);
    }

    public boolean isGooglePlayGames() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature("com.google.android.play.feature.HPE_EXPERIENCE");
    }

    private void requestNoStatusBarFeature() {
        //Hiding the status bar this way makes it see through when pulled down
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
