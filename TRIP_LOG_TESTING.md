# Trip Log Feature - Testing Guide

## How to Enable and Use Trip Logging

### Step 1: Enable Trip Logging
1. Open the DashCam app
2. Tap the Settings button (gear icon)
3. Find "Trip Log" in the settings list
4. **Toggle the switch ON** (this is required - it's OFF by default)
5. Tap anywhere on the "Trip Log" row to view existing logs (will be empty initially)

### Step 2: Grant Location Permission
- The app needs location permission to record GPS coordinates
- If not already granted, you'll see a toast message when recording starts
- Grant "Allow all the time" or "Allow while using the app" for best results

### Step 3: Record a Video
1. Go back to the main screen
2. Tap the record button to start recording
3. You should see a toast: "Trip logging started"
4. Record for at least 10-15 seconds (logs are written every 5 seconds)
5. Tap the stop button

### Step 4: View Trip Logs
1. Go to Settings
2. Tap on "Trip Log" (not just the toggle, tap the whole row)
3. You'll see a list of your trip log files
4. Tap any log file to view its contents
5. Use the share button to export the log
6. Use the delete button to remove a log

## Log File Location
- Logs are stored in: `/Android/data/com.kasahirotech.dashcamapp/files/Documents/TripLogs/`
- File format: `trip_log_YYYYMMDD_HHMMSS.txt`

## Log File Format
```
Trip Log
Start Time: 2025-11-20 14:30:15
Format Version: 1.0
---
Timestamp,Latitude,Longitude,Speed (mph)
2025-11-20 14:30:20,37.774900,-122.419400,25.3
2025-11-20 14:30:25,37.775000,-122.419500,26.1
...
```

## Troubleshooting

### No logs are created
1. **Check if trip logging is enabled**: Go to Settings and make sure the "Trip Log" toggle is ON
2. **Check location permission**: Make sure the app has location permission
3. **Check logcat**: Look for messages with tag "TripLogger" or "MainActivity"
   - Should see: "Trip logging started successfully!"
   - Should see: "Log file: /path/to/file"

### Logs are empty
- GPS might not have a fix yet
- Try recording outdoors or near a window
- Wait 10-15 seconds for GPS to acquire satellites
- Check logcat for "Skipping log entry: GPS data unavailable"

### Can't see logs in viewer
- Make sure you recorded with trip logging enabled
- Check the file path in logcat
- Verify files exist in the TripLogs directory

## Debug Commands

View logs in real-time:
```bash
adb logcat | grep -E "TripLogger|MainActivity"
```

Check if files were created:
```bash
adb shell ls -la /sdcard/Android/data/com.kasahirotech.dashcamapp/files/Documents/TripLogs/
```

Pull a log file to your computer:
```bash
adb pull /sdcard/Android/data/com.kasahirotech.dashcamapp/files/Documents/TripLogs/trip_log_20251120_143015.txt
```
