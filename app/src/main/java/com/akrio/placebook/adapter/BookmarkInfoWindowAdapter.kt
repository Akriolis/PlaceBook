package com.akrio.placebook.adapter

import android.app.Activity
import android.view.View
import com.akrio.placebook.databinding.ContentBookmarkInfoBinding
import com.akrio.placebook.ui.MapsActivity
import com.akrio.placebook.viewmodel.MapsViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class BookmarkInfoWindowAdapter(val context: Activity) : GoogleMap.InfoWindowAdapter {

    private val binding = ContentBookmarkInfoBinding
        .inflate(context.layoutInflater)

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun getInfoContents(marker: Marker): View {
        binding.title.text = marker.title ?: ""
        binding.phone.text = marker.snippet ?: ""
        when (marker.tag) {
            is MapsActivity.PlaceInfo -> {
                binding.photo.setImageBitmap(
                    (marker.tag as MapsActivity.PlaceInfo).image
                )
            }
            is MapsViewModel.BookmarkView -> {
                val bookMarkView =
                    marker.tag as MapsViewModel.BookmarkView
                binding.photo.setImageBitmap(bookMarkView.getImage(context))

            }
        }
        return binding.root
    }


}