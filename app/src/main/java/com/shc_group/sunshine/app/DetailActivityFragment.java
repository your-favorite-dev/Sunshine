package com.shc_group.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.shc_group.sunshine.R;
import com.shc_group.sunshine.data.WeatherContract;
import com.shc_group.sunshine.utils.Utility;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();
    private static final int FORECAST_LOADER = 0;
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private static final String[] FORECAST_COLUMNS = {

            WeatherContract.WeatherEntry.TABLE_NAME + " . " + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    @Bind(R.id.detail_text)
    protected TextView detailTextView;
    private ShareActionProvider shareActionProvider;
    private String forecastDetail;
    private Intent intent;
    private ShareActionProvider mShareActionProvider;


    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        forecastDetail = "Details";
        ButterKnife.bind(this, view);
        detailTextView.setText(forecastDetail);


    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_share, menu);
        storeShareActionProvider(menu);
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        }
        inflater.inflate(R.menu.menu_detail, menu);
        MenuItem menuItem = menu.findItem(R.id.share_menu);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (forecastDetail != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    private void storeShareActionProvider(Menu menu) {
        MenuItem shareMenuItem = menu.findItem(R.id.share_menu);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (intent != null) {

            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, forecastDetail + FORECAST_SHARE_HASHTAG);
        }
        return shareIntent;

    }

    private Uri getIntentDetails() {
        intent = getActivity().getIntent();
        if (intent != null) {
            return intent.getData();
        } else {
            Toast.makeText(getActivity(), "There is an error with the detail data", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri detailQuery = getIntentDetails();
        Log.d(LOG_TAG, "Detail Activity uri: " + detailQuery.toString());
        return new CursorLoader(getActivity(),
                detailQuery,
                FORECAST_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String dateString = Utility.formatDate(
                data.getLong(COL_WEATHER_DATE));

        String weatherDescription =
                data.getString(COL_WEATHER_DESC);

        boolean isMetric = Utility.isMetric(getActivity());

        String high = Utility.formatTemperature(
                data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);

        String low = Utility.formatTemperature(
                data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);

        forecastDetail = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);


        detailTextView.setText(forecastDetail);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        detailTextView.setText("RESET");
    }
}
