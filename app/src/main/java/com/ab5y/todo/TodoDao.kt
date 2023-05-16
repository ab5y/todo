package com.ab5y.todo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo")
    fun getAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todo WHERE uid IN (:todoIds)")
    fun loadAllByIds(todoIds: IntArray): Flow<List<Todo>>

    @Query("SELECT * FROM todo WHERE text LIKE :text")
    fun findByText(text: String): Flow<List<Todo>>

    @Query("SELECT * FROM todo WHERE done = :done")
    fun findByStatus(done: Boolean): Flow<List<Todo>>

    @Update
    suspend fun updateTodo(todo: Todo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo)

    @Insert
    suspend fun insertAll(vararg todos: Todo)

    @Delete
    suspend fun delete(todo: Todo)
}