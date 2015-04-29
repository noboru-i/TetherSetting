package hm.orz.chaos114.android.tethersetting;

import hm.orz.chaos114.android.tethersetting.util.NotificationUtil;
import hm.orz.chaos114.android.tethersetting.util.PreferenceUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.ProgressDialog;
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
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;

public class WifiApEnabler implements Preference.OnPreferenceChangeListener {
	private static final String TAG = WifiApEnabler.class.getSimpleName();

	private final Context mContext;
	private final CheckBoxPreference mCheckBox;
	private final CharSequence mOriginalSummary;

	private final WifiManager mWifiManager;
	private final IntentFilter mIntentFilter;

	ConnectivityManager mCm;
	private final String[] mWifiRegexs;

	private final ProgressDialog mDialog;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
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
				final ArrayList<String> available = intent
						.getStringArrayListExtra(getStringField(
								ConnectivityManager.class,
								"EXTRA_AVAILABLE_TETHER"));
				final ArrayList<String> active = intent
						.getStringArrayListExtra(getStringField(
								ConnectivityManager.class,
								"EXTRA_ACTIVE_TETHER"));
				final ArrayList<String> errored = intent
						.getStringArrayListExtra(getStringField(
								ConnectivityManager.class,
								"EXTRA_ERRORED_TETHER"));
				updateTetherState(available.toArray(), active.toArray(),
						errored.toArray());
			}

		}
	};

	public WifiApEnabler(final Context context, final CheckBoxPreference checkBox) {
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
		mDialog = new ProgressDialog(mContext);
	}

	private String getStringField(final Class<? extends Object> c, final String name) {
		return (String) getObjectField(c, name);
	}

	private int getIntField(final Class<? extends Object> c, final String name) {
		return (Integer) getObjectField(c, name);
	}

	private Object getObjectField(final Class<? extends Object> c, final String name) {
		try {
			final Field f = c.getField(name);
			return f.get(null);
		} catch (final Exception e) {
			throw new RuntimeException(c.getSimpleName() + " : " + name, e);
		}
	}

	private String[] getTetherableWifiRegexs(final ConnectivityManager manager) {
		try {
			final Method method = manager.getClass().getMethod(
					"getTetherableWifiRegexs", (Class[]) null);
			return (String[]) method.invoke(manager);
		} catch (final Exception e) {
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
		final boolean isAirplaneMode = Settings.System.getInt(
				mContext.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) != 0;
		if (!isAirplaneMode) {
			mCheckBox.setEnabled(true);
		} else {
			mCheckBox.setEnabled(false);
		}
	}

	@Override
	public boolean onPreferenceChange(final Preference preference, final Object value) {
		final boolean enable = (Boolean) value;

		final PreferenceUtil preferenceUtil = new PreferenceUtil(mContext);

		/**
		 * Disable Wifi if enabling tethering
		 */
		final int wifiState = mWifiManager.getWifiState();
		if (enable
				&& ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
			mWifiManager.setWifiEnabled(false);
			preferenceUtil.putInt("wifi_saved_state", 1);
		}

		if (setWifiApEnabled(mWifiManager, getWifiConfiguration(), enable)) {
			/* Disable here, enabled on receiving success broadcast */
			// mCheckBox.setEnabled(false);
			showDialog();
		} else {
			mCheckBox.setSummary(R.string.wifi_error);
		}

		/**
		 * If needed, restore Wifi on tether disable
		 */
		if (!enable) {
			final int wifiSavedState = preferenceUtil.getInt("wifi_saved_state");
			if (wifiSavedState == 1) {
				mWifiManager.setWifiEnabled(true);
				preferenceUtil.putInt("wifi_saved_state", 0);
			}
		}

		return false;
	}

	private WifiConfiguration getWifiConfiguration() {
		final PreferenceUtil preferenceUtil = new PreferenceUtil(mContext);
		final String ssid = preferenceUtil.getString(TetherSetting.WIFI_AP_SSID);
		final String security = preferenceUtil
				.getString(TetherSetting.WIFI_AP_SECURITY);
		Log.d(TAG, "ssid = " + ssid + ", security = " + security);

		final WifiConfiguration configuration = new WifiConfiguration();
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

	void updateConfigSummary(final WifiConfiguration wifiConfig) {
		final String s = mContext
				.getString(R.string.wifi_tether_configure_ssid_default);
		mCheckBox.setSummary(String.format(
				mContext.getString(R.string.wifi_tether_enabled_subtext),
				(wifiConfig == null) ? s : wifiConfig.SSID));
	}

	private void updateTetherState(final Object[] available, final Object[] tethered,
			final Object[] errored) {
		boolean wifiTethered = false;
		boolean wifiErrored = false;

		for (final Object o : tethered) {
			final String s = (String) o;
			for (final String regex : mWifiRegexs) {
				if (s.matches(regex)) {
					wifiTethered = true;
				}
			}
		}
		for (final Object o : errored) {
			final String s = (String) o;
			for (final String regex : mWifiRegexs) {
				if (s.matches(regex)) {
					wifiErrored = true;
				}
			}
		}

		if (wifiTethered) {
			final WifiConfiguration wifiConfig = getWifiApConfiguration(mWifiManager);
			updateConfigSummary(wifiConfig);
		} else if (wifiErrored) {
			mCheckBox.setSummary(R.string.wifi_error);
		}
	}

	private boolean setWifiApEnabled(final WifiManager wifiManager,
			final WifiConfiguration wifiConfig, final boolean enabled) {
		Log.d(TAG, "WifiConfiguration = " + wifiConfig);
		try {
			final Method method = wifiManager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, boolean.class);
			return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private WifiConfiguration getWifiApConfiguration(final WifiManager wifiManager) {
		try {
			final Method method = wifiManager.getClass().getMethod(
					"getWifiApConfiguration");
			return (WifiConfiguration) method.invoke(wifiManager);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void handleWifiApStateChanged(final int state) {
		Log.d(TAG, "handleWifiApStateChanged:state = " + state);

		switch (state) {
		// case WifiManager.WIFI_AP_STATE_ENABLING:
		case 12:
			// mCheckBox.setEnabled(false);
			showDialog();
			break;
		// case WifiManager.WIFI_AP_STATE_ENABLED:
		case 13:
			/**
			 * Summary on enable is handled by tether broadcast notice
			 */
			mCheckBox.setChecked(true);
			/* Doesnt need the airplane check */
			// mCheckBox.setEnabled(true);
			hideDialog();
			break;
		// case WifiManager.WIFI_AP_STATE_DISABLING:
		case 10:
			// mCheckBox.setEnabled(false);
			showDialog();
			break;
		// case WifiManager.WIFI_AP_STATE_DISABLED:
		case 11:
			mCheckBox.setChecked(false);
			mCheckBox.setSummary(mOriginalSummary);
			enableWifiCheckBox();
			hideDialog();
			break;
		default:
			mCheckBox.setChecked(false);
			enableWifiCheckBox();
		}
		setNotification();
	}

	public void setNotification(final String notificationSetting) {
		boolean showNotification = false;
		Log.d(TAG, "notificationSetting = " + notificationSetting);
		if ("1".equals(notificationSetting)) {
			showNotification = true;
		} else if ("2".equals(notificationSetting)) {
			final CheckBoxPreference mEnableWifiAp = (CheckBoxPreference) ((PreferenceActivity) mContext)
					.findPreference(TetherSetting.ENABLE_WIFI_AP);
			if (mEnableWifiAp.isEnabled() && mEnableWifiAp.isChecked()) {
				showNotification = true;
			}
		}

		if (showNotification) {
			NotificationUtil.notify(mContext,
					mContext.getString(R.string.app_name),
					mContext.getString(R.string.notification_message));
		} else {
			NotificationUtil.cancel(mContext);
		}
	}

	public void setNotification() {
		final PreferenceUtil preferenceUtil = new PreferenceUtil(mContext);
		final String notificationSetting = preferenceUtil
				.getString(TetherSetting.NOTIFICATION_SETTING);
		setNotification(notificationSetting);
	}

	private void showDialog() {
		if (mDialog.isShowing()) {
			return;
		}
		mDialog.setMessage(mContext.getString(R.string.dialog_progressing));
		mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mDialog.show();
	}

	private void hideDialog() {
		if (!mDialog.isShowing()) {
			return;
		}
		mDialog.dismiss();
	}
}