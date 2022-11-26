package com.akrio.placebook.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.akrio.placebook.model.Bookmark

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmark_table")
    fun loadAll(): LiveData<List<Bookmark>>

    @Query("SELECT * FROM bookmark_table WHERE id = :bookmarkId")
    fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

    @Query("SELECT * FROM bookmark_table WHERE id = :bookmarkId")
    fun loadBookmark(bookmarkId: Long): Bookmark

    @Insert(onConflict = IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long

    @Update(onConflict = REPLACE)
    fun updateBookmark(bookmark: Bookmark)

    @Delete
    fun deleteBookmark(bookmark: Bookmark)
}