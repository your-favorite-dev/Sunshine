package com.shc_group.sunshine.app;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.shc_group.sunshine.utils.Utility;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private Context mContext;

    public FetchWeatherTask(Context context) {
        mContext = context;
    }

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
    protected Void doInBackground(String... params) {
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
            new WeatherDataParser(mContext).getWeatherDataFromJson(forecastJSONString, Integer.parseInt(days), location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
