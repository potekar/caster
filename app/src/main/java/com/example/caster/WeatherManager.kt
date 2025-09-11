package com.example.caster

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherManager(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val weatherApiService: WeatherApiService
    

    private val apiKey = "0fc88a83dab096dc12bca7831c5b70b6"
    
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        weatherApiService = retrofit.create(WeatherApiService::class.java)
    }
    
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getCurrentWeather(
        onSuccess: (WeatherResponse, Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onError("Location permission not granted")
            return
        }
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    fetchWeatherData(it.latitude, it.longitude, it, onSuccess, onError)
                } ?: onError("Could not get location")
            }
            .addOnFailureListener { exception ->
                onError("Error getting location: ${exception.message}")
            }
    }
    
    private fun fetchWeatherData(
        lat: Double,
        lon: Double,
        location: Location,
        onSuccess: (WeatherResponse, Location) -> Unit,
        onError: (String) -> Unit
    ) {
        val call = weatherApiService.getWeather(lat, lon, apiKey)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { weatherResponse ->
                        onSuccess(weatherResponse, location)
                    } ?: onError("Empty response")
                } else {
                    onError("Error: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
        })
    }
} 