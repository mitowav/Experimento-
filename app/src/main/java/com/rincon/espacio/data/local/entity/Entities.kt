package com.rincon.espacio.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val colorKey: String,
    val iconKey: String,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float,
    val zIndex: Int,
    val isTask: Boolean,
    val done: Boolean,
    val pinned: Boolean,
    val priority: String,
    val dueDate: Long?,
    val dueTime: Int?,
    val goalId: Long?,
    val subjectId: Long?,
    val reminderId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(
        entity = NoteEntity::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("noteId")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long,
    val text: String,
    val done: Boolean,
    val position: Int,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String,
    val colorKey: String,
    val iconKey: String,
    val targetDate: Long?,
    val archived: Boolean,
    val createdAt: Long,
    val position: Int,
)

@Entity(
    tableName = "goal_steps",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("goalId")],
)
data class GoalStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val title: String,
    val done: Boolean,
    val position: Int,
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val colorKey: String,
    val iconKey: String,
    val cadence: String,
    val timesPerWeek: Int,
    val reminderTime: Int?,
    val reminderId: Long?,
    val archived: Boolean,
    val createdAt: Long,
    val position: Int,
)

@Entity(
    tableName = "habit_checks",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val habitId: Long,
    val date: Long,
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorKey: String,
    val iconKey: String,
    val teacher: String,
    val archived: Boolean,
    val position: Int,
)

@Entity(
    tableName = "exams",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subjectId")],
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long,
    val title: String,
    val date: Long,
    val time: Int?,
    val readiness: Int,
    val reminderId: Long?,
)

@Entity(tableName = "study_sessions", indices = [Index("subjectId"), Index("date")])
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long?,
    val date: Long,
    val startTime: Int,
    val minutes: Int,
    val note: String,
    val completed: Boolean,
    val reminderId: Long?,
)

@Entity(tableName = "events", indices = [Index("date")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val date: Long,
    val startTime: Int?,
    val endTime: Int?,
    val colorKey: String,
    val iconKey: String,
    val note: String,
    val reminderId: Long?,
)

@Entity(tableName = "reminders", indices = [Index(value = ["targetType", "targetId"])])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val targetType: String,
    val targetId: Long,
    val title: String,
    val body: String,
    val iconKey: String,
    val triggerAtMillis: Long,
    val repeat: String,
    val leadMinutes: Int,
    val enabled: Boolean,
    val sound: Boolean,
    val vibrate: Boolean,
)
