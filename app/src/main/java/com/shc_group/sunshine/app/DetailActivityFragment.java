package com.shc_group.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shc_group.sunshine.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {
    private ShareActionProvider shareActionProvider;
    private String forecastDetail;
    private Intent intent;
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();
    @Bind(R.id.forecast_detail_textview)
    protected TextView forecastDetails;

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
        super.onViewCreated(view, savedInstanceState);
        intent = getActivity().getIntent();
        if(intent !=null && intent.hasExtra("details")) {
            forecastDetail = intent.getExtras().getString("details");
            ButterKnife.bind(this, view);
            forecastDetails.setText(forecastDetail);
        }

    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_share, menu);
        storeShareActionProvider(menu);
        if (shareActionProvider != null){
            shareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    private void storeShareActionProvider(Menu menu) {
        MenuItem shareMenuItem = menu.findItem(R.id.share_menu);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            forecastDetail = getActivity().getIntent().getExtras().getString("details") + "#SunshineApp";

            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setType("text/plain")
                    .putExtra("", forecastDetail);
        }
        return shareIntent;

    }
}
