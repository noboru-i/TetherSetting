package hm.orz.chaos114.android.tethersetting;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class AdmobPreference extends ListPreference {
    public AdmobPreference(Context context) {
        super(context);
    }

    public AdmobPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        AdView adView = (AdView)view.findViewById(R.id.adView);

        // AdMob
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }
}
