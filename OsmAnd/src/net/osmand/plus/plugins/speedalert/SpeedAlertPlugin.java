package net.osmand.plus.plugins.speedalert;

import static net.osmand.plus.NavigationService.USED_BY_SPEED_ALERT;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.osmand.plus.quickaction.QuickActionType;

public class SpeedAlertPlugin extends OsmandPlugin {

	public static final String ID = "osmand.speedalert";
	private static final Log LOG = PlatformUtil.getLog(SpeedAlertPlugin.class);

	public static final String ACTION_START = "net.osmand.SpeedAlert.START";
	public static final String ACTION_STOP = "net.osmand.SpeedAlert.STOP";

	private boolean active;
	private boolean manuallyStopped = false;
	private long lastAlertTime;
	private long lastLogTime;
	private SoundPool soundPool;
	private int soundId = -1;
	private boolean soundLoaded;
	private Vibrator vibrator;

	public SpeedAlertPlugin(OsmandApplication app) {
		super(app);
		vibrator = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
		pluginPreferences.add(app.getSettings().SPEED_ALERT_ENABLED);
		pluginPreferences.add(app.getSettings().SPEED_ALERT_THRESHOLD_KMH);
		pluginPreferences.add(app.getSettings().SPEED_ALERT_FALLBACK_KMH);
		pluginPreferences.add(app.getSettings().SPEED_ALERT_INTERVAL_S);
		pluginPreferences.add(app.getSettings().SPEED_ALERT_VERBOSE_LOG);
		pluginPreferences.add(app.getSettings().SPEED_ALERT_VERBOSE_LOG_PERIOD);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return app.getString(R.string.speed_alert_name);
	}

