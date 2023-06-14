package com.ab5y.todo

import androidx.annotation.WorkerThread
import javax.inject.Inject

class TodoRepository @Inject constructor(
    private val todoDao: TodoDao
) {
    fun getTodos() = todoDao.getAll()

    fun findByStatus(done: Boolean) = todoDao.findByStatus(done)

    @WorkerThread
    suspend fun insert(todo: Todo): Long {
        return todoDao.insert(todo)
    }

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun updateTodo(todo: Todo) {
        todoDao.updateTodo(todo)
    }

    @WorkerThread
    suspend fun deleteTodo(todo: Todo) {
        todoDao.delete(todo)
    }
}