package rs.ruffle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NativeActivity;
import android.content.Intent;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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

    protected void startNativeActivity(Uri uri) {
        Intent intent = new Intent(MainActivity.this, NativeActivity.class);
        intent.putExtra("SWF_URI", uri);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /*
        new Thread(() -> {
            try {
                URL url = new URL("https://z0r.de/L/z0r-de_37.swf");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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

                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.i("rfl", "ioerror e " + e);

            }
        }).start();

        */

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            startNativeActivity(getIntent().getData());
        }


        ActivityResultLauncher launcher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> startNativeActivity(uri)
        );

        View button = findViewById(R.id.button);

        button.setOnClickListener((event) -> {
            launcher.launch("application/x-shockwave-flash");
        });

    }
}