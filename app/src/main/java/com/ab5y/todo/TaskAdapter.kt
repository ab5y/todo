package com.ab5y.todo

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ab5y.todo.databinding.ItemBinding

class TaskAdapter: ListAdapter<Todo, TaskAdapter.ViewHolder>(TodoDiffCallback()) {

    private lateinit var binding: ItemBinding
    var onCheckListener: ((todo: Todo)->Unit)? = null

    class ViewHolder(binding: ItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val checkBox: CheckBox = binding.todoItem

        fun bind(todo: Todo) {
            checkBox.text = todo.text
            checkBox.isChecked = todo.done == true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = getItem(position)
        holder.bind(todo)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Perform the desired action based on the checked state
            if (isChecked) {
                // Checkbox is checked
                this@TaskAdapter.onCheckListener?.invoke(todo)
            } else {
                // Checkbox is unchecked
                println("Checkbox is unchecked")
            }
        }
    }
}