package com.shc_group.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.shc_group.sunshine.R;
import com.shc_group.sunshine.exceptions.ConnectionException;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private final static String LOG_TAG_FRAGMENT = ForecastFragment.class.getSimpleName();
    private ArrayAdapter<String> weatherAdapter;
    //Used to find view without having to cast Object
    @Bind(R.id.listview_forecast)
    protected ListView forecastListView;

    public ForecastFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(LOG_TAG_FRAGMENT, "onStart called");

        try {
            updateWeather();
        } catch (ConnectionException e) {
            e.printStackTrace();
            Toast.makeText(getContext(),"There was a problem with the connection", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG_FRAGMENT,"onCreate called");
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(LOG_TAG_FRAGMENT, "onPause called");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG_FRAGMENT,"onResume called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(LOG_TAG_FRAGMENT,"onStop called");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        weatherAdapter = new ArrayAdapter<>(getContext(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //Initializes the ListView through ButterKnife
        ButterKnife.bind(this, view);
        forecastListView.setAdapter(weatherAdapter);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (forecastListView.getItemAtPosition(position) instanceof String) {
                    String details = (String) forecastListView.getItemAtPosition(position);
                    Intent intent = new Intent(getContext(), DetailActivity.class);
                    intent.putExtra("details", details);
                    startActivity(intent);
                }
            }
        });

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
        Log.wtf(ForecastFragment.class.getSimpleName(), "Menu Select");
        if (id == R.id.refresh) {
            try {
                updateWeather();
            } catch (ConnectionException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "There is an issue with the connection", Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private String loadApiKey() throws FileNotFoundException {
        String apiKeyFile = "weather_api.key";
        String apikey = null;
        InputStream is = null;
        try {
            is = getContext().getAssets().open(apiKeyFile);
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
    private boolean isOnline(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    }

    private void updateWeather() throws ConnectionException{
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String zipCode = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        String days = "7";
        if(isOnline(getContext())) {
            new FetchWeatherTask().execute(zipCode, days);
        }else{
            throw new ConnectionException("There is an issue with the connection");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG_FRAGMENT, "onDestroy Called");
        ButterKnife.unbind(this);
    }

    protected class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String weatherURI = "api.openweathermap.org";
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


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
            return forecastJsonStr;
        }

        @Override
        protected String[] doInBackground(String... params) {
            if (params == null) {
                return null;
            }
            if (params.length < 2) {
                return null;
            }
            if (params[1] != null && (Integer.valueOf(params[1]) == null)) {
                Log.e(LOG_TAG, params[0] + " " + params[1]);
                return null;
            }
            String forecastJSONString = getWeatherJSON(weatherURI, params[0], params[1]);
            try {
                return new WeatherDataParser(getContext()).getWeatherDataFromJson(forecastJSONString, Integer.parseInt(params[1]));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] s) {
            super.onPostExecute(s);
            if (s != null) {
                weatherAdapter.clear();
                weatherAdapter.addAll(Arrays.asList(s));
            } else {
                Toast.makeText(getContext(), "There was a problem updating the weather", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
