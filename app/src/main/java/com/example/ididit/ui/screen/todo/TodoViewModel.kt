package com.example.ididit.ui.screen.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ididit.data.local.SubtaskEntity
import com.example.ididit.data.local.TodoEntity
import com.example.ididit.data.local.TopicEntity
import com.example.ididit.data.repository.SubtaskRepository
import com.example.ididit.data.repository.TodoRepository
import com.example.ididit.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TodoWithSubtasks(
    val todo: TodoEntity,
    val topic: TopicEntity?,
    val subtasks: List<SubtaskEntity>
)

data class TodoUiState(
    val topics: List<TopicEntity> = emptyList(),
    val todosWithSubtasks: List<TodoWithSubtasks> = emptyList(),
    val showAddTodoDialog: Boolean = false,
    val showAddTopicDialog: Boolean = false,
    val showSubtaskDialog: Long? = null, // todoId
    val isLoading: Boolean = true
)

class TodoViewModel(
    private val todoRepository: TodoRepository,
    private val subtaskRepository: SubtaskRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                todoRepository.getAllTodos(),
                subtaskRepository.getSubtasksByTodo(0), // Get all subtasks
                topicRepository.getAllTopics()
            ) { todos, subtasks, topics ->
                Triple(todos, subtasks, topics)
            }.collect { (todos, allSubtasks, topics) ->
                // Get subtasks for each todo
                val topicMap = topics.associateBy { it.id }
                val subtasksByTodo = allSubtasks.groupBy { it.todoId }

                val todosWithSubtasks = todos.map { todo ->
                    TodoWithSubtasks(
                        todo = todo,
                        topic = todo.topicId?.let { topicMap[it] },
                        subtasks = subtasksByTodo[todo.id] ?: emptyList()
                    )
                }

                _uiState.value = _uiState.value.copy(
                    topics = topics,
                    todosWithSubtasks = todosWithSubtasks,
                    isLoading = false
                )
            }
        }
    }

    fun addTodo(title: String, topicId: Long?) {
        viewModelScope.launch {
            val todo = TodoEntity(
                title = title,
                topicId = topicId
            )
            todoRepository.insert(todo)
            _uiState.value = _uiState.value.copy(showAddTodoDialog = false)
        }
    }

    fun toggleTodoCompletion(todoId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            todoRepository.toggleCompletion(todoId, isCompleted)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.delete(todo)
        }
    }

    fun addSubtask(todoId: Long, title: String) {
        viewModelScope.launch {
            val subtask = SubtaskEntity(todoId = todoId, title = title)
            subtaskRepository.insert(subtask)
            _uiState.value = _uiState.value.copy(showSubtaskDialog = null)
        }
    }

    fun toggleSubtaskCompletion(subtaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            subtaskRepository.toggleCompletion(subtaskId, isCompleted)
        }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            subtaskRepository.delete(subtask)
        }
    }

    fun addTopic(name: String, color: Long) {
        viewModelScope.launch {
            val topic = TopicEntity(name = name, color = color)
            topicRepository.insert(topic)
            _uiState.value = _uiState.value.copy(showAddTopicDialog = false)
        }
    }

    fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            topicRepository.delete(topic)
        }
    }

    fun showAddTodoDialog() {
        _uiState.value = _uiState.value.copy(showAddTodoDialog = true)
    }

    fun hideAddTodoDialog() {
        _uiState.value = _uiState.value.copy(showAddTodoDialog = false)
    }

    fun showAddTopicDialog() {
        _uiState.value = _uiState.value.copy(showAddTopicDialog = true)
    }

    fun hideAddTopicDialog() {
        _uiState.value = _uiState.value.copy(showAddTopicDialog = false)
    }

    fun showSubtaskDialog(todoId: Long) {
        _uiState.value = _uiState.value.copy(showSubtaskDialog = todoId)
    }

    fun hideSubtaskDialog() {
        _uiState.value = _uiState.value.copy(showSubtaskDialog = null)
    }

    class Factory(
        private val todoRepository: TodoRepository,
        private val subtaskRepository: SubtaskRepository,
        private val topicRepository: TopicRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoViewModel(todoRepository, subtaskRepository, topicRepository) as T
        }
    }
}
