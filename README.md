# Caster Weather App

A simple Android weather app that displays current weather information and nearby rivers based on your location.

## Features

- Current temperature display
- Weather description
- Humidity, wind speed, and pressure information
- Location-based weather data
- Refresh functionality
- OpenStreetMap showing nearby rivers
- Interactive map with user location and river markers
- No API keys required for maps

## Setup Instructions

### 1. Get OpenWeatherMap API Key

1. Go to [OpenWeatherMap](https://openweathermap.org/)
2. Sign up for a free account
3. Navigate to your API keys section
4. Copy your API key

### 2. Update API Key

**OpenWeatherMap API Key:**
Open `app/src/main/java/com/example/caster/WeatherManager.kt` and replace the API key:
```kotlin
private val apiKey = "your_openweathermap_api_key_here"
```

**Note:** OpenStreetMap is completely free and doesn't require any API key!

### 3. Build and Run

1. Sync your project with Gradle files
2. Build the project
3. Run the app on your device or emulator

## Permissions

The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - For precise location data
- `ACCESS_COARSE_LOCATION` - For approximate location data
- `INTERNET` - For fetching weather data and maps
- `WRITE_EXTERNAL_STORAGE` - For OpenStreetMap tile caching (Android < 10)

The app will request these permissions at runtime when you first launch it.

## How It Works

1. The app requests location permissions when first launched
2. Once permissions are granted, it gets your current location
3. It fetches weather data from OpenWeatherMap API using your coordinates
4. The weather information is displayed in a beautiful card layout
5. An OpenStreetMap shows your location and nearby rivers
6. You can refresh the weather data by tapping the "Refresh Weather" button

## Map Features

- **User Location**: Shows your current position with a marker
- **Nearby Rivers**: Displays sample river locations around your area
- **Interactive Map**: Zoom, pan, and explore the area
- **River Markers**: Click on markers to see river information
- **Free Maps**: Uses OpenStreetMap (no API key required)

## Dependencies

- **Retrofit**: For network requests to the weather API
- **Gson**: For JSON parsing
- **Google Play Services Location**: For location services
- **OpenStreetMap (OSMDroid)**: For free map display and river locations
- **AndroidX**: For modern Android components

## API Information

The app uses:
- **OpenWeatherMap API**: For weather data (temperature, humidity, wind, pressure)
- **OpenStreetMap**: For free map display and location services (no API key needed)

## Advantages of OpenStreetMap

- ✅ **Completely Free**: No API keys or usage limits
- ✅ **Open Source**: Community-driven map data
- ✅ **No Rate Limits**: Unlimited map usage
- ✅ **Privacy Friendly**: No tracking or data collection
- ✅ **Offline Support**: Can cache map tiles for offline use

## Troubleshooting

- **"Location permission not granted"**: Make sure to grant location permissions when prompted
- **"Error getting weather data"**: Check your internet connection and OpenWeatherMap API key
- **"Could not get location"**: Ensure location services are enabled on your device
- **Map not loading**: Check your internet connection (OpenStreetMap requires internet)

## Customization

You can customize the app by:
- Changing the gradient colors in `weather_gradient.xml`
- Modifying the layout in `activity_main.xml`
- Adding more weather information by extending the data models
- Implementing real river search using OSM Overpass API
- Changing the API endpoints or adding more weather services 
