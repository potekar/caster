package com.example.caster

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.library.R
import com.example.caster.R as AppR
import android.content.Intent
import android.net.Uri


class MapManager(private val context: Context) {
    
    private var mapView: MapView? = null
    private val overpassService = OverpassService()
    private val firebasePinManager = FirebasePinManager()
    private val waterFeatureMarkers = mutableListOf<Marker>()
    private val databasePinMarkers = mutableListOf<Marker>()
    private var pinsListenerJob: Job? = null
    private var selectionMarker: Marker? = null
    private var selectionOverlay: Overlay? = null
    
    fun initializeMap(mapView: MapView) {
        try {
            this.mapView = mapView
            setupMap()
            // Start listening to Firestore pins
            startPinsListener()
        } catch (e: Exception) {
            Log.e("MapManager", "Error initializing map: ${e.message}", e)
        }
    }
    
    private fun setupMap() {
        try {
            mapView?.let { map ->
                // Configure OSM
                Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                
                // Set tile source (map tiles)
                map.setTileSource(TileSourceFactory.MAPNIK)
                
                // Enable multi-touch controls
                map.setMultiTouchControls(true)
                
                // Set initial zoom level
                map.setMinZoomLevel(5.0)
                map.setMaxZoomLevel(19.0)
                
                Log.d("MapManager", "Map initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error setting up map: ${e.message}", e)
        }
    }
    
    fun showLocationOnMap(location: Location) {
        try {
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            
            mapView?.let { map ->
                // Move camera to user location
                map.controller.setZoom(12.0)
                map.controller.setCenter(geoPoint)
                
                // Clear only water feature markers, preserve user location and database pins
                clearWaterFeatureMarkers()
                
                // Add marker for user location
                val userMarker = Marker(map)
                userMarker.position = geoPoint
                userMarker.title = "Your Location"
                userMarker.snippet = "Current position"
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(userMarker)
                
                // Search for nearby rivers
                searchNearbyRivers(geoPoint)
                
                // Load pins from database
                loadDatabasePins()
                
                // Refresh the map
                map.invalidate()
                
                Log.d("MapManager", "Location shown on map: ${location.latitude}, ${location.longitude}")
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error showing location on map: ${e.message}", e)
        }
    }
    
    private fun searchNearbyRivers(userLocation: GeoPoint) {
        try {
            // Fetch real water features from OpenStreetMap
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val waterFeatures = overpassService.getWaterFeatures(
                        latitude = userLocation.latitude,
                        longitude = userLocation.longitude,
                        radiusKm = 15.0 // Search within 15km radius
                    )
                    
                    if (waterFeatures.isNotEmpty()) {
                        addRealWaterFeatures(waterFeatures)
                    } else {
                        Log.d("MapManager", "No water features found in the area")
                    }
                } catch (e: Exception) {
                    Log.e("MapManager", "Error fetching real water features: ${e.message}", e)
                    // Fallback to sample data if API fails
                }
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error searching for water features: ${e.message}", e)
        }
    }
    
    private fun addRealWaterFeatures(waterFeatures: List<WaterFeature>) {
        try {
            mapView?.let { map ->
                // Clear existing water feature markers
                clearWaterFeatureMarkers()
                
                for (feature in waterFeatures) {
                    val geoPoint = GeoPoint(feature.latitude, feature.longitude)
                    val marker = Marker(map)
                    marker.position = geoPoint
                    marker.title = feature.name
                    marker.snippet = "${feature.type}: ${feature.description}"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map.overlays.add(marker)
                    waterFeatureMarkers.add(marker)
                }
                
                // Refresh the map to show new markers
                map.invalidate()
                Log.d("MapManager", "Added ${waterFeatures.size} real water feature markers")
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error adding real water feature markers: ${e.message}", e)
        }
    }
    
    private fun clearWaterFeatureMarkers() {
        try {
            mapView?.let { map ->
                // Remove water feature markers from map
                for (marker in waterFeatureMarkers) {
                    map.overlays.remove(marker)
                }
                waterFeatureMarkers.clear()
                Log.d("MapManager", "Cleared water feature markers")
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error clearing water feature markers: ${e.message}", e)
        }
    }
    
    private fun loadDatabasePins() {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Clear existing database pin markers
                    clearDatabasePinMarkers()
                    
                    // Get all public pins from database
                    firebasePinManager.fetchPublicPins(
                        onSuccess = { pins ->
                            Log.d("MapManager", "Loaded ${pins.size} pins from database")
                            addDatabasePinsToMap(pins)
                        },
                        onError = { error ->
                            Log.e("MapManager", "Error loading database pins: $error")
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MapManager", "Exception loading database pins: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error in loadDatabasePins: ${e.message}", e)
        }
    }
    
    private fun addDatabasePinsToMap(pins: List<MapPin>) {
        try {
            mapView?.let { map ->
                // Clear existing markers to avoid duplicates when flow updates
                clearDatabasePinMarkers()
                for (pin in pins) {
                    val geoPoint = GeoPoint(pin.latitude, pin.longitude)
                    val marker = Marker(map)
                    val pinIcon = ContextCompat.getDrawable(map.context, R.drawable.marker_default)!!.mutate()

                    val filter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                    pinIcon.colorFilter = filter

                    marker.icon = pinIcon
                    marker.position = geoPoint
                    marker.title = pin.title
                    marker.snippet = pin.description
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { m, _ ->
                        try {
                            showPinDetailsDialog(pin)
                        } catch (_: Exception) {}
                        true
                    }
                    map.overlays.add(marker)
                    databasePinMarkers.add(marker)
                }
                
                // Refresh the map to show new markers
                map.invalidate()
                Log.d("MapManager", "Added ${pins.size} database pin markers")
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error adding database pin markers: ${e.message}", e)
        }
    }

    private fun showPinDetailsDialog(pin: MapPin) {
        try {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(AppR.layout.dialog_pin_details, null)
            val titleView = view.findViewById<TextView>(AppR.id.pinTitle)
            val descView = view.findViewById<TextView>(AppR.id.pinDescription)
            val imageView = view.findViewById<ImageView>(AppR.id.pinImage)

            titleView.text = pin.title
            descView.text = pin.description

            if (!pin.imageUrl.isNullOrEmpty()) {
                try {
                    // Use Glide if available
                    Class.forName("com.bumptech.glide.Glide")
                    val glideClass = com.bumptech.glide.Glide::class.java
                    val glide = com.bumptech.glide.Glide.with(context)
                    glide.load(pin.imageUrl)
                        .centerCrop()
                        .into(imageView)
                    imageView.visibility = android.view.View.VISIBLE
                } catch (_: Throwable) {
                    // If Glide isn't present, hide the image view gracefully
                    imageView.visibility = android.view.View.GONE
                }
            } else {
                imageView.visibility = android.view.View.GONE
            }

            AlertDialog.Builder(context)
                .setView(view)
                .setNegativeButton("Open in Maps") { _, _ ->
                    try {
                        val label = Uri.encode(pin.title ?: "Location")
                        val geoUri = Uri.parse("geo:${pin.latitude},${pin.longitude}?q=${pin.latitude},${pin.longitude}($label)")
                        val intent = Intent(Intent.ACTION_VIEW, geoUri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${pin.latitude},${pin.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        } catch (_: Exception) {}
                    }
                }
                .setPositiveButton("Close", null)
                .show()
        } catch (e: Exception) {
            Log.e("MapManager", "Error showing pin details dialog: ${e.message}", e)
        }
    }
    
    private fun clearDatabasePinMarkers() {
        try {
            mapView?.let { map ->
                // Remove database pin markers from map
                for (marker in databasePinMarkers) {
                    map.overlays.remove(marker)
                }
                databasePinMarkers.clear()
                Log.d("MapManager", "Cleared database pin markers")
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error clearing database pin markers: ${e.message}", e)
        }
    }

    fun enableTapSelection(enabled: Boolean, onSelected: ((lat: Double, lon: Double) -> Unit)? = null) {
        try {
            mapView?.let { map ->
                if (enabled) {
                    // Remove any existing overlay before adding a new one
                    selectionOverlay?.let { map.overlays.remove(it) }
                    val overlay = object : Overlay() {
                        override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                            if (e == null || mapView == null) return false
                            val proj = mapView.projection
                            val geoPoint = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                            showSelectionMarker(geoPoint)
                            onSelected?.invoke(geoPoint.latitude, geoPoint.longitude)
                            return true
                        }
                    }
                    selectionOverlay = overlay
                    map.overlays.add(overlay)
                    Log.d("MapManager", "Tap selection enabled")
                } else {
                    // Remove selection marker and overlays
                    selectionMarker?.let { map.overlays.remove(it) }
                    selectionMarker = null
                    selectionOverlay?.let { map.overlays.remove(it) }
                    selectionOverlay = null
                    Log.d("MapManager", "Tap selection disabled")
                }
                map.invalidate()
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error toggling tap selection: ${e.message}", e)
        }
    }

    private fun showSelectionMarker(point: GeoPoint) {
        try {
            mapView?.let { map ->
                // Remove previous selection marker
                selectionMarker?.let { map.overlays.remove(it) }
                val marker = Marker(map)
                marker.position = point
                marker.title = "Selected Location"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Use a distinct color (e.g., green) for selection marker
                try {
                    val icon = ContextCompat.getDrawable(map.context, org.osmdroid.library.R.drawable.marker_default)!!.mutate()
                    icon.setTint(Color.GREEN)
                    marker.icon = icon
                } catch (_: Exception) {}
                map.overlays.add(marker)
                selectionMarker = marker
                map.invalidate()
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error showing selection marker: ${e.message}", e)
        }
    }

    
    fun onResume() {
        try {
            mapView?.onResume()
            // Ensure listener is active
            if (pinsListenerJob == null) {
                startPinsListener()
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error in onResume: ${e.message}", e)
        }
    }
    
    fun onPause() {
        try {
            mapView?.onPause()
            // Stop Firestore listener to avoid leaks
            pinsListenerJob?.cancel()
            pinsListenerJob = null
        } catch (e: Exception) {
            Log.e("MapManager", "Error in onPause: ${e.message}", e)
        }
    }

    private fun startPinsListener() {
        try {
            pinsListenerJob?.cancel()
            pinsListenerJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d("MapManager", "Starting Firestore pins listener...")
                    firebasePinManager.getPinsFlow().collectLatest { pins ->
                        Log.d("MapManager", "Pins flow emitted ${pins.size} items")
                        // Update markers on main thread
                        addDatabasePinsToMap(pins)
                    }
                } catch (e: CancellationException) {
                    Log.d("MapManager", "Pins listener cancelled")
                } catch (e: Exception) {
                    Log.e("MapManager", "Error collecting pins flow: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MapManager", "Error starting pins listener: ${e.message}", e)
        }
    }
} 