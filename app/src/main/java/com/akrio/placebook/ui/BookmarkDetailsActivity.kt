package com.akrio.placebook.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.akrio.placebook.R
import com.akrio.placebook.databinding.ActivityBookmarkDetailsBinding
import com.akrio.placebook.util.ImageUtils
import com.akrio.placebook.viewmodel.BookmarkDetailsViewModel
import java.io.File
import java.net.URLEncoder

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

    private val getCameraResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val photoFile = photoFile ?: return@registerForActivityResult
                val uri =
                    FileProvider.getUriForFile(this, "com.akrio.placebook.fileprovider", photoFile)
                revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val image = getImageWithPath(photoFile.absolutePath)
                val bitmap = ImageUtils.rotateImageIfRequired(this, image, uri)
                updateImage(bitmap)
            }
        }

    private val getGalleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            if (it.resultCode == Activity.RESULT_OK && (it != null && it.data != null)) {
                val result = it.data
                val imageUri = result?.data as Uri
                val image = getImageWithAuthority(imageUri)
                image?.let {
                    val bitmap = ImageUtils.rotateImageIfRequired(this, it, imageUri)
                    updateImage(bitmap)
                }

            }
        }
    private var photoFile: File? = null
    private lateinit var databinding: ActivityBookmarkDetailsBinding

    private val bookmarkDetailsViewModel
            by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView:
            BookmarkDetailsViewModel.BookmarkDetailsView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()
        getIntentData()
        setupFab()
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let {
            val placeImage = it.getImage(this)
            placeImage?.let {
                databinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
        databinding.imageViewPlace.setOnClickListener {
            replaceImage()
        }
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(
            MapsActivity.EXTRA_BOOKMARK_ID, 0
        )

        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this) {
            it?.let {
                bookmarkDetailsView = it
                databinding.bookmarkDetailsView = it
                populateImageView()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_save -> {
                saveChanges()
                true
            }
            R.id.action_delete -> {
                deleteBookmark()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun saveChanges() {
        val name = databinding.editTextName.text.toString()
        if (name.trim().isEmpty()) {
            Toast.makeText(this, "Empty name is not allowed", Toast.LENGTH_LONG).show()
            return

        }
        bookmarkDetailsView?.let {
            it.name = databinding.editTextName.text.toString()
            it.notes = databinding.editTextNotes.text.toString()
            it.address = databinding.editTextAddress.text.toString()
            it.phone = databinding.editTextPhone.text.toString()
            bookmarkDetailsViewModel.updateBookmark(it)
        }
        finish()
    }

    override fun onCaptureClick() {
        photoFile = null
        try {
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: java.io.IOException) {
            return
        }
        photoFile?.let {
            val photoUri =
                FileProvider.getUriForFile(
                    this,
                    "com.akrio.placebook.fileprovider",
                    photoFile!!
                )

            val captureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)

            val intentActivities = packageManager.queryIntentActivities(
                captureIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            intentActivities.map {
                it.activityInfo.packageName
            }.forEach {
                grantUriPermission(it, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            getCameraResult.launch(captureIntent)
        }
    }

    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getGalleryResult.launch(pickIntent)
    }

    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    private fun updateImage(image: Bitmap) {
        bookmarkDetailsView?.let {
            databinding.imageViewPlace.setImageBitmap(image)
            it.setImage(this, image)
        }
    }

    private fun getImageWithPath(filepath: String) = ImageUtils.decodeFileToSize(
        filepath,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height)
    )

    private fun getImageWithAuthority(uri: Uri) = ImageUtils.decodeUriStreamToSize(
        uri,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height),
        this
    )

    private fun deleteBookmark() {
        val bookmarkView = bookmarkDetailsView ?: return

        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().show()
    }

    private fun sharePlace() {
        val bookmarkView = bookmarkDetailsView ?: return
        val mapUrl: String
        if (bookmarkView.placeId == null) {
            val location =
                URLEncoder.encode("${bookmarkView.latitude}, ${bookmarkView.longitude}", "utf-8")
            mapUrl = "http://www.google.com/maps/dir/?api=1&destination=$location"
        } else {
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            mapUrl =
                "http://www.google.com/maps/dir/?api=1&destination=$name&destination_place_id=${bookmarkView.placeId}"
        }

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ${bookmarkView.name} at:\n$mapUrl")
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing ${bookmarkView.name}")

        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }

    private fun setupFab() {
        databinding.fab.setOnClickListener {
            sharePlace()
        }
    }

}