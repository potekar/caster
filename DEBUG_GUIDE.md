# Debug Guide - App Crash Fix

## Common Crash Causes and Solutions

### 1. **OpenStreetMap Initialization Issues**

**Symptoms:**
- App crashes immediately on startup
- "MapManager" or "OSMDroid" errors in logs

**Solutions:**
- ✅ Added proper permissions in AndroidManifest.xml
- ✅ Added error handling in MapManager
- ✅ Added `usesCleartextTraffic="true"` for HTTP tile loading

### 2. **Permission Issues**

**Symptoms:**
- App crashes when requesting location
- "Permission denied" errors

**Solutions:**
- ✅ Added all required permissions
- ✅ Added `requestLegacyExternalStorage="true"`
- ✅ Added proper permission handling

### 3. **Memory/Resource Issues**

**Symptoms:**
- App crashes after running for a while
- OutOfMemoryError in logs

**Solutions:**
- ✅ Added proper lifecycle management (onResume/onPause)
- ✅ Added error handling for all operations

## How to Debug

### 1. **Check Android Studio Logcat**

1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. Filter by your app package: `com.example.caster`
4. Look for red error messages

### 2. **Common Log Filters**

```
Tag: MainActivity
Tag: MapManager
Tag: AndroidRuntime
Level: Error
```

### 3. **Test Steps**

1. **Clean Build:**
   ```
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. **Install Fresh:**
   - Uninstall app from device
   - Install fresh build

3. **Check Permissions:**
   - Go to Settings → Apps → Your App → Permissions
   - Grant all location permissions

4. **Test on Different Device:**
   - Try on emulator vs real device
   - Try on different Android versions

## Quick Fixes to Try

### 1. **If Map Crashes:**
- Comment out map initialization temporarily
- Test if weather works without map

### 2. **If Location Crashes:**
- Add try-catch around location requests
- Test with mock location

### 3. **If UI Crashes:**
- Check if all views exist in layout
- Verify findViewById calls

## Emergency Fallback

If the app still crashes, try this simplified version:

1. **Remove Map Temporarily:**
   - Comment out map-related code
   - Test weather functionality only

2. **Add to MainActivity onCreate:**
   ```kotlin
   try {
       // Your existing code
   } catch (e: Exception) {
       Log.e("MainActivity", "Critical error: ${e.message}", e)
       Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
   }
   ```

## Report the Error

When reporting the crash, include:
1. **Error message** from Logcat
2. **Device info** (Android version, device model)
3. **Steps to reproduce** the crash
4. **When it happens** (startup, after permission, etc.)

This will help identify the exact cause and provide a targeted fix. 