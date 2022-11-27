package com.akrio.placebook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.akrio.placebook.R
import com.akrio.placebook.databinding.BookmarkItemBinding
import com.akrio.placebook.ui.MapsActivity
import com.akrio.placebook.viewmodel.MapsViewModel

class BookmarkListAdapter(
    private var bookmarkData: List<MapsViewModel.BookmarkView>?,
    private val mapsActivity: MapsActivity
) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

    class ViewHolder(
        val binding: BookmarkItemBinding,
        private val mapsActivity: MapsActivity
    ) : RecyclerView.ViewHolder(binding.root)

    fun setBookmarkData(bookmarks: List<MapsViewModel.BookmarkView>) {
        this.bookmarkData = bookmarks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BookmarkItemBinding
            .inflate(layoutInflater, parent, false)
        return ViewHolder(binding, mapsActivity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bookmarkData?.let {
            val bookmarkViewData = it[position]
            holder.binding.root.tag = bookmarkViewData
            holder.binding.bookmarkData = bookmarkViewData

            holder.binding.bookmarkIcon.setImageResource(R.drawable.favorite_48px)
        }
    }

    override fun getItemCount(): Int =
        bookmarkData?.size ?: 0
}