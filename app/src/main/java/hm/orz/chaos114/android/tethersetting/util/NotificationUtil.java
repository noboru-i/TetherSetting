package hm.orz.chaos114.android.tethersetting.util;

import hm.orz.chaos114.android.tethersetting.R;
import hm.orz.chaos114.android.tethersetting.TetherSetting;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * ステータスバーに通知するためのユーティリティクラス。
 * @author noboru
 */
public class NotificationUtil {

	/**
	 * 通知を表示する。
	 * @param context コンテキスト
	 * @param title タイトル（ex.アプリ名）
	 * @param message メッセージ
	 */
	public static void notify(Context context, CharSequence title,
			CharSequence message) {

		// 通知をタップされたときのintentを作成
		Intent newIntent = new Intent(context, TetherSetting.class);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				newIntent, 0);

		// Notificationオブジェクトの作成
		Notification notification = new Notification(R.drawable.ic_launcher, title,
				0);
		notification.setLatestEventInfo(context, title, message, contentIntent);
		notification.flags = Notification.FLAG_NO_CLEAR;

		// Managerオブジェクトの取得
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		// 通知
		notificationManager.notify(0, notification);
	}
	
	public static void cancel(Context context) {
		// Managerオブジェクトの取得
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}
}