	@Override
	public String getDescription(boolean linksEnabled) {
		return app.getString(R.string.speed_alert_descr);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_speed_limit;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.ic_action_speed_limit);
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (ACTION_START.equals(intent.getAction())) {
					startMonitoring();
				} else if (ACTION_STOP.equals(intent.getAction())) {
					stopMonitoring();
				}
			}
		};
		AndroidUtils.registerBroadcastReceiver(app, ACTION_START, receiver, true);
		AndroidUtils.registerBroadcastReceiver(app, ACTION_STOP, receiver, true);
		return true;
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		ApplicationMode appMode = activity.getApp().getSettings().getApplicationMode();
		if (app.getSettings().SPEED_ALERT_ENABLED.getModeValue(appMode) && !manuallyStopped && !active) {
			activate();
			app.startNavigationService(USED_BY_SPEED_ALERT);
		}
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		deactivate();
		manuallyStopped = false;
		if (app.getNavigationService() != null) {
			app.getNavigationService().stopIfNeeded(app, USED_BY_SPEED_ALERT);
		}
	}

	public void startMonitoring() {
		manuallyStopped = false;
		activate();
		app.startNavigationService(USED_BY_SPEED_ALERT);
		if (app.getSettings().SPEED_ALERT_VERBOSE_LOG.getModeValue(app.getSettings().getApplicationMode())) {
			LOG.warn("SPEEDALERT: Monitoring started");
		}
	}

	public void stopMonitoring() {
		manuallyStopped = true;
		deactivate();
		if (app.getNavigationService() != null) {
			app.getNavigationService().stopIfNeeded(app, USED_BY_SPEED_ALERT);
		}
		if (app.getSettings().SPEED_ALERT_VERBOSE_LOG.getModeValue(app.getSettings().getApplicationMode())) {
			LOG.warn("SPEEDALERT: Monitoring stopped");
		}
	}

	public boolean isMonitoring() {
		return active;
	}

	public void activate() {
		if (!active) {
			active = true;
			loadSound();
		}
	}

	public void deactivate() {
		if (active) {
			active = false;
			releaseSound();
			lastAlertTime = 0;
		}
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.SPEED_ALERT_SETTINGS;
	}

	public void manualTestAlert() {
		LOG.warn("SPEEDALERT: Manual test alert triggered");
		loadSound(); // Ensure sound is loaded
		playAlert();
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		return Collections.singletonList(net.osmand.plus.quickaction.actions.SpeedAlertToggleAction.TYPE);
	}

	private void loadSound() {
		if (soundPool == null) {
			AudioAttributes attr = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();
			soundPool = new SoundPool.Builder().setAudioAttributes(attr).setMaxStreams(1).build();
			soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> soundLoaded = (status == 0));
		}
		if (soundId == -1) {
			try {
				AssetFileDescriptor afd = app.getAssets().openFd("sounds/ding.ogg");
				soundId = soundPool.load(afd, 1);
				afd.close();
			} catch (IOException e) {
				LOG.error("SPEEDALERT: Failed to load speed alert sound", e);
			}
		}
	}

	private void releaseSound() {
		if (soundPool != null) {
			soundPool.release();
			soundPool = null;
			soundId = -1;
			soundLoaded = false;
		}
	}

	@Override
	public void updateLocation(Location location) {
		if (!active || location == null) {
			return;
		}

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		double currentSpeedKmh = location.getSpeed() * 3.6;
		RouteDataObject routeObject = app.getLocationProvider().getLastKnownRouteSegment();
		double limitKmh;
		String limitSource;

		if (routeObject != null) {
			boolean direction = routeObject.bearingVsRouteDirection(location);
			float maxSpeed = routeObject.getMaximumSpeed(direction);
			if (maxSpeed > 0 && maxSpeed != RouteDataObject.NONE_MAX_SPEED) {
				limitKmh = maxSpeed * 3.6;
				limitSource = "route";
			} else {
				limitKmh = app.getSettings().SPEED_ALERT_FALLBACK_KMH.getModeValue(appMode);
				limitSource = "fallback";
			}
		} else {
			limitKmh = app.getSettings().SPEED_ALERT_FALLBACK_KMH.getModeValue(appMode);
			limitSource = "fallback";
		}

		double thresholdKmh = app.getSettings().SPEED_ALERT_THRESHOLD_KMH.getModeValue(appMode);
		long now = System.currentTimeMillis();
		long intervalMs = app.getSettings().SPEED_ALERT_INTERVAL_S.getModeValue(appMode) * 1000L;
		long timeSinceLastAlert = lastAlertTime > 0 ? now - lastAlertTime : 0;
		boolean alertFired = currentSpeedKmh > limitKmh + thresholdKmh && now - lastAlertTime >= intervalMs;

		if (app.getSettings().SPEED_ALERT_VERBOSE_LOG.getModeValue(appMode)) {
			long logPeriodMs = app.getSettings().SPEED_ALERT_VERBOSE_LOG_PERIOD.getModeValue(appMode) * 1000L;
			if (alertFired || now - lastLogTime >= logPeriodMs) {
				LOG.warn("SPEEDALERT: speed=" + String.format("%.1f", currentSpeedKmh)
						+ " limit=" + String.format("%.1f", limitKmh)
						+ " (" + limitSource + ")"
						+ " threshold=" + String.format("%.1f", thresholdKmh)
						+ " alertFired=" + alertFired
						+ " timeSinceLast=" + timeSinceLastAlert + " ms");
				lastLogTime = now;
			}
		}

		if (alertFired) {
			playAlert();
			lastAlertTime = now;
		} else if (currentSpeedKmh <= limitKmh) {
			lastAlertTime = 0;
		}
	}

	private void playAlert() {
		if (soundPool != null && soundLoaded) {
			soundPool.play(soundId, 1, 1, 1, 0, 1);
		}
		if (vibrator != null && vibrator.hasVibrator()) {
			long[] pattern = {0, 100, 100, 100, 100, 100};
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
			} else {
				vibrator.vibrate(pattern, -1);
			}
		}
	}
}
