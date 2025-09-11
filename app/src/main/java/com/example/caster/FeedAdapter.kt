package com.example.caster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Post(val text: String, val imageUri: Uri?, val createdAt: Long)

class FeedAdapter(private val items: List<Post>) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

	class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val text: TextView = itemView.findViewById(R.id.postText)
		val image: ImageView = itemView.findViewById(R.id.postImage)
		val time: TextView = itemView.findViewById(R.id.postTime)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
		return PostViewHolder(view)
	}

	override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
		val post = items[position]
		holder.text.text = post.text
		holder.time.text = formatTimestamp(post.createdAt)
		if (post.imageUri != null) {
			holder.image.visibility = View.VISIBLE
			Glide.with(holder.image.context)
				.load(post.imageUri)
				.fitCenter()
				.into(holder.image)
		} else {
			holder.image.visibility = View.GONE
		}
	}

	override fun getItemCount(): Int = items.size

	private fun formatTimestamp(timestampMillis: Long): String {
		return try {
			val now = System.currentTimeMillis()
			val diff = now - timestampMillis
			val oneMinute = 60_000L
			val oneHour = 60 * oneMinute
			val oneDay = 24 * oneHour
			when {
				diff < oneMinute -> "Just now"
				diff < oneHour -> "${diff / oneMinute}m ago"
				diff < oneDay -> "${diff / oneHour}h ago"
				else -> {
					val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
					sdf.format(Date(timestampMillis))
				}
			}
		} catch (_: Exception) {
			""
		}
	}
}


