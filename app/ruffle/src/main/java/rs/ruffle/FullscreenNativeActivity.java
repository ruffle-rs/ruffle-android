package rs.ruffle;

import android.app.NativeActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class FullscreenNativeActivity extends NativeActivity {

    protected static byte[] SWF_BYTES;

    protected byte[] getSwfBytes() {
        return SWF_BYTES;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestNoStatusBarFeature();
        super.onCreate(savedInstanceState);
    }
    private void requestNoStatusBarFeature() {
        //Hiding the status bar this way makes it see through when pulled down
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
