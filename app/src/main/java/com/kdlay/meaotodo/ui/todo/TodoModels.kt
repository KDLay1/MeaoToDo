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

internal enum class TodoDisplayMode(val label: String) {
    List("列表"),
    Calendar("日历")
}

internal enum class TodoCalendarMode(val label: String) {
    Week("本周"),
    Month("本月"),
    Year("年度")
}

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
    val unscheduled: List<TaskEntity>,
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
    val unscheduledTasks = pendingTasks.filter { it.dueAt == null }
    val inboxTasks = pendingTasks.filter { it.listId == DEFAULT_TASK_LIST_ID }
    val upcomingTasks = pendingTasks.filter { it.dueAt?.let { dueAt -> !isOverdue(dueAt) && !isToday(dueAt) } == true }

    return TodoGroups(
        all = tasks,
        overdue = overdueTasks,
        today = todayTasks,
        unscheduled = unscheduledTasks,
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

internal fun taskTimeComparator(): Comparator<TaskEntity> =
    compareBy<TaskEntity> { !it.hasDueTime }.thenBy { it.dueAt ?: Long.MAX_VALUE }

internal fun tasksForDay(tasks: List<TaskEntity>, day: Long): List<TaskEntity> =
    tasks.filter { task -> task.dueAt?.let { startOfDay(it) == startOfDay(day) } == true }
        .sortedWith(taskTimeComparator())

internal fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

internal fun formatTime(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "%02d:%02d".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
}

internal fun formatDueAt(task: TaskEntity): String {
    val dueAt = task.dueAt ?: return ""
    return if (task.hasDueTime) "${formatDate(dueAt)} ${formatTime(dueAt)}" else formatDate(dueAt)
}

internal fun isToday(timestamp: Long): Boolean {
    val range = todayRange()
    return timestamp in range.first until range.second
}

internal fun isOverdue(timestamp: Long): Boolean = timestamp < todayRange().first

internal fun defaultTaskListIdFor(selectedListId: String): String = when (selectedListId) {
    SMART_ALL, SMART_TODAY, SMART_UPCOMING, SMART_COMPLETED -> DEFAULT_TASK_LIST_ID
    else -> selectedListId
}

internal fun defaultDueAtFor(
    selectedListId: String,
    displayMode: TodoDisplayMode = TodoDisplayMode.List,
    selectedDate: Long = System.currentTimeMillis()
): Long? = when {
    displayMode == TodoDisplayMode.Calendar -> startOfDay(selectedDate)
    selectedListId == SMART_TODAY -> todayRange().first
    else -> null
}

internal fun todayRange(): Pair<Long, Long> {
    val start = startOfDay(System.currentTimeMillis())
    return start to addDays(start, 1)
}

internal fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun startOfWeek(timestamp: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = startOfDay(timestamp)
        firstDayOfWeek = Calendar.MONDAY
    }
    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return calendar.timeInMillis
}

internal fun startOfMonth(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(timestamp)
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

internal fun startOfMonth(year: Int, month: Int): Long = Calendar.getInstance().apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun startOfYear(year: Int): Long = startOfMonth(year, Calendar.JANUARY)

internal fun addDays(timestamp: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis

internal fun addMonths(timestamp: Long, months: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.MONTH, months)
}.timeInMillis

internal fun combineDateAndTime(dateTimestamp: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(dateTimestamp)
    set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
    set(Calendar.MINUTE, minute.coerceIn(0, 59))
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun hourOf(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
internal fun minuteOf(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.MINUTE)
internal fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
internal fun monthOf(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.MONTH)
internal fun yearOf(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)

internal fun weekdayLabel(timestamp: Long): String = when (Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> "周一"
    Calendar.TUESDAY -> "周二"
    Calendar.WEDNESDAY -> "周三"
    Calendar.THURSDAY -> "周四"
    Calendar.FRIDAY -> "周五"
    Calendar.SATURDAY -> "周六"
    else -> "周日"
}

internal fun monthTitle(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${calendar.get(Calendar.YEAR)} 年 ${calendar.get(Calendar.MONTH) + 1} 月"
}
