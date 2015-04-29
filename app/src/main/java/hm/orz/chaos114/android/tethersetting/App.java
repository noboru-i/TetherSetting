package hm.orz.chaos114.android.tethersetting;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class App extends Application {
    private static final String PROPERTY_ID = "UA-42750443-1";

    private Tracker mAppTracker;
    public synchronized Tracker getTracker() {
        if (mAppTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mAppTracker = analytics.newTracker(PROPERTY_ID);
        }

        return mAppTracker;
    }
}
