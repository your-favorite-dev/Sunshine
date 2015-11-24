package com.shc_group.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private final static String LOG_TAG_FRAGMENT = ForecastFragment.class.getSimpleName();

    //Used to find view without having to cast Object
    @Bind(R.id.listview_forecast)
    protected ListView forecastListView;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        List<String> weatherList = Arrays.asList("Today - Sunny - 83 / 66", " Tomorrow - Foggy - 70 / 45",
                "Weds - Cloudy - 72 / 63", "Thurs - Rainy - 64 / 51", "Fri - Foggy - 70 / 46",
                "Sat - Sunny - 76 / 60");

        ArrayAdapter<String> weatherAdapter = new ArrayAdapter<>(getContext(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weatherList);

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //Initializes the ListView through ButterKnife
        ButterKnife.bind(this, view);
        forecastListView.setAdapter(weatherAdapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            new FetchWeatherTask().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String loadApiKey() throws FileNotFoundException {
        String apikey = null;
        InputStream is = null;
        try {
            is = getContext().getAssets().open("weather_api.key");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            while (reader.ready()) {
                apikey = reader.readLine();
            }
        } catch (IOException e) {
            Log.i(LOG_TAG_FRAGMENT, "The API file is empty");
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

    public class FetchWeatherTask extends AsyncTask<String, Void, String> {
        private final String weatherURI = "api.openweathermap.org";
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        private String getWeatherJSON(String weatherURI, String zipCode) {
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
                        .appendQueryParameter("q", zipCode)
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("units", "metic")
                        .appendQueryParameter("cnt", "7")
                        .appendQueryParameter("appid", loadApiKey());
                Uri uri = builder.build();
                Log.v(LOG_TAG, uri.toString());
                URL url = new URL(uri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
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
                    // buffer for debugging.
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
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
            Log.v(LOG_TAG, forecastJsonStr + " ");
            return forecastJsonStr;
        }

        @Override
        protected String doInBackground(String... params) {
            return getWeatherJSON(weatherURI, "10018");
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

}
