package com.example.caster

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import android.widget.CheckBox
import android.widget.ImageView
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {
    
    private lateinit var weatherManager: WeatherManager
    private lateinit var mapManager: MapManager
    private lateinit var locationText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var weatherDescriptionText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windSpeedText: TextView
    private lateinit var pressureText: TextView
    private lateinit var refreshButton: Button
    private lateinit var loadingProgress: ProgressBar
    private var mapView: MapView? = null
    
    // Panel and gesture handling
    private lateinit var slidingPanel: CardView
    private lateinit var addPinFab: FloatingActionButton
    private lateinit var closePanelButton: ImageButton
    private lateinit var addPinButton: Button
    private lateinit var pinTitleInput: TextInputEditText
    private lateinit var pinDescriptionInput: TextInputEditText
    private lateinit var manualPlacementCheckbox: CheckBox
    private lateinit var selectPhotoButton: Button
    private lateinit var photoPreview: ImageView
    private lateinit var gestureDetector: GestureDetector
    private var isPanelOpen = false
    
    // Firebase pin manager
    private lateinit var firebasePinManager: FirebasePinManager
    // Manual placement state
    private var selectedMapLat: Double? = null
    private var selectedMapLon: Double? = null
    private var selectedImageUri: Uri? = null
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        photoPreview.setImageURI(uri)
    }
    
    private var currentLocation: Location? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        applyImmersiveMode()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        try {
            AuthManager.signInAnonymouslyIfNeeded(
                onReady = { },
                onError = { }
            )
            initializeViews()
            initializeGestureDetector()
            initializePanel()
            mapManager = MapManager(this)
            initializeMap()
            
            weatherManager = WeatherManager(this)
            firebasePinManager = FirebasePinManager()
            
            // Check permissions and load weather
            Log.d("MainActivity", "Checking location permission...")
            if (checkLocationPermission()) {
                Log.d("MainActivity", "Location permission already granted")
                loadWeather()
            } else {
                Log.d("MainActivity", "Requesting location permission...")
                requestLocationPermission()
            }
            
            refreshButton.setOnClickListener {
                if (checkLocationPermission()) {
                    loadWeather()
                } else {
                    requestLocationPermission()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }
    
    private fun initializeViews() {
        try {
            locationText = findViewById(R.id.locationText)
            temperatureText = findViewById(R.id.temperatureText)
            weatherDescriptionText = findViewById(R.id.weatherDescriptionText)
            humidityText = findViewById(R.id.humidityText)
            windSpeedText = findViewById(R.id.windSpeedText)
            pressureText = findViewById(R.id.pressureText)
            refreshButton = findViewById(R.id.refreshButton)
            loadingProgress = findViewById(R.id.loadingProgress)
            mapView = findViewById(R.id.mapView)
            
            // Initialize panel views
            slidingPanel = findViewById(R.id.slidingPanel)
            addPinFab = findViewById(R.id.addPinFab)
            closePanelButton = findViewById(R.id.closePanelButton)
            addPinButton = findViewById(R.id.addPinButton)
            pinTitleInput = findViewById(R.id.pinTitleInput)
            pinDescriptionInput = findViewById(R.id.pinDescriptionInput)
            manualPlacementCheckbox = findViewById(R.id.manualPlacementCheckbox)
            selectPhotoButton = findViewById(R.id.selectPhotoButton)
            photoPreview = findViewById(R.id.photoPreview)

            // Bottom nav
            findViewById<BottomNavigationView>(R.id.bottomNav)?.apply {
                selectedItemId = R.id.nav_home
                setOnItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_feed -> {
                            val intent = Intent(this@MainActivity, FeedActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                            true
                        }
                        R.id.nav_home -> true
                        R.id.nav_inventory -> {
                            val intent = Intent(this@MainActivity, InventoryActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                            true
                        }
                        else -> false
                    }
                }
            }
            
            if (mapView == null) {
                Log.e("MainActivity", "MapView is null! Check your layout file.")
                Toast.makeText(this, "MapView is missing in layout!", Toast.LENGTH_LONG).show()
            } else {
                Log.d("MainActivity", "MapView initialized successfully")
            }
            Log.d("MainActivity", "Views initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing views: ${e.message}", e)
            throw e
        }
    }
    
    private fun initializeGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)
                
                // Check if it's a horizontal swipe
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) && 
                    kotlin.math.abs(velocityX) > 500) {
                    // Swipe from right to left (opening panel)
                    if (diffX < -50 && e1?.x ?: 0f > resources.displayMetrics.widthPixels - 150) {
                        if (!isPanelOpen) {
                            openPanel()
                            return true
                        }
                    }
                    // Swipe from left to right (closing panel)
                    else if (diffX > 50) {
                        if (isPanelOpen) {
                            closePanel()
                            return true
                        } else {
                            startActivity(Intent(this@MainActivity, FeedActivity::class.java))
                            return true
                        }
                    }
                }
                return false
            }
        })
    }
    
    private fun initializePanel() {
        // Set initial panel state (closed) - use post to ensure layout is measured
        slidingPanel.post {
            slidingPanel.translationX = slidingPanel.width.toFloat()
        }
        isPanelOpen = false
        
        // Set up click listeners
        addPinFab.setOnClickListener {
            if (isPanelOpen) {
                closePanel()
            } else {
                openPanel()
            }
        }
        
        closePanelButton.setOnClickListener {
            closePanel()
        }
        
        addPinButton.setOnClickListener {
            addPinToMap()
        }

        manualPlacementCheckbox.setOnCheckedChangeListener { _, isChecked ->
            try {
                mapManager.enableTapSelection(isChecked) { lat, lon ->
                    selectedMapLat = lat
                    selectedMapLon = lon
                    findViewById<TextView>(R.id.currentLocationText).text = "Selected: ${String.format("%.5f", lat)}, ${String.format("%.5f", lon)}"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error toggling manual placement: ${e.message}", e)
                Toast.makeText(this, "Failed to enable manual placement", Toast.LENGTH_SHORT).show()
            }
        }

        selectPhotoButton.setOnClickListener {
            try {
                imagePicker.launch("image/*")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching image picker: ${e.message}", e)
                Toast.makeText(this, "Unable to open gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openPanel() {
        if (!isPanelOpen) {
            slidingPanel.animate()
                .translationX(0f)
                .setDuration(300)
                .withStartAction {
                    Log.d("MainActivity", "Starting panel open animation")
                }
                .withEndAction {
                    Log.d("MainActivity", "Panel opened successfully")
                }
                .start()
            isPanelOpen = true
        }
    }
    
    private fun closePanel() {
        if (isPanelOpen) {
            slidingPanel.animate()
                .translationX(slidingPanel.width.toFloat())
                .setDuration(300)
                .withStartAction {
                    Log.d("MainActivity", "Starting panel close animation")
                }
                .withEndAction {
                    Log.d("MainActivity", "Panel closed successfully")
                }
                .start()
            isPanelOpen = false
        }
    }
    
    private fun addPinToMap() {
        try {
            val title = pinTitleInput.text?.toString()?.trim()
            val description = pinDescriptionInput.text?.toString()?.trim()
            
            // Validate input
            if (title.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a title for the pin", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (description.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a description for the pin", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Determine location source: manual selection or current location
            val useSelected = manualPlacementCheckbox.isChecked && selectedMapLat != null && selectedMapLon != null
            val pinLat = if (useSelected) selectedMapLat!! else currentLocation?.latitude
            val pinLon = if (useSelected) selectedMapLon!! else currentLocation?.longitude
            if (pinLat == null || pinLon == null) {
                Toast.makeText(this, "Location not available. Select on map or wait for GPS.", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Prepare base pin (imageUrl assigned after upload if needed)
            var pendingPin = MapPin(
                title = title,
                description = description,
                latitude = pinLat,
                longitude = pinLon,
                createdBy = "User",
                createdAt = System.currentTimeMillis(),
                isPublic = true,
                imageUrl = null
            )

            // Show loading
            addPinButton.isEnabled = false
            addPinButton.text = "Adding..."

            val uri = selectedImageUri
            if (uri != null) {
                try {
                    val file = uriToTempFile(uri)
                    ImgbbUploader.upload(
                        file = file,
                        apiKey = BuildConfig.IMGBB_API_KEY,
                        onSuccess = { link ->
                            pendingPin = pendingPin.copy(imageUrl = link)
                            savePinToFirestore(pendingPin)
                            file.delete()
                        },
                        onError = { ex ->
                            Log.e("MainActivity", "IMGBB upload failed: ${ex.message}")
                            savePinToFirestore(pendingPin)
                            file.delete()
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to prepare image for IMGBB: ${e.message}")
                    savePinToFirestore(pendingPin)
                }
            } else {
                savePinToFirestore(pendingPin)
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception adding pin: ${e.message}", e)
            Toast.makeText(this, "Error adding pin: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // Reset button
            addPinButton.isEnabled = true
            addPinButton.text = "Add Pin to Map"
        }
    }

    private fun savePinToFirestore(pin: MapPin) {
        firebasePinManager.addPin(
            pin = pin,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Pin added successfully!", Toast.LENGTH_SHORT).show()

                    // Clear form
                    pinTitleInput.text?.clear()
                    pinDescriptionInput.text?.clear()
                    selectedImageUri = null
                    photoPreview.setImageDrawable(null)

                    // Disable manual placement after adding
                    manualPlacementCheckbox.isChecked = false
                    mapManager.enableTapSelection(false)
                    selectedMapLat = null
                    selectedMapLon = null
                    findViewById<TextView>(R.id.currentLocationText).text = "Current location will be used"

                    // Reset button
                    addPinButton.isEnabled = true
                    addPinButton.text = "Add Pin to Map"

                    // Close panel
                    closePanel()

                    Log.d("MainActivity", "Pin added successfully")
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to add pin: $error", Toast.LENGTH_LONG).show()

                    // Reset button
                    addPinButton.isEnabled = true
                    addPinButton.text = "Add Pin to Map"

                    Log.e("MainActivity", "Error adding pin: $error")
                }
            }
        )
    }

    private fun uriToTempFile(uri: Uri): java.io.File {
        val inputStream = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open URI")
        val tempFile = java.io.File.createTempFile("pin_", ".img", cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        return tempFile
    }
    
    private fun initializeMap() {
        try {
            mapManager.initializeMap(mapView!!)
            Log.d("MainActivity", "Map initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing map: ${e.message}", e)
            Toast.makeText(this, "Error initializing map", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            mapManager.onResume()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume: ${e.message}", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            mapManager.onPause()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onPause: ${e.message}", e)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle tap outside panel to close it
        if (event.action == MotionEvent.ACTION_DOWN && isPanelOpen) {
            val x = event.x
            val y = event.y
            
            // Check if tap is outside the panel area
            if (x < slidingPanel.x || x > slidingPanel.x + slidingPanel.width ||
                y < slidingPanel.y || y > slidingPanel.y + slidingPanel.height) {
                closePanel()
                return true
            }
        }
        
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun loadWeather() {
        showLoading(true)
        
        weatherManager.getCurrentWeather(
            onSuccess = { weatherResponse, location ->
                runOnUiThread {
                    try {
                        currentLocation = location
                        updateWeatherUI(weatherResponse)
                        showLoading(false)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error updating UI: ${e.message}", e)
                        showLoading(false)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Log.e("MainActivity", "Weather error: $error")
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            }
        )
    }
    
    private fun updateWeatherUI(weatherResponse: WeatherResponse) {
        try {
            locationText.text = weatherResponse.name
            temperatureText.text = "${weatherResponse.main.temp.toInt()}°C"
            weatherDescriptionText.text = weatherResponse.weather.firstOrNull()?.description ?: ""
            humidityText.text = "${weatherResponse.main.humidity}%"
            windSpeedText.text = "${weatherResponse.wind.speed.toInt()} km/h"
            pressureText.text = "${weatherResponse.main.pressure} hPa"
            
            // Update map with location
            currentLocation?.let { location ->
                mapManager.showLocationOnMap(location)
            }
            
            Log.d("MainActivity", "Weather UI updated successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating weather UI: ${e.message}", e)
        }
    }
    
    private fun showLoading(show: Boolean) {
        try {
            loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
            refreshButton.isEnabled = !show
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing loading: ${e.message}", e)
        }
    }
    
    private fun checkLocationPermission(): Boolean {
        try {
            val fineLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val coarseLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            Log.d("MainActivity", "Fine location permission: $fineLocation")
            Log.d("MainActivity", "Coarse location permission: $coarseLocation")
            
            return fineLocation || coarseLocation
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions: ${e.message}", e)
            return false
        }
    }
    
    private fun requestLocationPermission() {
        try {
            Log.d("MainActivity", "Requesting location permissions...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting permissions: ${e.message}", e)
        }
    }
    
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        Log.d("MainActivity", "Permission result received: requestCode=$requestCode")
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Location permission granted")
                    loadWeather()
                } else {
                    Log.d("MainActivity", "Location permission denied")
                    Toast.makeText(
                        this,
                        getString(R.string.location_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}