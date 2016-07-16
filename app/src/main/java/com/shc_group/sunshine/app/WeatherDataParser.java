package com.shc_group.sunshine.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.shc_group.sunshine.R;
import com.shc_group.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeatherDataParser {
    private static final String LOG_TAG = WeatherDataParser.class.getSimpleName();
    private Context mContext;

    public WeatherDataParser() {

    }

    public WeatherDataParser(Context context) {
        this.mContext = context;
    }


    private String getReadableDateString(long time) {
// Because the API returns a unix timestamp (measured in seconds),
// it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        shortenedDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return shortenedDateFormat.format(time);
    }

    private String formatHighLows(double high, double low) {
// For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);
        return roundedHigh + "/" + roundedLow;
    }

    String[] convertContentValuesToUXFormat(CopyOnWriteArrayList<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for (int i = 0; i < cvv.size(); i++) {
            ContentValues weatherValues = cvv.get(i);
            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
            resultStrs[i] = getReadableDateString(
                    weatherValues.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;
        }
        return resultStrs;
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
    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        return -1L;
    }

    public String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationSetting)
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

        String[] resultStrs = new String[numDays];
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


        Log.d(LOG_TAG, "Fetch weather complete " + inserted + " inserted");

        resultStrs = convertContentValuesToUXFormat(contentValuesList);

        return resultStrs;

    }

    private double updateUnits(double target) {
        if (mContext != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            String units = sharedPreferences.getString(mContext.getString(R.string.pref_units_key), mContext.getString(R.string.pref_units_default_imperial));
            if (units.equals(mContext.getString(R.string.pref_units_default_imperial))) {
                target = convertToF(target);
            }
        }
        return target;
    }

    private double convertToF(double target) {
        target = (((target * 9.0) / 5.0) + 32.0);
        return target;
    }
}
