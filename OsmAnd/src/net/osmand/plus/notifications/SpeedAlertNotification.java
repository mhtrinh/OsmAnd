package net.osmand.plus.notifications;

import static androidx.core.app.NotificationCompat.PRIORITY_DEFAULT;
import static net.osmand.plus.NavigationService.USED_BY_SPEED_ALERT;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat.Builder;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.speedalert.SpeedAlertPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;

public class SpeedAlertNotification extends OsmandNotification {

	public static final String OSMAND_STOP_SPEED_ALERT_SERVICE_ACTION = "OSMAND_STOP_SPEED_ALERT_SERVICE_ACTION";
	public static final String GROUP_NAME = "SPEED_ALERT";

	public SpeedAlertNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	@Override
	public void init() {
		BroadcastReceiver stopSpeedAlertReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				SpeedAlertPlugin plugin = PluginsHelper.getActivePlugin(SpeedAlertPlugin.class);
				if (plugin != null) {
					plugin.stopMonitoring();
				}
			}
		};
		AndroidUtils.registerBroadcastReceiver(app, OSMAND_STOP_SPEED_ALERT_SERVICE_ACTION, stopSpeedAlertReceiver, true);
	}

	@Override
	public NotificationType getType() {
		return NotificationType.SPEED_ALERT;
	}

	@Override
	public int getPriority() {
		return PRIORITY_DEFAULT;
	}

	@Override
	public boolean isActive() {
		SpeedAlertPlugin plugin = PluginsHelper.getActivePlugin(SpeedAlertPlugin.class);
		return plugin != null && plugin.isMonitoring();
	}

	@Override
	public boolean isUsedByService(@Nullable Service service) {
		NavigationService navService = service instanceof NavigationService
				? (NavigationService) service : app.getNavigationService();
		return navService != null && (navService.getUsedBy() & USED_BY_SPEED_ALERT) != 0;
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public Builder buildNotification(@Nullable Service service, boolean wearable) {
		if (!isEnabled(service)) {
			return null;
		}

		color = app.getColor(R.color.osmand_orange);
		icon = R.drawable.ic_action_speed_limit;
		ongoing = true;

		String title = app.getString(R.string.speed_alert_active);
		SpeedAlertPlugin plugin = PluginsHelper.getPlugin(SpeedAlertPlugin.class);
		if (plugin != null && plugin.isMonitoring()) {
			String speedStr = OsmAndFormatter.getFormattedSpeed((float) (plugin.getCurrentSpeedKmh() / 3.6), app);
			String limitStr = OsmAndFormatter.getFormattedSpeed((float) (plugin.getLimitKmh() / 3.6), app);
			title = app.getString(R.string.speed_alert_active, speedStr, limitStr);
		}

		Builder notificationBuilder = createBuilder(wearable)
				.setContentTitle(title)
				.setContentText(app.getString(R.string.speed_alert_descr));

		Intent stopIntent = new Intent(OSMAND_STOP_SPEED_ALERT_SERVICE_ACTION);
		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(app, 0, stopIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		notificationBuilder.addAction(R.drawable.ic_notification_rec_stop,
				app.getString(R.string.shared_string_control_stop), stopPendingIntent);

		return notificationBuilder;
	}

	@Override
	public int getOsmandNotificationId() {
		return SPEED_ALERT_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_SPEED_ALERT_NOTIFICATION_SERVICE_ID;
	}
}
