package com.example.todolist.data

import com.example.todolist.data.local.TodoDao
import com.example.todolist.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {
    val todos: Flow<List<TodoEntity>> = dao.getAll()

    suspend fun add(title: String) {
        val t = title.trim()
        if (t.isNotEmpty()) dao.insert(TodoEntity(title = t))
    }

    suspend fun toggle(todo: TodoEntity) = dao.update(todo.copy(done = !todo.done))
    suspend fun rename(todo: TodoEntity, newTitle: String) {
        val t = newTitle.trim()
        if (t.isNotEmpty()) dao.update(todo.copy(title = t))
    }
    suspend fun remove(todo: TodoEntity) = dao.delete(todo)
}
