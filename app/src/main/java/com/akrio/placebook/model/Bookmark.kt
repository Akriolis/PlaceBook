package com.akrio.placebook.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark_table")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    @ColumnInfo(name = "place_Id")
    var placeID: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = ""
)