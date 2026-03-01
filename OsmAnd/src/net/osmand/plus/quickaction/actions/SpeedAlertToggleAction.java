package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SPEED_ALERT_TOGGLE_ACTION_ID;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.speedalert.SpeedAlertPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class SpeedAlertToggleAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SPEED_ALERT_TOGGLE_ACTION_ID,
			"speedalert.toggle", SpeedAlertToggleAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.speed_alert_name)
			.iconRes(R.drawable.ic_action_speed_limit).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public SpeedAlertToggleAction() {
		super(TYPE);
	}

	public SpeedAlertToggleAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		SpeedAlertPlugin plugin = PluginsHelper.getPlugin(SpeedAlertPlugin.class);
		if (plugin == null || !plugin.isActive()) {
			Toast.makeText(mapActivity, R.string.speed_alert_enable_plugin_first, Toast.LENGTH_SHORT).show();
			return;
		}

		if (plugin.isMonitoring()) {
			plugin.stopMonitoring();
		} else {
			plugin.startMonitoring();
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.speed_alert_descr);
		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		return isActionWithSlash(app) ? app.getString(R.string.speed_alert_monitoring_start) : app.getString(R.string.speed_alert_monitoring_stop);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		SpeedAlertPlugin plugin = PluginsHelper.getPlugin(SpeedAlertPlugin.class);
		return plugin != null && !plugin.isMonitoring();
	}
}
