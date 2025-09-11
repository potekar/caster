package com.example.caster

data class InventoryItem(
	val id: String,
	val name: String,
	val quantity: Int
)

data class CatchItem(
	val id: String,
	val type: String,
	val sizeKg: Double?,
	val lengthCm: Double?,
	val imagePath: String?,
	val timestamp: Long
)


