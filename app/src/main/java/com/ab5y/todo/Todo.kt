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
    @ColumnInfo(name = "finished_on") var finished_on: String?
) {
    // Other properties and methods of the Todo class

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Todo

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}
