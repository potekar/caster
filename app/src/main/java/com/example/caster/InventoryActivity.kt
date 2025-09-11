package com.example.caster

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry



class InventoryActivity : AppCompatActivity() {
	private lateinit var storage: InventoryStorage
	private lateinit var inventoryAdapter: InventoryAdapter
	private lateinit var catchAdapter: CatchAdapter
	private var selectedCatchImageUri: Uri? = null

	private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		selectedCatchImageUri = uri
		val preview = findViewById<ImageView>(R.id.catchImagePreview)
		if (uri != null) {
			preview.visibility = android.view.View.VISIBLE
			preview.setImageURI(uri)
		} else {
			preview.visibility = android.view.View.GONE
			preview.setImageDrawable(null)
		}
	}
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_inventory)

		applyImmersiveMode()

		findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)?.apply {
			selectedItemId = R.id.nav_inventory
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_feed -> { 
						val intent = android.content.Intent(this@InventoryActivity, FeedActivity::class.java)
						intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
						startActivity(intent)
						overridePendingTransition(0, 0)
						true 
					}
					R.id.nav_home -> { 
						val intent = android.content.Intent(this@InventoryActivity, MainActivity::class.java)
						intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
						startActivity(intent)
						overridePendingTransition(0, 0)
						true 
					}
					R.id.nav_inventory -> true
					else -> false
				}
			}
		}

		storage = InventoryStorage(this)

		// Setup lists
		val invList = findViewById<RecyclerView>(R.id.listInventory)
		invList.layoutManager = LinearLayoutManager(this)
		inventoryAdapter = InventoryAdapter(storage.getInventory(), { item, newQty ->
			val list = storage.getInventory()
			val idx = list.indexOfFirst { it.id == item.id }
			if (idx >= 0) {
				list[idx] = list[idx].copy(quantity = newQty)
				storage.saveInventory(list)
				inventoryAdapter.updateData(list)
			}
		}, { item ->
			val list = storage.getInventory()
			val idx = list.indexOfFirst { it.id == item.id }
			if (idx >= 0) {
				list.removeAt(idx)
				storage.saveInventory(list)
				inventoryAdapter.updateData(list)
			}
		})
		invList.adapter = inventoryAdapter

		val catchList = findViewById<RecyclerView>(R.id.listCatches)
		catchList.layoutManager = LinearLayoutManager(this)
		catchAdapter = CatchAdapter(storage.getCatches()) { path ->
			showImageFullscreen(path)
		}
		catchList.adapter = catchAdapter

		// Add inventory item
		findViewById<Button>(R.id.btnAddItem).setOnClickListener {
			val name = findViewById<EditText>(R.id.inputItemName).text?.toString()?.trim().orEmpty()
			val qty = findViewById<EditText>(R.id.inputItemQty).text?.toString()?.toIntOrNull() ?: 0
			if (name.isNotEmpty() && qty > 0) {
				storage.addInventoryItem(name, qty)
				inventoryAdapter.updateData(storage.getInventory())
				findViewById<EditText>(R.id.inputItemName).setText("")
				findViewById<EditText>(R.id.inputItemQty).setText("")
			}
		}

		// Pick catch image
		findViewById<Button>(R.id.btnPickCatchImage).setOnClickListener {
			imagePicker.launch("image/*")
		}

		// Add catch
		findViewById<Button>(R.id.btnAddCatch).setOnClickListener {
			val type = findViewById<EditText>(R.id.inputCatchType).text?.toString()?.trim().orEmpty()
			val size = findViewById<EditText>(R.id.inputCatchSize).text?.toString()?.toDoubleOrNull()
			val length = findViewById<EditText>(R.id.inputCatchLength).text?.toString()?.toDoubleOrNull()
			val savedPath = selectedCatchImageUri?.let { saveImageLocally(it) }
			storage.addCatch(type = type, sizeKg = size, lengthCm = length, imagePath = savedPath)
			catchAdapter.updateData(storage.getCatches())
			updateStats()
			findViewById<EditText>(R.id.inputCatchType).setText("")
			findViewById<EditText>(R.id.inputCatchSize).setText("")
			findViewById<EditText>(R.id.inputCatchLength).setText("")
			selectedCatchImageUri = null
			findViewById<ImageView>(R.id.catchImagePreview).apply {
				setImageDrawable(null)
				visibility = android.view.View.GONE
			}
		}

		// Single subtle clear-all button
		findViewById<Button>(R.id.btnClearAllData).setOnClickListener {
			androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle("Clear all data?")
				.setMessage("This will remove all inventory, catches, and delete saved images.")
				.setPositiveButton("Delete") { d, _ ->
					storage.clearInventory()
					storage.clearCatches(deleteImages = true)
					inventoryAdapter.updateData(storage.getInventory())
					catchAdapter.updateData(storage.getCatches())
					updateStats()
					d.dismiss()
				}
				.setNegativeButton("Cancel", null)
				.show()
		}

		updateStats()
	}

	private fun saveImageLocally(uri: Uri): String? {
		return try {
			val input = contentResolver.openInputStream(uri) ?: return null
			val file = java.io.File(filesDir, "catch_${System.currentTimeMillis()}.jpg")
			file.outputStream().use { out -> input.copyTo(out) }
			input.close()
			file.absolutePath
		} catch (_: Exception) { null }
	}

	private fun updateStats() {
		val catches = storage.getCatches()
		val total = catches.size
		val avgKg = catches.mapNotNull { it.sizeKg }.average().takeIf { !it.isNaN() }
		val maxKg = catches.mapNotNull { it.sizeKg }.maxOrNull()
		val maxLen = catches.mapNotNull { it.lengthCm }.maxOrNull()
		val text = buildString {
			append("Total catches: ${total}\n")
			avgKg?.let { append("Average size: ${String.format("%.2f", it)} kg\n") }
			maxKg?.let { append("Biggest size: ${it} kg\n") }
			maxLen?.let { append("Longest: ${it} cm\n") }
		}
		findViewById<android.widget.TextView>(R.id.textStats).text = text

		// Update chart with last 7 catches by size
		val chart = findViewById<BarChart>(R.id.catchChart)
		if (chart != null) {
		val last = catches.mapNotNull { it.sizeKg }.takeLast(7)
		val entries = last.mapIndexed { index, d -> BarEntry(index.toFloat(), d.toFloat()) }
		val dataSet = BarDataSet(entries, "Size (kg)")
		dataSet.setDrawValues(false)
		chart.setData(BarData(dataSet))
		chart.description.isEnabled = true
		chart.description.text = "Recent catches"
		chart.axisRight.isEnabled = false
		chart.invalidate()
		}
	}


	private fun showImageFullscreen(path: String) {
		val file = java.io.File(path)
		if (!file.exists()) {
			android.widget.Toast.makeText(this, "Image not found", android.widget.Toast.LENGTH_SHORT).show()
			return
		}
		val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
		val container = android.widget.FrameLayout(this)
		container.setBackgroundColor(android.graphics.Color.BLACK)
		val imageView = android.widget.ImageView(this)
		imageView.adjustViewBounds = true
		imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
		imageView.layoutParams = android.widget.FrameLayout.LayoutParams(
			android.view.ViewGroup.LayoutParams.MATCH_PARENT,
			android.view.ViewGroup.LayoutParams.MATCH_PARENT
		)
		container.addView(imageView)
		dialog.setContentView(container)
		com.bumptech.glide.Glide.with(this)
			.load(file)
			.error(android.R.drawable.stat_notify_error)
			.into(imageView)
		container.setOnClickListener { dialog.dismiss() }
		dialog.show()
	}

	private fun applyImmersiveMode() {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		val controller = WindowInsetsControllerCompat(window, window.decorView)
		controller.hide(WindowInsetsCompat.Type.systemBars())
		controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
	}

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		if (hasFocus) applyImmersiveMode()
	}
}


