package com.shc_group.sunshine;

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
    //Used to find view without having to cast Object
    @Bind(R.id.listview_forecast)
    protected ListView forecastListView;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        List<String> weatherList = Arrays.asList("Today - Sunny - 83 / 66", " Tomorrow - Foggy - 70 / 45",
                "Weds - Cloudy - 72 / 63", "Thurs - Rainy - 64 / 51", "Fri - Foggy - 70 / 46",
                "Sat - Sunny - 76 / 60");

        ArrayAdapter<String> weatherAdapter = new ArrayAdapter<String>(getContext(),
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
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.refresh){
            new FetchWeatherTask().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        private final String weatherURI = "http://api.openweathermap.org/data/2.5/forecast/daily?q=10018,us&mode=json&units=metric&cnt=7&appid=ccfa053751de07b5d5d6c2cc21f9c83d";
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        private String getWeatherJSON(String weatherURI) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;

            try {

                URL url = new URL(weatherURI);

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
                }else {
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
            Log.v(LOG_TAG, forecastJsonStr);
            return forecastJsonStr;
        }

        @Override
        protected String doInBackground(Void... params) {
            return getWeatherJSON(weatherURI);
        }
    }

}
