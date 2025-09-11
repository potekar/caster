package com.example.caster

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebasePinManager {
    
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val pinsCollection = firestore.collection("map_pins")
    
    fun addPin(pin: MapPin, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            // Remove the id field as Firestore will auto-generate it
            val pinData = mapOf(
                "title" to pin.title,
                "description" to pin.description,
                "latitude" to pin.latitude,
                "longitude" to pin.longitude,
                "createdBy" to pin.createdBy,
                "createdAt" to pin.createdAt,
                "isPublic" to pin.isPublic,
                "imageUrl" to pin.imageUrl
            )
            
            pinsCollection.add(pinData)
                .addOnSuccessListener { documentReference ->
                    Log.d("FirebasePinManager", "Pin added successfully with ID: ${documentReference.id}")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebasePinManager", "Error adding pin: ${exception.message}")
                    onError("Failed to add pin: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebasePinManager", "Exception adding pin: ${e.message}")
            onError("Exception: ${e.message}")
        }
    }
    
    fun fetchPublicPins(onSuccess: (List<MapPin>) -> Unit, onError: (String) -> Unit) {
        try {
            pinsCollection
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val pins = mutableListOf<MapPin>()
                    for (document in documents) {
                        val pin = document.toObject<MapPin>()
                        pin?.let { 
                            // Set the document ID
                            val pinWithId = it.copy(id = document.id)
                            pins.add(pinWithId)
                        }
                    }
                    Log.d("FirebasePinManager", "Fetched ${pins.size} public pins")
                    onSuccess(pins)
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebasePinManager", "Error fetching pins: ${exception.message}")
                    onError("Failed to fetch pins: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebasePinManager", "Exception fetching pins: ${e.message}")
            onError("Exception: ${e.message}")
        }
    }
    
    fun deletePin(pinId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            pinsCollection.document(pinId).delete()
                .addOnSuccessListener {
                    Log.d("FirebasePinManager", "Pin deleted successfully: $pinId")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebasePinManager", "Error deleting pin: ${exception.message}")
                    onError("Failed to delete pin: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebasePinManager", "Exception deleting pin: ${e.message}")
            onError("Exception: ${e.message}")
        }
    }
    
    fun getPinsFlow(): Flow<List<MapPin>> = callbackFlow {
        val listener = pinsCollection
            .whereEqualTo("isPublic", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebasePinManager", "Error in pins flow: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                val pins = mutableListOf<MapPin>()
                snapshot?.documents?.forEach { document ->
                    val pin = document.toObject<MapPin>()
                    pin?.let { 
                        val pinWithId = it.copy(id = document.id)
                        pins.add(pinWithId)
                    }
                }
                
                trySend(pins)
            }
        
        awaitClose { listener.remove() }
    }


}