package com.shc_group.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.shc_group.sunshine.R;
import com.shc_group.sunshine.utils.Utility;

public class MainActivity extends AppCompatActivity {

    private String mLocation;
    private String FORECASTFRAGMENT_TAG = "FFTAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment(), FORECASTFRAGMENT_TAG)
                    .commit();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        String preferredLocation = Utility.getPreferredLocation(this);
        if (preferredLocation != null && !preferredLocation.equals(getmLocation())) {

            ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentByTag(FORECASTFRAGMENT_TAG);
            forecastFragment.onLocationChanged();
            setmLocation(preferredLocation);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getmLocation() {
        return mLocation;
    }

    public void setmLocation(String mLocation) {
        this.mLocation = mLocation;
    }
}
