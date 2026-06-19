package com.kdlay.meaotodo.ui.todo

import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

internal const val SMART_ALL = "smart_all"
internal const val SMART_TODAY = "smart_today"
internal const val SMART_UPCOMING = "smart_upcoming"
internal const val SMART_COMPLETED = "smart_completed"

internal enum class TodoListKind {
    SMART,
    SYSTEM,
    CUSTOM
}

internal data class TodoListOption(
    val id: String,
    val label: String,
    val count: Int,
    val kind: TodoListKind
) {
    val isSmart: Boolean get() = kind == TodoListKind.SMART
    val isSystem: Boolean get() = kind == TodoListKind.SYSTEM
}

internal data class TodoGroups(
    val all: List<TaskEntity>,
    val overdue: List<TaskEntity>,
    val today: List<TaskEntity>,
    val inbox: List<TaskEntity>,
    val upcoming: List<TaskEntity>,
    val completed: List<TaskEntity>,
    val byList: Map<String, List<TaskEntity>>
) {
    fun tasksFor(listId: String): List<TaskEntity> = when (listId) {
        SMART_ALL -> all
        SMART_TODAY -> overdue + today
        SMART_UPCOMING -> upcoming
        SMART_COMPLETED -> completed
        DEFAULT_TASK_LIST_ID -> inbox
        else -> byList[listId].orEmpty()
    }
}

internal fun buildTodoGroups(tasks: List<TaskEntity>): TodoGroups {
    val pendingTasks = tasks.filterNot { it.isDone }
    val completedTasks = tasks.filter { it.isDone }
    val overdueTasks = pendingTasks.filter { it.dueAt?.let(::isOverdue) == true }
    val todayTasks = pendingTasks.filter { it.dueAt?.let(::isToday) == true }
    val inboxTasks = pendingTasks.filter { it.listId == DEFAULT_TASK_LIST_ID }
    val upcomingTasks = pendingTasks.filter { it.dueAt?.let { dueAt -> !isOverdue(dueAt) && !isToday(dueAt) } == true }

    return TodoGroups(
        all = tasks,
        overdue = overdueTasks,
        today = todayTasks,
        inbox = inboxTasks,
        upcoming = upcomingTasks,
        completed = completedTasks,
        byList = tasks.groupBy { it.listId }
    )
}

internal fun buildListOptions(groups: TodoGroups, customLists: List<TaskListEntity>): List<TodoListOption> =
    buildList {
        add(TodoListOption(SMART_ALL, "全部", groups.tasksFor(SMART_ALL).size, TodoListKind.SMART))
        add(TodoListOption(SMART_TODAY, "今天", groups.tasksFor(SMART_TODAY).size, TodoListKind.SMART))
        add(TodoListOption(SMART_UPCOMING, "未来", groups.tasksFor(SMART_UPCOMING).size, TodoListKind.SMART))
        add(TodoListOption(DEFAULT_TASK_LIST_ID, "收集箱", groups.tasksFor(DEFAULT_TASK_LIST_ID).size, TodoListKind.SYSTEM))
        customLists.forEach { taskList ->
            add(TodoListOption(taskList.id, taskList.name, groups.tasksFor(taskList.id).size, TodoListKind.CUSTOM))
        }
        add(TodoListOption(SMART_COMPLETED, "已完成", groups.tasksFor(SMART_COMPLETED).size, TodoListKind.SMART))
    }

internal fun List<TodoListOption>.taskListOptions(): List<TodoListOption> =
    filterNot { it.isSmart }

internal fun priorityLabel(priority: Int): String = when (priority) {
    3 -> "高"
    2 -> "中"
    1 -> "低"
    else -> "普通"
}

internal fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

internal fun isToday(timestamp: Long): Boolean {
    val range = todayRange()
    return timestamp in range.first until range.second
}

internal fun isOverdue(timestamp: Long): Boolean = timestamp < todayRange().first

internal fun defaultTaskListIdFor(selectedListId: String): String = when (selectedListId) {
    SMART_ALL, SMART_TODAY, SMART_UPCOMING, SMART_COMPLETED -> DEFAULT_TASK_LIST_ID
    else -> selectedListId
}

internal fun defaultDueAtFor(selectedListId: String): Long? = when (selectedListId) {
    SMART_TODAY -> todayRange().first
    else -> null
}

private fun todayRange(): Pair<Long, Long> {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    return start to calendar.timeInMillis
}
