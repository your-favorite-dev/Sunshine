package com.shc_group.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.shc_group.sunshine.R;
import com.shc_group.sunshine.utils.Utility;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private Context mContext;
    private ArrayAdapter<String> weatherAdapter;
    private boolean DEBUG = true;

    public FetchWeatherTask(Context context, ArrayAdapter<String> arrayAdapter) {
        mContext = context;
        weatherAdapter = arrayAdapter;
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d", Locale.US);
        return format.format(date);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // Data is fetched in Celsius by default.
        // If user prefers to see in Fahrenheit, convert the values here.
        // We do this rather than fetching in Fahrenheit so that the user can
        // change this option without us having to re-fetch the data once
        // we start storing the values in a database.
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_metric));

        if (unitType.equals(mContext.getString(R.string.pref_units_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(mContext.getString(R.string.pref_units_metric))) {
            Log.d(LOG_TAG, "Unit type not found: " + unitType);
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);


        return roundedHigh + "/" + roundedLow;
    }



    /*
        Students: This code will allow the FetchWeatherTask to continue to return the strings that
        the UX expects so that we can continue to test the application even once we begin using
        the database.
     */


    private String getWeatherJSON(String weatherURI, String zipCode, String days) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        Uri.Builder builder = new Uri.Builder();
        String forecastJsonStr = null;

        try {

            builder.scheme("http")
                    .authority(weatherURI)
                    .appendEncodedPath("data")
                    .appendEncodedPath("2.5")
                    .appendEncodedPath("forecast")
                    .appendEncodedPath("daily")
                    .appendQueryParameter("zip", zipCode)
                    .appendQueryParameter("mode", "json")
                    .appendQueryParameter("units", "metric")
                    .appendQueryParameter("cnt", days)
                    .appendQueryParameter("appid", Utility.loadApiKey(mContext));
            Uri uri = builder.build();
            Log.v(LOG_TAG, uri.toString());
            URL url = new URL(uri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder sBuilder = new StringBuilder();
            if (inputStream == null) {
                // Nothing to do.
                forecastJsonStr = null;
            } else {
                reader = new BufferedReader(new InputStreamReader(inputStream));
            }
            String line;
            while (reader != null && (line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // sBuilder for debugging.
                sBuilder.append(line).append("\n");
            }

            if (sBuilder.length() == 0) {
                // Stream was empty.  No point in parsing.
                forecastJsonStr = null;
            }
            forecastJsonStr = sBuilder.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            forecastJsonStr = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return forecastJsonStr;
    }

    @Override
    protected String[] doInBackground(String... params) {
        if (params == null || params.length < 2) {
            return null;
        }
        if (params[1] != null && (Integer.valueOf(params[1]) == null)) {
            Log.e(LOG_TAG, params[0] + " " + params[1]);
            return null;
        }
        final String location = params[0];
        final String days = params[1];
        final String weatherURI = "api.openweathermap.org";
        final String forecastJSONString = getWeatherJSON(weatherURI, params[0], params[1]);
        try {
            return new WeatherDataParser(mContext).getWeatherDataFromJson(forecastJSONString, Integer.parseInt(days), location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String[] s) {
        super.onPostExecute(s);
        if (s != null && weatherAdapter != null) {
            weatherAdapter.clear();
            weatherAdapter.addAll(Arrays.asList(s));
        } else {
            Toast.makeText(mContext, "There was a problem updating the weather", Toast.LENGTH_SHORT).show();
        }
    }
}
