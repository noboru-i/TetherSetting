package hm.orz.chaos114.android.tethersetting;

import hm.orz.chaos114.android.tethersetting.util.PreferenceUtil;

import java.lang.reflect.Method;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.EasyTracker;

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

	/** 強制実行処理リスナー */
	OnPreferenceClickListener mForceExecutionListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(final Preference preference) {
			new AlertDialog.Builder(TetherSetting.this)
					.setTitle(R.string.dialog_force_execution_title)
					.setPositiveButton(R.string.dialog_force_on_button,
							mForceOnListener)
					.setNegativeButton(R.string.dialog_force_off_button,
							mForceOffListener)
					.setNeutralButton(android.R.string.cancel, null)
					.show();
			return false;
		}
	};

	/** 強制実行ON */
	DialogInterface.OnClickListener mForceOnListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			mWifiApEnabler.onPreferenceChange(null, true);
		}
	};

	/** 強制実行OFF */
	DialogInterface.OnClickListener mForceOffListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			mWifiApEnabler.onPreferenceChange(null, false);
		}
	};

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(getApplication(),
				getString(R.string.bug_sense_api_key));
		addPreferencesFromResource(R.layout.activity_tether_setting);

		final CheckBoxPreference mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);
		mSsid = (EditTextPreference) findPreference(WIFI_AP_SSID);
		mSecurity = (EditTextPreference) findPreference(WIFI_AP_SECURITY);
		mNotificationSetting = (ListPreference) findPreference(NOTIFICATION_SETTING);
		mWifiApEnabler = new WifiApEnabler(this, mEnableWifiAp);

		mSsid.setOnPreferenceChangeListener(this);
		mSecurity.setOnPreferenceChangeListener(this);
		mNotificationSetting.setOnPreferenceChangeListener(this);
		setSummary();

		final Preference forcedExecution = findPreference("forced_execution");
		forcedExecution.setOnPreferenceClickListener(mForceExecutionListener);
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
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

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	public WifiConfiguration getWifiConfiguration() {
		final WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		try {
			final Method method = manager.getClass().getMethod(
					"getWifiApConfiguration");
			final WifiConfiguration configuration = (WifiConfiguration) method
					.invoke(manager, (Object[]) null);
			return configuration;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean onPreferenceChange(final Preference preference,
			final Object newValue) {
		final String value = (String) newValue;
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
			mWifiApEnabler.setNotification((String) newValue);
			return true;
		}
		preference.setSummary(prefix + value);
		return true;
	}

	private void setSummary() {
		final PreferenceUtil preferenceUtil = new PreferenceUtil(this);
		final String ssid = preferenceUtil
				.getString(TetherSetting.WIFI_AP_SSID);
		final String security = preferenceUtil
				.getString(TetherSetting.WIFI_AP_SECURITY);
		mSsid.setSummary("SSID:" + ssid);
		mSecurity.setSummary("password:" + security);
	}
}
