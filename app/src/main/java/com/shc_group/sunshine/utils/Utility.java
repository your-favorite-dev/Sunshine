package com.shc_group.sunshine.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utility {
    private static String LOG_TAG = Utility.class.getSimpleName();

    public static String loadApiKey(Context context) throws FileNotFoundException {
        String apiKeyFile = "weather_api.key";
        String apikey = null;
        InputStream is = null;
        try {
            is = context.getAssets().open(apiKeyFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            while (reader.ready()) {
                apikey = reader.readLine();
            }
        } catch (IOException e) {
            Log.i(LOG_TAG, "The API file is empty");
            Toast.makeText(context, "The API Key file is empty", Toast.LENGTH_SHORT).show();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return apikey;
    }
}
