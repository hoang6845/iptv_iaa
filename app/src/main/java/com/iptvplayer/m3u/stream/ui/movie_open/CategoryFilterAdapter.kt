package com.iptvplayer.m3u.stream.ui.movie_open

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.databinding.ItemCategoryFilterBinding

class CategoryFilterAdapter : RecyclerView.Adapter<CategoryFilterAdapter.VH>() {

    data class Item(
        val categoryId: Int?,   // null = "All"
        val name: String,
        val count: Int,
        val isSelected: Boolean
    )

    private var items: List<Item> = emptyList()
    private var onItemClick: ((Int?) -> Unit)? = null

    fun setOnItemClick(l: (Int?) -> Unit) { onItemClick = l }

    fun submitList(list: List<Item>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemCategoryFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick?.invoke(items[bindingAdapterPosition].categoryId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvName.text  = item.name
        holder.binding.tvCount.text = item.count.toString()
        holder.binding.ivCheck.isVisible = item.isSelected
    }
}