package hm.orz.chaos114.android.tethersetting;

import hm.orz.chaos114.android.tethersetting.util.PreferenceUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.Log;

public class WifiApEnabler implements Preference.OnPreferenceChangeListener {
	private final Context mContext;
	private final CheckBoxPreference mCheckBox;
	private final CharSequence mOriginalSummary;

	private WifiManager mWifiManager;
	private final IntentFilter mIntentFilter;

	ConnectivityManager mCm;
	private String[] mWifiRegexs;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (getStringField(WifiManager.class,
					"WIFI_AP_STATE_CHANGED_ACTION").equals(action)) {
				handleWifiApStateChanged(intent
						.getIntExtra(
								getStringField(WifiManager.class,
										"EXTRA_WIFI_AP_STATE"),
								getIntField(WifiManager.class,
										"WIFI_AP_STATE_FAILED")));
			} else if (getStringField(ConnectivityManager.class,
					"ACTION_TETHER_STATE_CHANGED").equals(action)) {
				ArrayList<String> available = intent
						.getStringArrayListExtra(getStringField(
								ConnectivityManager.class,
								"EXTRA_AVAILABLE_TETHER"));
				ArrayList<String> active = intent
						.getStringArrayListExtra(getStringField(
								ConnectivityManager.class,
								"EXTRA_ACTIVE_TETHER"));
				ArrayList<String> errored = intent
						.getStringArrayListExtra(getStringField(
								ConnectivityManager.class,
								"EXTRA_ERRORED_TETHER"));
				updateTetherState(available.toArray(), active.toArray(),
						errored.toArray());
			}

		}
	};

	public WifiApEnabler(Context context, CheckBoxPreference checkBox) {
		mContext = context;
		mCheckBox = checkBox;
		mOriginalSummary = checkBox.getSummary();
		checkBox.setPersistent(false);

		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		mCm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		mWifiRegexs = getTetherableWifiRegexs(mCm);

		mIntentFilter = new IntentFilter(getStringField(WifiManager.class,
				"WIFI_AP_STATE_CHANGED_ACTION"));
		mIntentFilter.addAction(getStringField(ConnectivityManager.class,
				"ACTION_TETHER_STATE_CHANGED"));
	}

	private String getStringField(Class<? extends Object> c, String name) {
		return (String) getObjectField(c, name);
	}

	private int getIntField(Class<? extends Object> c, String name) {
		return (Integer) getObjectField(c, name);
	}

	private Object getObjectField(Class<? extends Object> c, String name) {
		try {
			Field f = c.getField(name);
			return f.get(null);
		} catch (Exception e) {
			throw new RuntimeException(c.getSimpleName() + " : " + name, e);
		}
	}

	private String[] getTetherableWifiRegexs(ConnectivityManager manager) {
		try {
			Method method = manager.getClass().getMethod(
					"getTetherableWifiRegexs", (Class[]) null);
			return (String[]) method.invoke(manager);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void resume() {
		mContext.registerReceiver(mReceiver, mIntentFilter);
		enableWifiCheckBox();
		mCheckBox.setOnPreferenceChangeListener(this);
	}

	public void pause() {
		mContext.unregisterReceiver(mReceiver);
		mCheckBox.setOnPreferenceChangeListener(null);
	}

	private void enableWifiCheckBox() {
		boolean isAirplaneMode = Settings.System.getInt(
				mContext.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) != 0;
		if (!isAirplaneMode) {
			mCheckBox.setEnabled(true);
		} else {
			mCheckBox.setEnabled(false);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object value) {
		boolean enable = (Boolean) value;

		PreferenceUtil preferenceUtil = new PreferenceUtil(mContext);

		/**
		 * Disable Wifi if enabling tethering
		 */
		int wifiState = mWifiManager.getWifiState();
		if (enable
				&& ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
			mWifiManager.setWifiEnabled(false);
			preferenceUtil.putInt("wifi_saved_state", 1);
		}

		if (setWifiApEnabled(mWifiManager, getWifiConfiguration(), enable)) {
			/* Disable here, enabled on receiving success broadcast */
			mCheckBox.setEnabled(false);
		} else {
			mCheckBox.setSummary(R.string.wifi_error);
		}

		/**
		 * If needed, restore Wifi on tether disable
		 */
		if (!enable) {
			int wifiSavedState = preferenceUtil.getInt("wifi_saved_state");
			if (wifiSavedState == 1) {
				mWifiManager.setWifiEnabled(true);
				preferenceUtil.putInt("wifi_saved_state", 0);
			}
		}

		return false;
	}

	private WifiConfiguration getWifiConfiguration() {
		PreferenceUtil preferenceUtil = new PreferenceUtil(mContext);
		String ssid = preferenceUtil.getString(TetherSetting.WIFI_AP_SSID);
		String security = preferenceUtil
				.getString(TetherSetting.WIFI_AP_SECURITY);
		Log.d("test", "ssid = " + ssid + ", security = " + security);

		WifiConfiguration configuration = new WifiConfiguration();
		configuration.SSID = ssid;
		if (security == null || security.length() == 0) {
			// passwordが設定されていない
			configuration.allowedKeyManagement.set(KeyMgmt.NONE);
		} else {
			// passwordが設定されている
			configuration.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			configuration.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			configuration.preSharedKey = security;
		}
		return configuration;
	}

	void updateConfigSummary(WifiConfiguration wifiConfig) {
		String s = mContext
				.getString(R.string.wifi_tether_configure_ssid_default);
		mCheckBox.setSummary(String.format(
				mContext.getString(R.string.wifi_tether_enabled_subtext),
				(wifiConfig == null) ? s : wifiConfig.SSID));
	}

	private void updateTetherState(Object[] available, Object[] tethered,
			Object[] errored) {
		boolean wifiTethered = false;
		boolean wifiErrored = false;

		for (Object o : tethered) {
			String s = (String) o;
			for (String regex : mWifiRegexs) {
				if (s.matches(regex))
					wifiTethered = true;
			}
		}
		for (Object o : errored) {
			String s = (String) o;
			for (String regex : mWifiRegexs) {
				if (s.matches(regex))
					wifiErrored = true;
			}
		}

		if (wifiTethered) {
			WifiConfiguration wifiConfig = getWifiApConfiguration(mWifiManager);
			updateConfigSummary(wifiConfig);
		} else if (wifiErrored) {
			mCheckBox.setSummary(R.string.wifi_error);
		}
	}

	private boolean setWifiApEnabled(WifiManager wifiManager,
			WifiConfiguration wifiConfig, boolean enabled) {
		Log.d("test", "WifiConfiguration = " + wifiConfig);
		try {
			Method method = wifiManager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, boolean.class);
			return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private WifiConfiguration getWifiApConfiguration(WifiManager wifiManager) {
		try {
			Method method = wifiManager.getClass().getMethod(
					"getWifiApConfiguration");
			return (WifiConfiguration) method.invoke(wifiManager);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void handleWifiApStateChanged(int state) {
		switch (state) {
		// case WifiManager.WIFI_AP_STATE_ENABLING:
		case 12:
			mCheckBox.setEnabled(false);
			break;
		// case WifiManager.WIFI_AP_STATE_ENABLED:
		case 13:
			/**
			 * Summary on enable is handled by tether broadcast notice
			 */
			mCheckBox.setChecked(true);
			/* Doesnt need the airplane check */
			mCheckBox.setEnabled(true);
			break;
		// case WifiManager.WIFI_AP_STATE_DISABLING:
		case 10:
			mCheckBox.setEnabled(false);
			break;
		// case WifiManager.WIFI_AP_STATE_DISABLED:
		case 11:
			mCheckBox.setChecked(false);
			mCheckBox.setSummary(mOriginalSummary);
			enableWifiCheckBox();
			break;
		default:
			mCheckBox.setChecked(false);
			enableWifiCheckBox();
		}
	}
}