package rs.ruffle;

import android.app.NativeActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FullscreenNativeActivity extends NativeActivity implements KeyEvent.Callback {
    private SwfOverlay overlay;
    public static final int REQUEST_SWF_CODE = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overlay = new SwfOverlay(this);
        overlay.currentSWFPlayer = this;
        overlay.setCancelable(false);
        setupOverlayMenu();
        overlay.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlay.hideSystemUI();

        //Trying to load swf in oncreate or onresume panics rust due
        //to the flash player not yet being loaded/ready
//        Intent intent = getIntent();
//        Bundle extras = intent.getExtras();
//        Uri uri = (Uri)extras.get("SWF_URI");
//        sendSWFFromUri(uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = (Uri)data.getExtras().get("SWF_URI");
        Log.d("NativeActivity", "Received uri " + uri + " from code " + requestCode);
//        overlay.LoadSWF();
        sendSWFFromUri(uri);
    }

    public void sendSWFFromUri(Uri uri) {
        ContentResolver resolver = getContentResolver();
        try {
            InputStream inputStream = resolver.openInputStream(uri);

            int available = inputStream.available();
            byte[] bytes = new byte[available];
            // assuming the whole contents will be available at once
            int _num_bytes_read = inputStream.read(bytes);
            overlay.loadSWFFromBytes(bytes);
        } catch (IOException ex) {
            Log.e("FullscreenNativeActivity", ex.getMessage());
        }
    }
    public void sendSWFFromAsset(String asset) {
        try {
            InputStream inputStream = getAssets().open(asset);

            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            byte[] bytes = output.toByteArray();
            overlay.loadSWFFromBytes(bytes);
        } catch(IOException ex) {
            Log.e("FullscreenNativeActivity", ex.getMessage());
        }
    }

    private void setupOverlayMenu() {
        ListView menu = overlay.overlayMenu.findViewById(R.id.overlay_list_view);
        ArrayList<String> items = new ArrayList<>();
        items.add("Resume");
        items.add("Read Extra");
        items.add("Bowman2");
        items.add("Motherload");
        items.add("Tetris");
        items.add("2048");
        items.add("BOI");
        items.add("Dud");
        items.add("Dud");
        items.add("Dud");
        items.add("Dud");
        items.add("Dud");
        items.add("Dud");
        items.add("Dud");
        items.add("Dud");
        OverlayMenuAdapter menuAdapter = new OverlayMenuAdapter(this, items);
        menu.setAdapter(menuAdapter);

        menuAdapter.AddListener(new OverlayMenuListener() {
            @Override
            public void onButtonPress(int position) {
//                Log.d("NativeActivity", "Overlay button " + position + " pressed");
                if (position == 0)
                    overlay.setMenuVisible(false);
                if (position == 1) {
//                    Intent intent = new Intent(FullscreenNativeActivity.this, MainActivity.class);
//                    startActivityForResult(intent, REQUEST_SWF_CODE);
                    Intent intent = getIntent();
                    Bundle extras = intent.getExtras();
                    Uri uri = (Uri)extras.get("SWF_URI");
                    sendSWFFromUri(uri);
                }
                if (position == 2) {
                    sendSWFFromAsset("Bow Man 2.swf");
                }
                if (position == 3) {
                    sendSWFFromAsset("Motherload.swf");
                }
                if (position == 4) {
                    sendSWFFromAsset("Flash Tetris.swf");
                }
                if (position == 5) {
                    sendSWFFromAsset("2048.swf");
                }
                if (position == 6) {
                    sendSWFFromAsset("tboiwotl-v1.666.swf");
                }
            }
        });
    }

    public void quit() {
        overlay.dismiss();
        finish();
    }
}
