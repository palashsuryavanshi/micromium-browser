package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY parentId, sortOrder, createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE parentId = :parentId ORDER BY sortOrder, createdAt DESC")
    fun getBookmarksInFolder(parentId: Long?): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isFolder = 1 ORDER BY parentId, sortOrder")
    fun getAllFolders(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url")
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity): Int

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    fun isBookmarked(url: String): Flow<Boolean>

    // Folder management
    @Query("SELECT COUNT(*) FROM bookmarks WHERE parentId = :folderId")
    suspend fun countItemsInFolder(folderId: Long): Int

    @Query("SELECT MAX(sortOrder) FROM bookmarks WHERE parentId = :parentId")
    suspend fun getMaxSortOrder(parentId: Long?): Int?

    // History
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 200")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history WHERE url LIKE '%' || :host || '%'")
    suspend fun deleteHistoryForHost(host: String): Int

    @Query("DELETE FROM history WHERE visitedAt < :beforeTimestamp")
    suspend fun deleteHistoryBefore(beforeTimestamp: Long): Int

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}
