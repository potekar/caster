package com.example.caster

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class FeedActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: FeedAdapter
	private val posts: MutableList<Post> = mutableListOf()
	private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
	private var selectedImageUri: Uri? = null

	private var addDialogImageView: ImageView? = null
	private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		selectedImageUri = uri
		addDialogImageView?.setImageURI(uri)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_feed)
		applyImmersiveMode()

		AuthManager.signInAnonymouslyIfNeeded(onReady = {
			listenToFeed()
		}, onError = {
			Toast.makeText(this, "Auth failed: ${it.message}", Toast.LENGTH_LONG).show()
		})

		recyclerView = findViewById(R.id.feedRecycler)
		recyclerView.layoutManager = LinearLayoutManager(this)
		adapter = FeedAdapter(posts)
		recyclerView.adapter = adapter

		findViewById<ImageButton>(R.id.btnAddPost).setOnClickListener {
			showAddPostDialog()
		}

		// Bottom navigation
		findViewById<BottomNavigationView>(R.id.bottomNav)?.apply {
			selectedItemId = R.id.nav_feed
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_feed -> true
					R.id.nav_home -> { 
						val intent = android.content.Intent(this@FeedActivity, MainActivity::class.java)
						intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
						startActivity(intent)
						overridePendingTransition(0, 0)
						true 
					}
					R.id.nav_inventory -> { 
						val intent = android.content.Intent(this@FeedActivity, InventoryActivity::class.java)
						intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
						startActivity(intent)
						overridePendingTransition(0, 0)
						true 
					}
					else -> false
				}
			}
		}

	}

	private fun showAddPostDialog() {
		selectedImageUri = null
		val dialogView = layoutInflater.inflate(R.layout.dialog_add_post, null)
		val inputText = dialogView.findViewById<EditText>(R.id.inputPostText)
		val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
		addDialogImageView = imagePreview
		val pickImage = dialogView.findViewById<Button>(R.id.btnPickImage)

		pickImage.setOnClickListener {
			imagePicker.launch("image/*")
		}

		val dialog = AlertDialog.Builder(this)
			.setTitle("New Post")
			.setView(dialogView)
			.setPositiveButton("Post") { d, _ ->
				val text = inputText.text?.toString()?.trim().orEmpty()
				if (text.isEmpty() && selectedImageUri == null) {
					Toast.makeText(this, "Add text or an image", Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}
				uploadPost(text, selectedImageUri)
				d.dismiss()
			}
			.setNegativeButton("Cancel", null)
			.create()

		dialog.setOnShowListener {
			selectedImageUri?.let { imagePreview.setImageURI(it) }
		}

		dialog.setOnDismissListener {
			addDialogImageView = null
		}
		dialog.show()
	}

	private fun uploadPost(text: String, imageUri: Uri?) {
		AuthManager.signInAnonymouslyIfNeeded(onReady = {
			performUpload(text, imageUri)
		}, onError = {
			Toast.makeText(this, "Auth failed: ${it.message}", Toast.LENGTH_LONG).show()
		})
	}

	private fun performUpload(text: String, imageUri: Uri?) {
		if (imageUri != null) {
			try {
				val file = uriToTempFile(imageUri)
				ImgbbUploader.upload(
					file = file,
					apiKey = BuildConfig.IMGBB_API_KEY,
					onSuccess = { link ->
						file.delete()
						savePost(text, link)
					},
					onError = { ex ->
						file.delete()
						Toast.makeText(this, "Image upload failed: ${ex.message}", Toast.LENGTH_LONG).show()
						savePost(text, null)
					}
				)
			} catch (e: Exception) {
				Toast.makeText(this, "Error preparing image", Toast.LENGTH_SHORT).show()
				savePost(text, null)
			}
		} else {
			savePost(text, null)
		}
	}

	private fun savePost(text: String, imageUrl: String?) {
		val data = hashMapOf(
			"text" to text,
			"imageUrl" to imageUrl,
			"createdAt" to System.currentTimeMillis(),
			"isPublic" to true
		)
		firestore.collection("feed_posts").add(data)
			.addOnSuccessListener { }
			.addOnFailureListener { e ->
				Toast.makeText(this, "Post failed: ${e.message}", Toast.LENGTH_LONG).show()
			}
	}

	private fun listenToFeed() {
		firestore.collection("feed_posts")
			.orderBy("createdAt", Query.Direction.DESCENDING)
			.addSnapshotListener { snapshot, error ->
				if (error != null) {
					Toast.makeText(this, "Feed error: ${error.message}", Toast.LENGTH_LONG).show()
					return@addSnapshotListener
				}
				posts.clear()
				val now = System.currentTimeMillis()
				val cutoff = now - 24L * 60L * 60L * 1000L
				snapshot?.documents?.forEach { doc ->
					val createdAt = doc.getLong("createdAt") ?: 0L
					if (createdAt < cutoff) {
						doc.reference.delete()
					} else {
						val text = doc.getString("text") ?: ""
						val imageUrl = doc.getString("imageUrl")
						val imageUri = if (!imageUrl.isNullOrEmpty()) Uri.parse(imageUrl) else null
						posts.add(Post(text = text, imageUri = imageUri, createdAt = createdAt))
					}
				}
				adapter.notifyDataSetChanged()
			}
	}

	private fun uriToTempFile(uri: Uri): java.io.File {
		val inputStream = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open URI")
		val tempFile = java.io.File.createTempFile("feed_", ".img", cacheDir)
		tempFile.outputStream().use { output -> inputStream.copyTo(output) }
		inputStream.close()
		return tempFile
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


