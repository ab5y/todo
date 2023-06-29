package com.ab5y.todo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ab5y.todo.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var repository: TodoRepository

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    // Define the shake gesture detection variables
    private val shakeThreshold = 12 // Adjust the threshold value as needed
    private val shakeTimeout = 1000 // Adjust the timeout value as needed
    private var lastShakeTime: Long = 0

    private var gravity = FloatArray(3)
    private var acceleration = FloatArray(3)

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

        binding.fabShare.setOnClickListener {
            shareTodoList()
        }

        taskAdapter.onCheckListener = {
            completeItem(it)
        }

        // Initialize the sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Get the accelerometer sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        updateDate()
    }

    private fun updateDate() {
        if (supportActionBar?.isShowing == true) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            supportActionBar!!.subtitle  = dateFormat.format(Date())
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
                todo.uid = repository.insert(todo)
                Action.actions.add(ItemCreated(todo))
            }
            refreshList()
            // Display a Snackbar with the option to undo
            // Display a Snackbar with the option to undo
            val snackbar = Snackbar.make(binding.root, "Task added", Snackbar.LENGTH_LONG)
            snackbar.setAction("Undo") {
                // User clicked "Undo", so undo the completion
                undoAddItem(todo)
            }
            snackbar.show()
        }
    }

    private fun undoAddItem(todo: Todo) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.deleteTodo(todo)
            Action.actions.remove(ItemCreated(todo))
        }
        refreshList()
    }
    private fun completeItem(todo: Todo) {
        todo.done = true
        todo.finished_on = getDateTime()
        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTodo(todo)
            Action.actions.add(ItemCompleted(todo))
        }
        refreshList()
        // Display a Snackbar with the option to undo
        // Display a Snackbar with the option to undo
        val snackbar = Snackbar.make(binding.root, "Task completed", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo") {
            // User clicked "Undo", so undo the completion
            undoCompleteItem(todo)
        }
        snackbar.show()
    }

    private fun undoCompleteItem(todo: Todo) {
        todo.done = false
        todo.finished_on = ""
        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTodo(todo)
            Action.actions.remove(ItemCompleted(todo))
        }
        refreshList()
    }

    private fun onUndoGesture() {
        if (Action.actions.isNotEmpty()) {
            val lastAction = Action.actions.last()
            if (lastAction is ItemCompleted) {
                undoCompleteItem(lastAction.item)
                Toast.makeText(this, "Done Undone", Toast.LENGTH_SHORT).show()
            }
            else if (lastAction is ItemCreated) {
                undoAddItem(lastAction.item)
                Toast.makeText(this, "Creation Undone", Toast.LENGTH_SHORT).show()
            }
        }
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


    private fun shareTodoList() {
        shareTodoListAsImage(this, getRecyclerViewBitmap(binding.recyclerViewTasks))
    }

    private fun getRecyclerViewBitmap(recyclerView: RecyclerView): Bitmap {
        val adapter = recyclerView.adapter
        val layoutManager = recyclerView.layoutManager
        val width = recyclerView.width
        var height = 0

        // Calculate the total height of the RecyclerView by measuring each item
        for (i in 0 until (adapter?.itemCount ?: 0)) {
            val itemView = layoutManager?.findViewByPosition(i)
            height += itemView?.height ?: 0
        }

        // Create a bitmap with the calculated width and height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        recyclerView.draw(canvas)

        return bitmap
    }

    private fun shareTodoListAsImage(context: Context, bitmap: Bitmap) {
        try {
            // Step 1: Convert the bitmap into a file
            val file = File(context.cacheDir, "todo_list_image.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            // Step 2: Create an intent to share the image
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.provider", file))

            // Step 3: Start the activity chooser
            val chooser = Intent.createChooser(shareIntent, "Share Todo List")
            context.startActivity(chooser)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the accelerometer sensor listener
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the accelerometer sensor listener
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Called when the accuracy of the sensor changes (not used for accelerometer)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Apply a high-pass filter to remove gravity
            gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0]
            gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1]
            gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2]

            // Calculate the acceleration values without gravity
            acceleration[0] = event.values[0] - gravity[0]
            acceleration[1] = event.values[1] - gravity[1]
            acceleration[2] = event.values[2] - gravity[2]

            // Calculate the magnitude of acceleration
            val magnitude = sqrt(
                acceleration[0] * acceleration[0] +
                        acceleration[1] * acceleration[1] +
                        acceleration[2] * acceleration[2]
            )

            // Check if the magnitude exceeds the shake threshold
            if (magnitude > shakeThreshold) {
                val currentTime = System.currentTimeMillis()
                val timeDifference = currentTime - lastShakeTime

                // Check if enough time has passed since the last shake
                if (timeDifference > shakeTimeout) {
                    // Shake gesture detected, perform your desired action here
                    onUndoGesture()
                    // Update the last shake time
                    lastShakeTime = currentTime
                }
            }
        }
    }
}