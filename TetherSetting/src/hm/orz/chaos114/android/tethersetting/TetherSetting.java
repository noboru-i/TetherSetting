package hm.orz.chaos114.android.tethersetting;

import hm.orz.chaos114.android.tethersetting.util.PreferenceUtil;

import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

public class TetherSetting extends PreferenceActivity implements
		Preference.OnPreferenceChangeListener {

	public static final String WIFI_AP_SSID = "wifi_ap_ssid";
	public static final String WIFI_AP_SECURITY = "wifi_ap_security";
	public static final String ENABLE_WIFI_AP = "enable_wifi_ap";
	
	public static final String NOTIFICATION_SETTING = "notification_setting";

	private WifiApEnabler mWifiApEnabler;
	private EditTextPreference mSsid;
	private EditTextPreference mSecurity;
	private ListPreference mNotificationSetting;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(getApplication(),
				getString(R.string.bug_sense_api_key));
		addPreferencesFromResource(R.layout.activity_tether_setting);

		CheckBoxPreference mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);
		mSsid = (EditTextPreference) findPreference(WIFI_AP_SSID);
		mSecurity = (EditTextPreference) findPreference(WIFI_AP_SECURITY);
		mNotificationSetting = (ListPreference) findPreference(NOTIFICATION_SETTING);
		mWifiApEnabler = new WifiApEnabler(this, mEnableWifiAp);

		mSsid.setOnPreferenceChangeListener(this);
		mSecurity.setOnPreferenceChangeListener(this);
		mNotificationSetting.setOnPreferenceChangeListener(this);
		setSummary();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mWifiApEnabler.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWifiApEnabler.pause();
	}

	public WifiConfiguration getWifiConfiguration() {
		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		try {
			Method method = manager.getClass().getMethod(
					"getWifiApConfiguration");
			WifiConfiguration configuration = (WifiConfiguration) method
					.invoke(manager, (Object[]) null);
			return configuration;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String value = (String) newValue;
		String prefix = "";
		if (WIFI_AP_SSID.equals(preference.getKey())) {
			prefix = "SSID:";
			if (value.length() == 0) {
				return false;
			}
		} else if (WIFI_AP_SECURITY.equals(preference.getKey())) {
			if (value.length() != 0
					&& (value.length() < 8 || value.length() > 63)) {
				// 8文字未満 or 63文字を超える場合
				Toast.makeText(this,
						getString(R.string.toast_validation_error_password),
						Toast.LENGTH_LONG).show();
				return false;
			}
			prefix = "password:";
		} else if (NOTIFICATION_SETTING.equals(preference.getKey())) {
			mWifiApEnabler.setNotification((String)newValue);
			return true;
		}
		preference.setSummary(prefix + value);
		return true;
	}

	private void setSummary() {
		PreferenceUtil preferenceUtil = new PreferenceUtil(this);
		String ssid = preferenceUtil.getString(TetherSetting.WIFI_AP_SSID);
		String security = preferenceUtil
				.getString(TetherSetting.WIFI_AP_SECURITY);
		mSsid.setSummary("SSID:" + ssid);
		mSecurity.setSummary("password:" + security);
	}
}
