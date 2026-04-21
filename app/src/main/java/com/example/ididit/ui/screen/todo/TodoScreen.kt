package com.example.ididit.ui.screen.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ididit.data.local.AppDatabase
import com.example.ididit.data.local.SubtaskEntity
import com.example.ididit.data.local.TodoEntity
import com.example.ididit.data.repository.SubtaskRepository
import com.example.ididit.data.repository.TodoRepository
import com.example.ididit.data.repository.TopicRepository
import com.example.ididit.ui.components.CustomDialog
import com.example.ididit.ui.components.CustomTextField
import com.example.ididit.ui.theme.LocalExtendedColors

@Composable
fun TodoScreen(
    database: AppDatabase,
    viewModel: TodoViewModel = viewModel(
        factory = TodoViewModel.Factory(
            TodoRepository(database.todoDao()),
            SubtaskRepository(database.subtaskDao()),
            TopicRepository(database.topicDao())
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val extendedColors = LocalExtendedColors.current

    // Group todos by topic
    val groupedTodos = uiState.topics.mapNotNull { topic ->
        val topicTodos = uiState.todosWithSubtasks.filter { it.todo.topicId == topic.id }
        if (topicTodos.isEmpty()) null
        else topic to topicTodos
    }

    // Todos without topic
    val todosWithoutTopic = uiState.todosWithSubtasks.filter { it.todo.topicId == null }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddTodoDialog() },
                containerColor = extendedColors.accent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "待办",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        IconButton(onClick = { viewModel.showHistoryDialog() }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "历史完成",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.showAddTopicDialog() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Manage Topics",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            groupedTodos.forEach { (topic, todos) ->
                item {
                    TodoTopicSection(
                        topicName = topic.name,
                        topicColor = Color(topic.color),
                        todosWithSubtasks = todos,
                        onToggleTodo = { viewModel.toggleTodoCompletion(it.id, !it.isCompleted) },
                        onDeleteTodo = { viewModel.deleteTodo(it) },
                        onToggleSubtask = { viewModel.toggleSubtaskCompletion(it.id, !it.isCompleted) },
                        onAddSubtask = { viewModel.showSubtaskDialog(it.id) }
                    )
                }
            }

            if (todosWithoutTopic.isNotEmpty()) {
                item {
                    TodoTopicSection(
                        topicName = "未分组",
                        topicColor = Color.Gray,
                        todosWithSubtasks = todosWithoutTopic,
                        onToggleTodo = { viewModel.toggleTodoCompletion(it.id, !it.isCompleted) },
                        onDeleteTodo = { viewModel.deleteTodo(it) },
                        onToggleSubtask = { viewModel.toggleSubtaskCompletion(it.id, !it.isCompleted) },
                        onAddSubtask = { viewModel.showSubtaskDialog(it.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (uiState.showAddTodoDialog) {
        AddTodoDialog(
            topics = uiState.topics,
            onDismiss = { viewModel.hideAddTodoDialog() },
            onAdd = { title, topicId -> viewModel.addTodo(title, topicId) }
        )
    }

    if (uiState.showAddTopicDialog) {
        AddTopicDialog(
            onDismiss = { viewModel.hideAddTopicDialog() },
            onAdd = { name, color -> viewModel.addTopic(name, color) }
        )
    }

    uiState.showSubtaskDialog?.let { todoId ->
        AddSubtaskDialog(
            onDismiss = { viewModel.hideSubtaskDialog() },
            onAdd = { title -> viewModel.addSubtask(todoId, title) }
        )
    }

    if (uiState.showHistoryDialog) {
        TodoHistoryDialog(
            completedGroups = uiState.completedTodoGroups,
            onDismiss = { viewModel.hideHistoryDialog() }
        )
    }
}

@Composable
private fun TodoTopicSection(
    topicName: String,
    topicColor: Color,
    todosWithSubtasks: List<TodoWithSubtasks>,
    onToggleTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (TodoEntity) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onAddSubtask: (TodoEntity) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(topicColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = topicName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        todosWithSubtasks.forEach { todoWithSubtasks ->
            TodoItem(
                todoWithSubtasks = todoWithSubtasks,
                onToggle = { onToggleTodo(todoWithSubtasks.todo) },
                onDelete = { onDeleteTodo(todoWithSubtasks.todo) },
                onToggleSubtask = { onToggleSubtask(it) },
                onAddSubtask = { onAddSubtask(todoWithSubtasks.todo) }
            )
        }
    }
}

@Composable
private fun TodoItem(
    todoWithSubtasks: TodoWithSubtasks,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onAddSubtask: () -> Unit
) {
    val todo = todoWithSubtasks.todo
    val subtasks = todoWithSubtasks.subtasks
    val extendedColors = LocalExtendedColors.current

    var isVisible by remember { mutableStateOf(true) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(200),
        label = "scale"
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200))
    ) {
        SwipeToDismissBox(
            state = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        isVisible = false
                        true
                    } else {
                        false
                    }
                }
            ),
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            },
            enableDismissFromStartToEnd = false
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = todo.isCompleted,
                            onCheckedChange = { onToggle() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = extendedColors.accentSage,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = todo.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                            color = if (todo.isCompleted)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onAddSubtask) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Subtask",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Subtasks
                    if (subtasks.isNotEmpty()) {
                        subtasks.forEach { subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp, end = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = subtask.isCompleted,
                                    onCheckedChange = { onToggleSubtask(subtask) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = extendedColors.accentSage,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (subtask.isCompleted)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTodoDialog(
    topics: List<com.example.ididit.data.local.TopicEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }
    var showTopicWarning by remember { mutableStateOf(false) }
    val extendedColors = LocalExtendedColors.current

    CustomDialog(
        onDismiss = onDismiss,
        title = "新增待办",
        confirmText = "添加",
        onConfirm = {
            if (title.isBlank()) return@CustomDialog
            onAdd(title, selectedTopicId)
        },
        content = {
            Column {
                CustomTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "待办内容"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "选择分组（可选）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // No topic option
                val isSelectedNone = selectedTopicId == null
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelectedNone) extendedColors.accent.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            selectedTopicId = null
                            showTopicWarning = false
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "无分组",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelectedNone) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = extendedColors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                topics.forEach { topic ->
                    val isSelected = selectedTopicId == topic.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) extendedColors.accent.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedTopicId = topic.id }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(topic.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = topic.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = extendedColors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AddTopicDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0xFF9A9A9A.toLong()) }

    val colorOptions = listOf(
        0xFF9A9A9A.toLong(),
        0xFFE8B4A8.toLong(),
        0xFFA8C4B0.toLong(),
        0xFFA8B8C8.toLong(),
        0xFFD4C89C.toLong(),
        0xFFB8A8C8.toLong()
    )

    CustomDialog(
        onDismiss = onDismiss,
        title = "添加分组",
        confirmText = "添加",
        onConfirm = {
            if (name.isNotBlank()) {
                onAdd(name, color)
            }
        },
        content = {
            Column {
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "分组名称"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { colorOption ->
                        val isSelected = color == colorOption
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorOption))
                                .clickable { color = colorOption }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    CustomDialog(
        onDismiss = onDismiss,
        title = "添加子任务",
        confirmText = "添加",
        onConfirm = {
            if (title.isNotBlank()) {
                onAdd(title)
            }
        },
        content = {
            CustomTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "子任务内容"
            )
        }
    )
}

@Composable
private fun TodoHistoryDialog(
    completedGroups: List<CompletedTodoGroup>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "完成历史",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (completedGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无完成记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        completedGroups.forEach { group ->
                            item {
                                Column {
                                    Text(
                                        text = group.date,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    group.todos.forEach { todo ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = LocalExtendedColors.current.accentSage,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = todo.title,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击外部关闭",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
