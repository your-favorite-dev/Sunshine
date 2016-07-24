package com.shc_group.sunshine.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.shc_group.sunshine.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

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

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static String getDefaultDays() {
        return "14";
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatTemperature(double temperature, boolean isMetric) {
        double temp;
        if (!isMetric) {
            temp = 9 * temperature / 5 + 32;
        } else {
            temp = temperature;
        }
        return String.format(Locale.US, "%.0f", temp);
    }

    public static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }
}
