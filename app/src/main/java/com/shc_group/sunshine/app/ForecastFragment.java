package com.shc_group.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.shc_group.sunshine.R;
import com.shc_group.sunshine.data.WeatherContract;
import com.shc_group.sunshine.exceptions.ConnectionException;
import com.shc_group.sunshine.utils.Utility;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private final static String LOG_TAG_FRAGMENT = ForecastFragment.class.getSimpleName();
    private static final int FORECAST_LOADER = 0;
    private static final String[] FORECAST_COLUMNS = {

            WeatherContract.WeatherEntry.TABLE_NAME + " . " + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };
    //Used to find view without having to cast Object
    @Bind(R.id.listview_forecast)
    protected ListView forecastListView;
    private ForecastAdapter weatherAdapter;


    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG_FRAGMENT,"onCreate called");
        setHasOptionsMenu(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(LOG_TAG_FRAGMENT,"onStop called");
        ButterKnife.unbind(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        weatherAdapter = new ForecastAdapter(getActivity(), null, 0);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //Initializes the ListView through ButterKnife
        ButterKnife.bind(this, view);
        forecastListView.setAdapter(weatherAdapter);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String location = Utility.getPreferredLocation(getActivity());
                    Uri intentQuery = WeatherContract
                            .WeatherEntry
                            .buildWeatherLocationWithDate(location, cursor.getLong(COL_WEATHER_DATE));

                    Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .setData(intentQuery);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.refresh) {
            try {
                updateWeather();
                return true;
            } catch (ConnectionException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "There is an issue with the connection", Toast.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isOnline(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    }

    private void updateWeather() throws ConnectionException{
        String location = Utility.getPreferredLocation(getActivity());
        String days = Utility.getDefaultDays();

        if(isOnline(getContext())) {
            new FetchWeatherTask(getContext()).execute(location, days);
        }else{
            throw new ConnectionException("There is an issue with the connection");
        }
    }

    public void onLocationChanged() {
        try {
            updateWeather();
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG_FRAGMENT, "onDestroy Called");
        ButterKnife.unbind(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String locationSetting = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        weatherAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        weatherAdapter.swapCursor(null);
    }
}
