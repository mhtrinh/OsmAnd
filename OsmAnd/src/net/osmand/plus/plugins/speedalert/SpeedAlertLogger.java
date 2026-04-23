package net.osmand.plus.plugins.speedalert;

import android.content.Context;
import androidx.annotation.NonNull;
import net.osmand.plus.OsmandApplication;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpeedAlertLogger {

	private static final String LOG_FILE_NAME = "speed_alert.log";
	private static final String LOG_FILE_BAK = "speed_alert.log.bak";
	private static final long MAX_LOG_SIZE = 1024 * 1024; // 1MB
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

	public static synchronized void log(@NonNull OsmandApplication app, @NonNull String message) {
		File logFile = getLogFile(app);
		checkRotation(logFile);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
			String timestamp = DATE_FORMAT.format(new Date());
			writer.write(timestamp + " " + message);
			writer.newLine();
		} catch (IOException e) {
			// Fallback to Logcat if file writing fails
			android.util.Log.e("SpeedAlertLogger", "Failed to write to log file", e);
		}
	}

	public static File getLogFile(@NonNull Context context) {
		return new File(context.getFilesDir(), LOG_FILE_NAME);
	}

	public static File getBackupFile(@NonNull Context context) {
		return new File(context.getFilesDir(), LOG_FILE_BAK);
	}

	private static void checkRotation(File logFile) {
		if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
			File backupFile = new File(logFile.getParent(), LOG_FILE_BAK);
			if (backupFile.exists()) {
				backupFile.delete();
			}
			logFile.renameTo(backupFile);
		}
	}

	public static void clearLog(@NonNull OsmandApplication app) {
		File logFile = getLogFile(app);
		if (logFile.exists()) {
			logFile.delete();
		}
		File backupFile = getBackupFile(app);
		if (backupFile.exists()) {
			backupFile.delete();
		}
	}
}
