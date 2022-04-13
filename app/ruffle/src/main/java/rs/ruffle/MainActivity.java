package rs.ruffle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    static {
        // load the native activity
        System.loadLibrary("ruffle_android");
    }

    protected void startFromContentUri(Uri uri) {

        ContentResolver resolver = getContentResolver();
        try {
            InputStream inputStream = resolver.openInputStream(uri);

            int available = inputStream.available();
            byte[] bytes = new byte[available];
            // assuming the whole contents will be available at once
            int _num_bytes_read = inputStream.read(bytes);

            FullscreenNativeActivity.SWF_BYTES = bytes;
        }
        catch (IOException e) {

        }

        Intent intent = new Intent(MainActivity.this, FullscreenNativeActivity.class);
        startActivity(intent);
    }

    void startFromHttpUrl(String url) {
        new Thread(() -> {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.connect();

                int length = urlConnection.getContentLength();
                Log.i("rfl", "content length: " + length);
                byte[] bytes = new byte[length];
                int offs = 0;
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    while (offs < length) {

                        int read = in.read(bytes, offs, length-offs);
                        offs += read;
                        if (read > 0)
                            Log.i("rfl", "read " + read + " bytes");
                    }
                    Log.i("rfl", "read done: " + offs);



                    FullscreenNativeActivity.SWF_BYTES = bytes;


                    Intent intent = new Intent(MainActivity.this, FullscreenNativeActivity.class);

                    startActivity(intent);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.i("rfl", "ioerror e " + e);

            }
        }).start();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNoStatusBarFeature();
        setContentView(R.layout.activity_main);
        hideActionBar();


        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            if ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                startFromHttpUrl(uri.toString());
            else
                startFromContentUri(uri);
        }


        ActivityResultLauncher launcher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> startFromContentUri(uri)
        );

        View button = findViewById(R.id.button);

        button.setOnClickListener((event) -> {
            launcher.launch("application/x-shockwave-flash");
        });


        View button3 = findViewById(R.id.button3);
        button3.setOnClickListener((event) -> {
            EditText swfUrl = findViewById(R.id.editTextSwfUrl);
            startFromHttpUrl(swfUrl.getText().toString());
        });
    }

    private void requestNoStatusBarFeature() {
        //Hiding the status bar this way makes it see through when pulled down
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }
}