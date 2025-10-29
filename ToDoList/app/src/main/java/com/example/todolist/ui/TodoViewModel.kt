package com.example.todolist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.TodoRepository
import com.example.todolist.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TodoViewModel(private val repo: TodoRepository) : ViewModel() {
    val todos: Flow<List<TodoEntity>> = repo.todos

    fun add(title: String) = viewModelScope.launch { repo.add(title) }
    fun toggle(todo: TodoEntity) = viewModelScope.launch { repo.toggle(todo) }
    fun rename(todo: TodoEntity, t: String) = viewModelScope.launch { repo.rename(todo, t) }
    fun delete(todo: TodoEntity) = viewModelScope.launch { repo.remove(todo) }
}
