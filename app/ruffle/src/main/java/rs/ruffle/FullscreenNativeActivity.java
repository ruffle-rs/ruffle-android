package rs.ruffle;

import android.app.NativeActivity;
import android.os.Bundle;
import android.view.KeyEvent;

public class FullscreenNativeActivity extends NativeActivity implements KeyEvent.Callback {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SwfOverlay dialog = new SwfOverlay(this);
        dialog.setContentView(R.layout.overlay_layout);
        dialog.setCancelable(false);
        dialog.show();
    }
}
