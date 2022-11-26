package com.akrio.placebook.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.akrio.placebook.BuildConfig.MAPS_API_KEY
import com.akrio.placebook.R
import com.akrio.placebook.adapter.BookmarkInfoWindowAdapter
import com.akrio.placebook.databinding.ActivityMapsBinding
import com.akrio.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private val apiKey = MAPS_API_KEY

    private val mapsViewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupPlacesClient()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapListeners()
        getCurrentLocation()
    }

    private fun setupMapListeners() {
        mMap.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        mMap.setOnPoiClickListener {
            displayPoi(it)
        }
        mMap.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
        createBookmarkMarkerObserver()
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupPlacesClient() {
        Places.initialize(applicationContext, apiKey)
        placesClient = Places.createClient(this)
    }

    private fun displayPoi(poi: PointOfInterest) {
        displayPoiGetPlaceStep(poi)
    }

    private fun displayPoiGetPlaceStep(poi: PointOfInterest) {
        val placeId = poi.placeId
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request).addOnSuccessListener {
            val place = it.place
            displayPoiGetPhotoStep(place)
        }.addOnFailureListener {
            if (it is ApiException) {
                val statusCode = it.statusCode
                Log.e(TAG, "Place not found: ${it.message}, status code: $statusCode")
            }
        }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadataList = place.photoMetadatas
        val photoMetadata = photoMetadataList?.get(0)
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }
        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width))
            .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height))
            .build()

        placesClient.fetchPhoto(photoRequest).addOnSuccessListener {
            val bitmap = it.bitmap
            displayPoiDisplayStep(place, bitmap)
        }.addOnFailureListener {
            if (it is ApiException) {
                val statusCode = it.statusCode
                Log.e(TAG, "Place not found: ${it.message}, status code: $statusCode")
            }
        }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(place.latLng as LatLng)
                .title(place.name)
                .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
        )

    }

    private fun isLocationPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")

    private fun getCurrentLocation() {
        if (!isLocationPermissionGranted()) {
            requestLocationPermission()
        } else {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    mMap.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null) {
            GlobalScope.launch {
                mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
            }
        }
        marker.remove()
    }

    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkMarkerView
    ): Marker? {
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(bookmark.location)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.8f)
        )

        marker?.tag = bookmark

        return marker
    }

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkMarkerView>
    ) {
        bookmarks.forEach {
            addPlaceMarker(it)
        }
    }

    private fun createBookmarkMarkerObserver() {
        mapsViewModel.getBookmarkMarkerViews()?.observe(this) {
            mMap.clear()
            it?.let {
                displayAllBookmarks(it)
            }
        }
    }

    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)
}
