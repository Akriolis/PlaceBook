package com.akrio.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.akrio.placebook.model.Bookmark
import com.akrio.placebook.repository.BookmarkRepo
import com.akrio.placebook.util.ImageUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MapsViewModel"
    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())

    private var bookmarks: LiveData<List<BookmarkView>>? = null

    private fun bookmarkToBookmarkView(bookmark: Bookmark) =
        BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone

        )

    private fun mapBookmarksToBookmarkView() {
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks) { it ->
            it.map {
                bookmarkToBookmarkView(it)
            }
        }
    }

    fun getBookmarkViews(): LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.apply {
            placeID = place.id
            name = place.name?.toString() ?: "Name not found"
            longitude = place.latLng?.longitude ?: 0.0
            latitude = place.latLng?.latitude ?: 0.0
            phone = place.phoneNumber?.toString() ?: "Phone number not found"
            address = place.address?.toString() ?: "Address not found"
        }

        val newId =
            bookmarkRepo.addBookmark(bookmark)

        image?.let {
            bookmark.setImage(it, getApplication())
        }

        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var phone: String = ""
    ) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }
    }
}