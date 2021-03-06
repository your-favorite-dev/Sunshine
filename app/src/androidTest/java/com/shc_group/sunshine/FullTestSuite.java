package com.shc_group.sunshine;

import com.shc_group.sunshine.data.TestProvider;
import com.shc_group.sunshine.data.TestUriMatcher;
import com.shc_group.sunshine.data.TestUtilities;
import com.shc_group.sunshine.data.TestWeatherContract;
import com.shc_group.sunshine.data.WeatherDbHelperTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({WeatherDbHelperTest.class, TestUtilities.class, TestProvider.class, TestUriMatcher.class, TestWeatherContract.class})
public class FullTestSuite {
}
