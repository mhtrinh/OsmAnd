package net.osmand.plus.plugins.speedalert;

import static net.osmand.plus.plugins.PluginInfoFragment.PLUGIN_INFO;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpeedAlertSettingsFragment extends BaseSettingsFragment implements CopyAppModePrefsListener, ResetAppModePrefsListener {

	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String TEST_ALERT = "test_alert";

	private boolean showSwitchProfile;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			showSwitchProfile = args.getBoolean(PLUGIN_INFO, false);
		}
	}

	@Override
	protected void setupPreferences() {
		setupSoundModePref();
		setupVibratePref();
		setupThresholdPref();
		setupFallbackPref();
		setupIntervalPref();
		setupVerboseLogPeriodPref();
		setupBottomButtons();
	}

	private void setupSoundModePref() {
		SpeedAlertSoundMode[] modes = SpeedAlertSoundMode.values();
		String[] entries = {
			getString(R.string.speed_alert_sound_adaptive),
			getString(R.string.speed_alert_sound_speaker),
			getString(R.string.shared_string_none)
		};
		ListPreferenceEx pref = findPreference(settings.SPEED_ALERT_SOUND_MODE.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(modes);
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, showSwitchProfile);
		}
	}

	private void setupVibratePref() {
		SpeedAlertVibrateMode[] modes = SpeedAlertVibrateMode.values();
		String[] entries = {
			getString(R.string.speed_alert_vibrate_pattern_1),
			getString(R.string.speed_alert_vibrate_pattern_2),
			getString(R.string.shared_string_none)
		};
		ListPreferenceEx pref = findPreference(settings.SPEED_ALERT_VIBRATE_MODE.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(modes);
		}
	}

	private void setupThresholdPref() {
		Integer[] values = {0, 1, 2, 5, 10, 15, 20};
		String[] entries = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			entries[i] = values[i] + " " + getString(R.string.km_h);
		}
		ListPreferenceEx pref = findPreference(settings.SPEED_ALERT_THRESHOLD_KMH.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(values);
		}
	}

	private void setupFallbackPref() {
		Integer[] values = {30, 40, 50, 60, 70, 80, 90, 100, 110, 120};
		String[] entries = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			entries[i] = values[i] + " " + getString(R.string.km_h);
		}
		ListPreferenceEx pref = findPreference(settings.SPEED_ALERT_FALLBACK_KMH.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(values);
		}
	}

	private void setupIntervalPref() {
		int[] SECONDS = {0, 1, 2, 3, 5, 10, 15, 20, 30, 60, 90};
		Map<Integer, String> entry = new LinkedHashMap<>();
		for (int second : SECONDS) {
			if (second == 0) {
				entry.put(0, getString(R.string.shared_string_none));
			} else {
				entry.put(second, second + " " + getString(R.string.int_seconds));
			}
		}
		ListPreferenceEx pref = findPreference(settings.SPEED_ALERT_INTERVAL_S.getId());
		if (pref != null) {
			pref.setEntries(entry.values().toArray(new String[0]));
			pref.setEntryValues(entry.keySet().toArray());
		}
	}

	private void setupVerboseLogPeriodPref() {
		int[] SECONDS = {1, 2, 3, 5, 10, 30, 60};
		String[] entries = new String[SECONDS.length];
		Integer[] values = new Integer[SECONDS.length];
		for (int i = 0; i < SECONDS.length; i++) {
			entries[i] = SECONDS[i] + " " + getString(R.string.int_seconds);
			values[i] = SECONDS[i];
		}
		ListPreferenceEx pref = findPreference(settings.SPEED_ALERT_VERBOSE_LOG_PERIOD.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(values);
		}
	}

	private void setupBottomButtons() {
		int profileColor = getActiveProfileColor();

		Preference divider = createDividerPref();
		addOnPreferencesScreen(divider);

		Preference testAlert = new Preference(requireContext());
		testAlert.setKey(TEST_ALERT);
		testAlert.setLayoutResource(R.layout.preference_button);
		testAlert.setTitle(R.string.speed_alert_trigger_test);
		testAlert.setIcon(getPaintedIcon(R.drawable.ic_action_volume_up, profileColor));
		testAlert.setPersistent(false);
		addOnPreferencesScreen(testAlert);

		Preference resetToDefault = new Preference(requireContext());
		resetToDefault.setKey(RESET_TO_DEFAULT);
		resetToDefault.setLayoutResource(R.layout.preference_button);
		resetToDefault.setTitle(R.string.reset_plugin_to_default);
		resetToDefault.setIcon(getPaintedIcon(R.drawable.ic_action_reset_to_default_dark, profileColor));
		resetToDefault.setPersistent(false);
		addOnPreferencesScreen(resetToDefault);

		Preference copyPluginSettings = new Preference(requireContext());
		copyPluginSettings.setKey(COPY_PLUGIN_SETTINGS);
		copyPluginSettings.setLayoutResource(R.layout.preference_button);
		copyPluginSettings.setTitle(R.string.copy_from_other_profile);
		copyPluginSettings.setIcon(getPaintedIcon(R.drawable.ic_action_copy, profileColor));
		copyPluginSettings.setPersistent(false);
		addOnPreferencesScreen(copyPluginSettings);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (TEST_ALERT.equals(key)) {
			SpeedAlertPlugin plugin = PluginsHelper.getPlugin(SpeedAlertPlugin.class);
			if (plugin != null) {
				plugin.manualTestAlert();
			}
		} else if (RESET_TO_DEFAULT.equals(key)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, getSelectedAppMode(), this);
			}
		} else if (COPY_PLUGIN_SETTINGS.equals(key)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, getSelectedAppMode());
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		SpeedAlertPlugin plugin = PluginsHelper.getPlugin(SpeedAlertPlugin.class);
		if (plugin != null) {
			settings.copyProfilePreferences(appMode, getSelectedAppMode(), plugin.getPreferences());
			updateAllSettings();
		}
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		SpeedAlertPlugin plugin = PluginsHelper.getPlugin(SpeedAlertPlugin.class);
		if (plugin != null) {
			settings.resetProfilePreferences(appMode, plugin.getPreferences());
			app.showToastMessage(R.string.plugin_prefs_reset_successful);
			updateAllSettings();
		}
	}
}
