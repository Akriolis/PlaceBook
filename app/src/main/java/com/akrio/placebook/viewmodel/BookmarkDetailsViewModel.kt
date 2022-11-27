package com.akrio.placebook.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.akrio.placebook.model.Bookmark
import com.akrio.placebook.repository.BookmarkRepo
import com.akrio.placebook.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val bookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes
        )
    }

    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark) {
            bookmarkToBookmarkView(it)
        }
    }

    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }
        return bookmarkDetailsView
    }

    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView): Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }
        bookmark?.apply {
            id = bookmarkView.id
            name = bookmarkView.name
            phone = bookmarkView.phone
            address = bookmarkView.address
            notes = bookmarkView.notes
        }
        return bookmark
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView){
        viewModelScope.launch (Dispatchers.IO){
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            bookmark?.let {
                bookmarkRepo.updateBookmark(it)
            }
        }
    }

    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = ""
    ) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }
    }

}