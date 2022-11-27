package com.akrio.placebook.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.akrio.placebook.R
import com.akrio.placebook.databinding.ActivityBookmarkDetailsBinding
import com.akrio.placebook.viewmodel.BookmarkDetailsViewModel

class BookmarkDetailsActivity : AppCompatActivity() {

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
}