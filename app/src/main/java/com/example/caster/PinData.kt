package com.example.caster

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.*

data class MapPin(
    @DocumentId
    val id: String = "",
    
    @PropertyName("title")
    val title: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("latitude")
    val latitude: Double = 0.0,
    
    @PropertyName("longitude")
    val longitude: Double = 0.0,
    
    @PropertyName("createdBy")
    val createdBy: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    
    @PropertyName("imageUrl")
    val imageUrl: String? = null
) {
    // Empty constructor for Firestore
    constructor() : this(
        id = "",
        title = "",
        description = "",
        latitude = 0.0,
        longitude = 0.0,
        createdBy = "",
        createdAt = System.currentTimeMillis(),
        isPublic = true,
        imageUrl = null
    )
}

 