package com.shc_group.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import com.shc_group.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArrayList;

public class WeatherDataParser {
    private static final String LOG_TAG = WeatherDataParser.class.getSimpleName();
    private Context mContext;

    public WeatherDataParser(Context context) {
        this.mContext = context;
    }


    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (locationCursor != null && locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        // Wait, that worked?  Yes!
        return locationId;
    }

    public void getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationSetting)
            throws JSONException {
        // These are the names of the JSON objects that need to be extracted.

        final String CITY = "city";
        final String CITY_NAME = "name";
        final String COORD = "coord";

        final String LATITUDE = "lat";
        final String LONGITUDE = "lon";

        final String LIST = "list";
        final String PRESSURE = "pressure";
        final String HUMIDITY = "humidity";
        final String WIND_SPEED = "speed";
        final String WIND_DIRECTION = "deg";

        final String TEMPERATURE = "temp";
        final String MAX = "max";
        final String MIN = "min";

        final String WEATHER = "weather";
        final String DESCRIPTION = "main";
        final String WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(LIST);

        JSONObject cityJson = forecastJson.getJSONObject(CITY);
        String cityName = cityJson.getString(CITY_NAME);

        JSONObject cityCoord = cityJson.getJSONObject(COORD);
        double cityLat = cityCoord.getDouble(LATITUDE);
        double cityLon = cityCoord.getDouble(LONGITUDE);

        long locationId = addLocation(locationSetting, cityName, cityLat, cityLon);

        CopyOnWriteArrayList<ContentValues> contentValuesList = new CopyOnWriteArrayList<>();

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        for (int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            String description;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            dateTime = dayTime.setJulianDay(julianStartDay + i);

            pressure = dayForecast.getDouble(PRESSURE);
            humidity = dayForecast.getInt(HUMIDITY);
            windSpeed = dayForecast.getDouble(WIND_SPEED);
            windDirection = dayForecast.getDouble(WIND_DIRECTION);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(WEATHER).getJSONObject(0);
            description = weatherObject.getString(DESCRIPTION);
            weatherId = weatherObject.getInt(WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(TEMPERATURE);
            double high = temperatureObject.getDouble(MAX);
            double low = temperatureObject.getDouble(MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            contentValuesList.add(weatherValues);

        }
        int inserted = 0;

        if (contentValuesList.size() > 0) {
            ContentValues[] cvArray = new ContentValues[contentValuesList.size()];
            contentValuesList.toArray(cvArray);
            inserted = mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
        }
        Log.d(LOG_TAG, "FetchWeatherTask Complete. " + inserted + " Inserted");

    }

}
