package com.example.caster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class InventoryAdapter(
    private val items: MutableList<InventoryItem>,
    private val onQtyChanged: (InventoryItem, Int) -> Unit,
    private val onDelete: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.VH>() {
	class VH(v: View) : RecyclerView.ViewHolder(v) {
		val name: TextView = v.findViewById(R.id.textName)
		val qty: TextView = v.findViewById(R.id.textQty)
		val plus: Button = v.findViewById(R.id.btnPlus)
		val minus: Button = v.findViewById(R.id.btnMinus)
		val delete: ImageButton = v.findViewById(R.id.btnDelete)
	}
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
		val v = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
		return VH(v)
	}
	override fun onBindViewHolder(holder: VH, position: Int) {
		val item = items[position]
		holder.name.text = item.name
		holder.qty.text = item.quantity.toString()
		holder.plus.setOnClickListener { onQtyChanged(item, item.quantity + 1) }
		holder.minus.setOnClickListener { if (item.quantity > 0) onQtyChanged(item, item.quantity - 1) }
		holder.delete.setOnClickListener { onDelete(item) }
	}
	override fun getItemCount(): Int = items.size

	fun updateData(newItems: List<InventoryItem>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}
}

class CatchAdapter(private val items: MutableList<CatchItem>, private val onImageClick: (String) -> Unit) : RecyclerView.Adapter<CatchAdapter.VH>() {
	class VH(v: View) : RecyclerView.ViewHolder(v) {
		val text: TextView = v.findViewById(R.id.textCatch)
		val image: ImageView = v.findViewById(R.id.imageThumb)
	}
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
		val v = LayoutInflater.from(parent.context).inflate(R.layout.item_catch, parent, false)
		return VH(v)
	}
	override fun onBindViewHolder(holder: VH, position: Int) {
		val catchItem = items[position]
		holder.text.text = buildString {
			append(catchItem.type)
			catchItem.sizeKg?.let { s -> append("  •  ${s}kg") }
			catchItem.lengthCm?.let { l -> append("  •  ${l}cm") }
		}
		if (!catchItem.imagePath.isNullOrEmpty()) {
			holder.image.visibility = View.VISIBLE
			Glide.with(holder.image.context).load(java.io.File(catchItem.imagePath)).centerCrop().into(holder.image)
			holder.image.setOnClickListener { onImageClick(catchItem.imagePath!!) }
		} else {
			holder.image.visibility = View.GONE
			holder.image.setOnClickListener(null)
		}
	}
	override fun getItemCount(): Int = items.size

	fun updateData(newItems: List<CatchItem>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}
}


