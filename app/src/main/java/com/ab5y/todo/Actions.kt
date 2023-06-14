package com.ab5y.todo

sealed class Action {
    abstract val type: ActionType
    companion object {
        val actions: MutableList<Action> = mutableListOf()
    }
}

data class ItemCreated(val item: Todo) : Action() {
    override val type: ActionType = ActionType.ITEM_CREATED
}

data class ItemCompleted(val item: Todo) : Action() {
    override val type: ActionType = ActionType.ITEM_COMPLETED
}

// Add more action types by creating additional subclasses of Action

enum class ActionType {
    ITEM_CREATED,
    ITEM_COMPLETED
}