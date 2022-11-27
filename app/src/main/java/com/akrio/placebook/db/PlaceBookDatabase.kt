package com.akrio.placebook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.akrio.placebook.model.Bookmark

@Database(entities = [Bookmark::class], version = 2)
abstract class PlaceBookDatabase : RoomDatabase() {

    abstract val bookmarkDao: BookmarkDao

    companion object {

        private var instance: PlaceBookDatabase? = null

        fun getInstance(context: Context): PlaceBookDatabase {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaceBookDatabase::class.java,
                    "PlaceBook"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            return instance as PlaceBookDatabase
        }
    }
}