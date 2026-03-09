# SpeedAlert Plugin Specification

## Overview
The SpeedAlert plugin is designed to enhance driver safety by monitoring the vehicle's current speed and providing alerts when the speed exceeds the speed limit of the current road.

## Core Features
1.  **Speed Monitoring**: Continuously tracks the user's current speed using GPS.
2.  **Speed Limit Detection**: Retrieves speed limit information from OpenStreetMap (OSM) data via the navigation engine.
3.  **Fallback Mechanism**: Allows users to set a default speed limit when OSM data is unavailable.
4.  **Customizable Thresholds**: Users can define a "grace" speed (threshold) before an alert is triggered.
5.  **Multi-Modal Alerts**: Supports audio, haptic (vibration), and visual (toast) feedback for speed violations.
6.  **Speed Limit Change Notification**: Optionally displays a toast message when a new speed limit is detected.
7.  **Profile-Specific Settings**: Settings can be configured differently for various application modes (e.g., Car, Bicycle, Truck).
8.  **Background Operation**: Integrates with the navigation service to continue monitoring even when the app is in the background.

## Behavior and Logic

### 1. Speed Limit Determination
- **Route Data**: The plugin attempts to get the speed limit from the current `RouteDataObject`. It considers the direction of travel relative to the route segment.
- **Fallback**: If no speed limit is found in the route data (or no route is active), it uses the user-defined `SPEED_ALERT_FALLBACK_KMH` setting.

### 2. Alert Trigger Logic
An alert is triggered if:
`Current Speed > (Detected Speed Limit + User Threshold)`

- **Threshold**: Configured via `SPEED_ALERT_THRESHOLD_KMH`.
- **Interval**: To prevent constant alerting, subsequent alerts are only fired after a user-defined `SPEED_ALERT_INTERVAL_S`.
- **Reset**: The interval timer is reset if the current speed drops below the detected speed limit.

### 3. Feedback Mechanisms

#### Audio (Sound Mode)
- **Speaker**: Plays the alert sound using the `ALARM` audio stream, ensuring it is heard even if other media is playing or muted.
- **Adaptive**: Checks if music or other media is currently active. If so, it uses the `NOTIFICATION` stream to be less intrusive; otherwise, it uses the `ALARM` stream.
- **Off**: No audio alert.
- **Sound Asset**: Uses `sounds/ding.ogg`.

#### Haptic (Vibration Mode)
- **Pattern 1**: A single 800ms vibration.
- **Pattern 2**: Two pulses (600ms on, 100ms off, 200ms on).
- **Off**: No vibration.

#### Visual
- **Toast**: An optional on-screen message showing the current speed and the limit (e.g., "Speed Alert: 85 / 70 km/h").
- **Notification**: A persistent system notification is shown while monitoring is active, providing a "Stop" action button.

### 4. Lifecycle and Control
- **Auto-Start**: If enabled for the current profile, monitoring starts automatically when the Map activity resumes.
- **Manual Control**: Users can start or stop monitoring manually via:
    - The Plugin settings screen.
    - A dedicated Quick Action button on the map.
    - The "Stop" button in the system notification.
- **Navigation Service**: The plugin requests the `USED_BY_SPEED_ALERT` flag from the `NavigationService` to ensure the GPS remains active and the process is not killed by the OS.

## Configuration Settings

| Setting | Type | Description | Values |
| :--- | :--- | :--- | :--- |
| **Enabled** | Boolean | Global toggle for the plugin. | On / Off |
| **Threshold** | List | Speed above limit to trigger alert. | 0, 1, 2, 5, 10, 15, 20 km/h |
| **Fallback Limit** | List | Default limit when OSM data is missing. | 30 to 120 km/h |
| **Alert Interval** | List | Minimum time between alerts. | 0 (None) to 90 seconds |
| **Sound Mode** | List | Audio feedback behavior. | Adaptive, Speaker, Off |
| **Vibrate Mode** | List | Haptic feedback behavior. | Pattern 1, Pattern 2, Off |
| **Show Toast** | Boolean | Display on-screen alert message. | On / Off |
| **Toast on Limit Change** | Boolean | Show toast when speed limit changes. | On / Off (Default: On) |
| **Verbose Log** | Boolean | Enable detailed logging for debugging. | On / Off |
| **Log Period** | List | Frequency of verbose log entries. | 1 to 60 seconds |

## Integration Points

### Quick Action
- **Type**: `SpeedAlertToggleAction`
- **Behavior**: Toggles monitoring on/off. Displays different text/icon state based on whether monitoring is currently active.

### System Notification
- **Title**: Speed Alert Active
- **Text**: Monitors speed limits
- **Action**: "Stop" button to deactivate monitoring.

### Technical Details for Implementation
- **Core Class**: `SpeedAlertPlugin` (inherits from `OsmandPlugin`).
- **Update Hook**: Overrides `updateLocation(Location location)` to perform speed checks on every GPS update.
- **Media Management**: Uses `SoundPool` for low-latency audio playback.
- **Permissions**: Requires `VIBRATE` permission for haptic feedback.
- **Logging**: Uses `PlatformUtil.getLog()` for diagnostic output.
