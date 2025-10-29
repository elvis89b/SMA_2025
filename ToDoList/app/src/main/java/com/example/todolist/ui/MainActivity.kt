package com.example.todolist.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todolist.data.TodoRepository
import com.example.todolist.data.local.AppDatabase
import com.example.todolist.data.local.TodoEntity
import androidx.compose.ui.Alignment   // ⬅ import added

class MainActivity : ComponentActivity() {

    private val viewModel: TodoViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val repo = TodoRepository(db.todoDao())
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TodoViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { TodoScreen(viewModel) } }
        }
    }
}

@Composable
fun TodoScreen(vm: TodoViewModel) {
    val todos by vm.todos.collectAsState(initial = emptyList())
    var input by remember { mutableStateOf(TextFieldValue("")) }

    var editOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TodoEntity?>(null) }
    var editText by remember { mutableStateOf(TextFieldValue("")) }

    Column(Modifier.padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = input,
                onValueChange = { input = it },
                label = { Text("New task") },
                singleLine = true
            )
            Button(
                onClick = {
                    vm.add(input.text)
                    input = TextFieldValue("")
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(todos, key = { it.id }) { todo ->
                ElevatedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(checked = todo.done, onCheckedChange = { vm.toggle(todo) })
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editTarget = todo
                                    editText = TextFieldValue(todo.title)
                                    editOpen = true
                                }
                        ) {
                            Text(todo.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (todo.done) "Done" else "Pending",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { vm.delete(todo) }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (editOpen) {
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text("Edit task") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editTarget?.let { vm.rename(it, editText.text) }
                    editOpen = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editOpen = false }) { Text("Cancel") } }
        )
    }
}
