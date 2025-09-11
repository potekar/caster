package com.example.caster

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class InventoryStorage(context: Context) {
	private val prefs = context.getSharedPreferences("inventory_store", Context.MODE_PRIVATE)
	private val gson = Gson()

	fun getInventory(): MutableList<InventoryItem> {
		val json = prefs.getString("inventory", "[]")
		val type = object : TypeToken<MutableList<InventoryItem>>() {}.type
		return gson.fromJson(json, type)
	}

	fun saveInventory(items: List<InventoryItem>) {
		prefs.edit().putString("inventory", gson.toJson(items)).apply()
	}

	fun addInventoryItem(name: String, quantity: Int) {
		val list = getInventory()
		list.add(InventoryItem(UUID.randomUUID().toString(), name, quantity))
		saveInventory(list)
	}

	fun getCatches(): MutableList<CatchItem> {
		val json = prefs.getString("catches", "[]")
		val type = object : TypeToken<MutableList<CatchItem>>() {}.type
		return gson.fromJson(json, type)
	}

	fun saveCatches(items: List<CatchItem>) {
		prefs.edit().putString("catches", gson.toJson(items)).apply()
	}

	fun addCatch(type: String, sizeKg: Double?, lengthCm: Double?, imagePath: String?) {
		val list = getCatches()
		list.add(CatchItem(UUID.randomUUID().toString(), type, sizeKg, lengthCm, imagePath, System.currentTimeMillis()))
		saveCatches(list)
	}

	fun clearInventory() {
		saveInventory(emptyList())
	}

	fun clearCatches(deleteImages: Boolean) {
		if (deleteImages) {
			try {
				getCatches().mapNotNull { it.imagePath }.forEach { path ->
					try { java.io.File(path).takeIf { it.exists() }?.delete() } catch (_: Exception) { }
				}
			} catch (_: Exception) { }
		}
		saveCatches(emptyList())
	}
}


