package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index("parentId"), Index("sortOrder")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val parentId: Long? = null, // null = root folder
    val sortOrder: Int = 0,
    val isFolder: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isRoot: Boolean
        get() = parentId == null
}
