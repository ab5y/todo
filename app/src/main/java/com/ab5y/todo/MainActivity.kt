package com.ab5y.todo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.ab5y.todo.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var repository: TodoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskAdapter = TaskAdapter()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.findByStatus(false).collect { items ->
                    taskAdapter.submitList(items)
                }
            }
        }

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTasks.adapter = taskAdapter

        binding.buttonAddTask.setOnClickListener {
            Log.d("Wtf", "Click is working")
            addItem()
            clearText()
        }

        binding.editTextTask.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Perform the desired action when the "Done" button is pressed
                // For example, save the input or trigger a validation process
                addItem()
                clearText()
                return@setOnEditorActionListener true
            }
            false
        }

        taskAdapter.onCheckListener = {
            removeItem(it)
        }
    }

    private fun addItem() {
        val text = this.binding.editTextTask.text.toString()
        if (text.isNotBlank()) {
            val todo = Todo(
                text = text,
                done = false,
                created_on = getDateTime(),
                finished_on = ""
            )
            lifecycleScope.launch(Dispatchers.IO) {
                repository.insert(todo)
            }
            refreshList()
        }
    }

    private fun removeItem(todo: Todo) {
        todo.done = true
        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTodo(todo)
        }
        refreshList()
    }

    private fun refreshList() {
        // Retrieve the updated list of todo items from the database using a Flow object
        lifecycleScope.launch(Dispatchers.IO) {
            repository.findByStatus(false).collect { todoItems ->
                // Submit the updated list of todo items to the adapter using the `submitList()` method
                taskAdapter.submitList(todoItems)
            }
        }
    }

    private fun clearText() {
        this.binding.editTextTask.setText("")
    }

    private fun getDateTime(): String {
        val formattedDate = SimpleDateFormat("dd-MM-yyyy").format(Date())
        val formattedTime = SimpleDateFormat("HH:mm").format(Date())
        return "$formattedDate  $formattedTime"
    }
}