package com.ab5y.todo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo")
data class Todo(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "done") var done: Boolean?,
    @ColumnInfo(name = "created_on") val created_on: String?,
    @ColumnInfo(name = "finished_on") val finished_on: String?
)
