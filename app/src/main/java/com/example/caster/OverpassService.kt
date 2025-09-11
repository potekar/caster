package com.example.caster

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>? = null
)

data class WaterFeature(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val description: String
)

class OverpassService {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    
    companion object {
        private const val OVERPASS_API_URL = "https://overpass-api.de/api/interpreter"
        private const val TAG = "OverpassService"
    }
    
    suspend fun getWaterFeatures(
        latitude: Double, 
        longitude: Double, 
        radiusKm: Double = 10.0
    ): List<WaterFeature> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching water features for lat: $latitude, lon: $longitude, radius: ${radiusKm}km")
            val query = buildOverpassQuery(latitude, longitude, radiusKm)
            Log.d(TAG, "Overpass query: $query")
            val response = makeRequest(query)
            Log.d(TAG, "Received response length: ${response.length}")
            val features = parseWaterFeatures(response)
            Log.d(TAG, "Parsed ${features.size} water features")
            features
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching water features: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun buildOverpassQuery(lat: Double, lon: Double, radiusKm: Double): String {
        val radiusMeters = (radiusKm * 1000).toInt()
        
        return """
            [out:json][timeout:25];
            (
              way["natural"="water"]["name"](around:$radiusMeters,$lat,$lon);
              way["waterway"="river"]["name"](around:$radiusMeters,$lat,$lon);
              way["waterway"="stream"]["name"](around:$radiusMeters,$lat,$lon);
              way["waterway"="canal"]["name"](around:$radiusMeters,$lat,$lon);
              way["leisure"="nature_reserve"]["natural"="water"](around:$radiusMeters,$lat,$lon);
              relation["natural"="water"]["name"](around:$radiusMeters,$lat,$lon);
              relation["waterway"="river"]["name"](around:$radiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()
    }
    
    private suspend fun makeRequest(query: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$OVERPASS_API_URL?data=${java.net.URLEncoder.encode(query, "UTF-8")}")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code: $response")
            }
            response.body?.string() ?: throw IOException("Empty response body")
        }
    }
    
    private fun parseWaterFeatures(jsonResponse: String): List<WaterFeature> {
        return try {
            val overpassResponse = gson.fromJson(jsonResponse, OverpassResponse::class.java)
            val waterFeatures = mutableListOf<WaterFeature>()
            
            for (element in overpassResponse.elements) {
                val tags = element.tags ?: continue
                val name = tags["name"] ?: continue
                
                // Get coordinates - for ways, use center; for nodes, use lat/lon directly
                val lat = element.lat ?: continue
                val lon = element.lon ?: continue
                
                // Determine water feature type
                val type = when {
                    tags["natural"] == "water" -> "Water Body"
                    tags["waterway"] == "river" -> "River"
                    tags["waterway"] == "stream" -> "Stream"
                    tags["waterway"] == "canal" -> "Canal"
                    else -> "Water Feature"
                }
                
                // Create description
                val description = buildDescription(tags, type)
                
                waterFeatures.add(
                    WaterFeature(
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        type = type,
                        description = description
                    )
                )
            }
            
            Log.d(TAG, "Found ${waterFeatures.size} water features")
            waterFeatures
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing water features: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun buildDescription(tags: Map<String, String>, type: String): String {
        val descriptions = mutableListOf<String>()
        
        // Add type-specific descriptions
        when (type) {
            "River" -> descriptions.add("Flowing waterway")
            "Stream" -> descriptions.add("Small flowing water")
            "Canal" -> descriptions.add("Artificial waterway")
            "Water Body" -> descriptions.add("Natural water feature")
        }
        
        // Add additional information from tags
        tags["amenity"]?.let { descriptions.add("Amenity: $it") }
        tags["leisure"]?.let { descriptions.add("Leisure: $it") }
        tags["tourism"]?.let { descriptions.add("Tourism: $it") }
        
        return descriptions.joinToString(", ").ifEmpty { "Local water feature" }
    }
}
