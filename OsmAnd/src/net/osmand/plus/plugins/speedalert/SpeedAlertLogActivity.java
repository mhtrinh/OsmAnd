package net.osmand.plus.plugins.speedalert;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.FileProvider;

import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SpeedAlertLogActivity extends ActionBarProgressActivity {

	private TextView logTextView;
	private ScrollView scrollView;
	private final Handler refreshHandler = new Handler(Looper.getMainLooper());
	private long lastModified = -1;
	private boolean autoScroll = true;

	private final Runnable refreshRunnable = new Runnable() {
		@Override
		public void run() {
			refreshLog();
			refreshHandler.postDelayed(this, 1000);
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		scrollView = new ScrollView(this);
		logTextView = new TextView(this);
		logTextView.setTypeface(Typeface.MONOSPACE);
		logTextView.setPadding(16, 16, 16, 16);
		scrollView.addView(logTextView);
		setContentView(scrollView);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.speed_alert_view_log);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
			// If user scrolled up, disable auto-scroll. If at bottom, enable.
			int diff = (scrollView.getChildAt(0).getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
			autoScroll = (diff <= 0);
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshLog();
		refreshHandler.postDelayed(refreshRunnable, 1000);
	}

	@Override
	protected void onPause() {
		super.onPause();
		refreshHandler.removeCallbacks(refreshRunnable);
	}

	private void refreshLog() {
		File logFile = SpeedAlertLogger.getLogFile(this);
		if (!logFile.exists()) {
			logTextView.setText(R.string.shared_string_none);
			return;
		}

		if (logFile.lastModified() == lastModified) {
			return;
		}
		lastModified = logFile.lastModified();

		StringBuilder content = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				content.append(line).append("\n");
			}
		} catch (IOException e) {
			content.append("Error reading log: ").append(e.getMessage());
		}

		logTextView.setText(content.toString());
		if (autoScroll) {
			scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.speed_alert_log_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		} else if (itemId == R.id.action_share) {
			shareLog();
			return true;
		} else if (itemId == R.id.action_clear) {
			SpeedAlertLogger.clearLog(app);
			refreshLog();
			Toast.makeText(this, R.string.speed_alert_log_cleared, Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void shareLog() {
		File logFile = SpeedAlertLogger.getLogFile(this);
		if (!logFile.exists()) return;

		Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_STREAM, uri);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(intent, getString(R.string.shared_string_export)));
	}
}
