package com.shc_group.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import java.util.ArrayList;

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
    private ArrayAdapter<String> weatherAdapter;

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

    private boolean isOnline(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    }

    private void updateWeather() throws ConnectionException{
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String zipCode = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        String days = "14";
        if(isOnline(getContext())) {
            new FetchWeatherTask(getContext(), weatherAdapter).execute(zipCode, days);
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

}
