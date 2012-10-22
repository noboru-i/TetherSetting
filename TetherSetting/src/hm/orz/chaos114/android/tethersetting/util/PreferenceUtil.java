package hm.orz.chaos114.android.tethersetting.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Preferenceのユーティリティクラス
 * 
 * @author ishikuranoboru
 */
public class PreferenceUtil {

	/** 暗号化キー：String */
	public static final String KEY_SECRET_KEY = "secret_key";

	/** twitterトークン：String */
	public static final String KEY_TOKEN = "token";

	/** twitterトークンシークレット：String */
	public static final String KEY_TOKEN_SECRET = "token_secret";

	/** 音の出力要否：boolean */
	public static final String KEY_SOUND = "sound";

	/** 最後に表示していたステージ番号：int */
	public static final String KEY_LAST_STAGE_NO = "last_stage_no";

	private SharedPreferences sp;

	public PreferenceUtil(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context
				.getApplicationContext());
	}

	public void putString(String key, String value) {
		Editor editor = sp.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public void putInt(String key, int value) {
		Editor editor = sp.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public void putBoolean(String key, boolean value) {
		Editor editor = sp.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public String getString(String key) {
		return sp.getString(key, null);
	}

	public int getInt(String key) {
		return sp.getInt(key, 0);
	}

	public boolean getBoolean(String key) {
		return sp.getBoolean(key, false);
	}
	
	public void remove(String key) {
		Editor editor = sp.edit();
		editor.remove(key);
		editor.commit();
	}
}
