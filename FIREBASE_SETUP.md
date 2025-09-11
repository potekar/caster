# Firebase Firestore Setup Guide for Pin Sharing

## Overview
This app now includes a sliding panel that allows users to add pins to the map that are shared with other users via **Firebase Firestore** (instead of Realtime Database).

## Features Added
- ✅ **Sliding Panel**: Swipe from right to open pin creation form
- ✅ **Floating Action Button**: Tap to open pin panel

- ✅ **Firestore Integration**: Pins are stored and shared in real-time
- ✅ **Real-time Updates**: All users see pins added by others
- ✅ **Advanced Queries**: Search by location and more

## Why Firestore Instead of Realtime Database?

### **Advantages of Firestore:**
- 🔥 **Better Querying**: Complex queries with multiple conditions
- 📱 **Offline Support**: Works without internet connection
- 🚀 **Scalability**: Better performance with large datasets
- 🔍 **Advanced Search**: Location-based queries
- 📊 **Better Data Structure**: Document-based with collections

## Firebase Setup Required

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Enter project name: `caster-weather-app`
4. Follow setup wizard

### 2. Enable Firestore Database
1. In Firebase Console, go to **"Firestore Database"**
2. Click "Create Database"
3. Choose **"Start in test mode"** (for development)
4. Select location (closest to your users)
5. Click "Done"

### 3. Get Configuration File
1. In Firebase Console, go to Project Settings
2. Click "Add app" → Android
3. Enter package name: `com.example.caster`
4. Download `google-services.json`
5. Replace the placeholder file in `app/google-services.json`

### 4. Update Project-level build.gradle
Add to your project-level `build.gradle.kts`:
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

## How to Use

### Adding a Pin
1. **Tap the + button** (floating action button)
2. **Fill the form**:
   - Title (required)
   - Description (required)
3. **Tap "Add Pin to Map"**
4. **Pin appears** on map for all users

### Viewing Pins
- Pins automatically appear on the map
- Tap any pin to see title and description
- Pins are shared in real-time across all users

## Firestore Database Structure
```json
Collection: "map_pins"
Documents: Auto-generated IDs
Fields:
{
  "title": "Beautiful River",
  "description": "Great spot for fishing",
  "latitude": 45.1234,
  "longitude": -75.5678,

  "createdBy": "User",
  "createdAt": 1703123456789,
  "isPublic": true
}
```

## Security Rules
For development, use these rules in Firestore Console:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /map_pins/{document} {
      allow read, write: if true;
    }
  }
}
```

## Advanced Features Available

### 1. **Location-based Search**
```kotlin
firebasePinManager.searchPinsNearLocation(lat, lon, 5.0) { pins, error ->
    // Find pins within 5km radius
}
```

### 2. **Real-time Updates**
```kotlin
firebasePinManager.getPinsFlow().collect { pins ->
    // Pins update automatically
}
```

## Troubleshooting

### App Crashes on Startup
- Check if `google-services.json` is properly placed
- Verify Firebase project is created
- Check internet connection

### Pins Not Appearing
- Check Firebase Console for Firestore errors
- Verify security rules allow read/write
- Check internet connection

### Panel Not Sliding
- Ensure all views are properly initialized
- Check for layout errors in logcat

## Next Steps
1. **Set up Firebase project** (follow steps above)
2. **Enable Firestore Database** (not Realtime Database)
3. **Replace google-services.json** with your actual file
4. **Test pin creation** and sharing

5. **Add user authentication** for better security

## Advanced Features (Future)
- User accounts and authentication
- Pin moderation and approval
- Pin ratings and comments
- Offline pin caching
- Pin search and filtering
- Location-based recommendations
- Pin analytics and insights

## Migration from Realtime Database
If you were using Realtime Database before:
1. **Export data** from Realtime Database
2. **Import to Firestore** using Firebase Console
3. **Update security rules** for Firestore
4. **Test functionality** with new Firestore implementation 