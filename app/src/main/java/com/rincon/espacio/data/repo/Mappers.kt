package com.rincon.espacio.data.repo

import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.local.entity.EventEntity
import com.rincon.espacio.data.local.entity.ExamEntity
import com.rincon.espacio.data.local.entity.GoalEntity
import com.rincon.espacio.data.local.entity.GoalStepEntity
import com.rincon.espacio.data.local.entity.HabitCheckEntity
import com.rincon.espacio.data.local.entity.HabitEntity
import com.rincon.espacio.data.local.entity.NoteEntity
import com.rincon.espacio.data.local.entity.ReminderEntity
import com.rincon.espacio.data.local.entity.StudySessionEntity
import com.rincon.espacio.data.local.entity.SubjectEntity
import com.rincon.espacio.data.local.entity.SubtaskEntity
import com.rincon.espacio.domain.model.CalendarEvent
import com.rincon.espacio.domain.model.Exam
import com.rincon.espacio.domain.model.Goal
import com.rincon.espacio.domain.model.GoalStep
import com.rincon.espacio.domain.model.Habit
import com.rincon.espacio.domain.model.HabitCadence
import com.rincon.espacio.domain.model.HabitCheck
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.domain.model.Reminder
import com.rincon.espacio.domain.model.ReminderTarget
import com.rincon.espacio.domain.model.RepeatRule
import com.rincon.espacio.domain.model.StudySession
import com.rincon.espacio.domain.model.Subject
import com.rincon.espacio.domain.model.Subtask

/**
 * Traducción entre la capa de persistencia y el dominio.
 *
 * Las fechas se guardan como epochDay y las horas como segundo del día: enteros
 * estables, ordenables en SQL y sin sorpresas de zona horaria en la base.
 */

fun NoteEntity.toDomain() = Note(
    id = id,
    text = text,
    colorKey = colorKey,
    iconKey = iconKey,
    styleKey = styleKey,
    x = x,
    y = y,
    rotation = rotation,
    scale = scale,
    zIndex = zIndex,
    isTask = isTask,
    done = done,
    pinned = pinned,
    priority = Priority.fromKey(priority),
    dueDate = dueDate?.let(Dates::fromEpochDay),
    dueTime = dueTime?.let(Dates::fromSecondOfDay),
    goalId = goalId,
    subjectId = subjectId,
    reminderId = reminderId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    archived = archived,
)

fun Note.toEntity() = NoteEntity(
    id = id,
    text = text,
    colorKey = colorKey,
    iconKey = iconKey,
    styleKey = styleKey,
    x = x,
    y = y,
    rotation = rotation,
    scale = scale,
    zIndex = zIndex,
    isTask = isTask,
    done = done,
    pinned = pinned,
    priority = priority.name,
    dueDate = dueDate?.let(Dates::epochDay),
    dueTime = dueTime?.let(Dates::secondOfDay),
    goalId = goalId,
    subjectId = subjectId,
    reminderId = reminderId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    archived = archived,
)

fun SubtaskEntity.toDomain() = Subtask(id, noteId, text, done, position)
fun Subtask.toEntity() = SubtaskEntity(id, noteId, text, done, position)

fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    note = note,
    colorKey = colorKey,
    iconKey = iconKey,
    targetDate = targetDate?.let(Dates::fromEpochDay),
    archived = archived,
    createdAt = createdAt,
    position = position,
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    title = title,
    note = note,
    colorKey = colorKey,
    iconKey = iconKey,
    targetDate = targetDate?.let(Dates::epochDay),
    archived = archived,
    createdAt = createdAt,
    position = position,
)

fun GoalStepEntity.toDomain() = GoalStep(id, goalId, title, done, position)
fun GoalStep.toEntity() = GoalStepEntity(id, goalId, title, done, position)

fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    colorKey = colorKey,
    iconKey = iconKey,
    cadence = HabitCadence.fromKey(cadence),
    timesPerWeek = timesPerWeek,
    reminderTime = reminderTime?.let(Dates::fromSecondOfDay),
    reminderId = reminderId,
    archived = archived,
    createdAt = createdAt,
    position = position,
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    colorKey = colorKey,
    iconKey = iconKey,
    cadence = cadence.name,
    timesPerWeek = timesPerWeek,
    reminderTime = reminderTime?.let(Dates::secondOfDay),
    reminderId = reminderId,
    archived = archived,
    createdAt = createdAt,
    position = position,
)

fun HabitCheckEntity.toDomain() = HabitCheck(id, habitId, Dates.fromEpochDay(date))

fun SubjectEntity.toDomain() = Subject(id, name, colorKey, iconKey, teacher, archived, position)
fun Subject.toEntity() = SubjectEntity(id, name, colorKey, iconKey, teacher, archived, position)

fun ExamEntity.toDomain() = Exam(
    id = id,
    subjectId = subjectId,
    title = title,
    date = Dates.fromEpochDay(date),
    time = time?.let(Dates::fromSecondOfDay),
    readiness = readiness,
    reminderId = reminderId,
)

fun Exam.toEntity() = ExamEntity(
    id = id,
    subjectId = subjectId,
    title = title,
    date = Dates.epochDay(date),
    time = time?.let(Dates::secondOfDay),
    readiness = readiness,
    reminderId = reminderId,
)

fun StudySessionEntity.toDomain() = StudySession(
    id = id,
    subjectId = subjectId,
    date = Dates.fromEpochDay(date),
    startTime = Dates.fromSecondOfDay(startTime),
    minutes = minutes,
    note = note,
    completed = completed,
    reminderId = reminderId,
)

fun StudySession.toEntity() = StudySessionEntity(
    id = id,
    subjectId = subjectId,
    date = Dates.epochDay(date),
    startTime = Dates.secondOfDay(startTime),
    minutes = minutes,
    note = note,
    completed = completed,
    reminderId = reminderId,
)

fun EventEntity.toDomain() = CalendarEvent(
    id = id,
    title = title,
    date = Dates.fromEpochDay(date),
    startTime = startTime?.let(Dates::fromSecondOfDay),
    endTime = endTime?.let(Dates::fromSecondOfDay),
    colorKey = colorKey,
    iconKey = iconKey,
    note = note,
    reminderId = reminderId,
)

fun CalendarEvent.toEntity() = EventEntity(
    id = id,
    title = title,
    date = Dates.epochDay(date),
    startTime = startTime?.let(Dates::secondOfDay),
    endTime = endTime?.let(Dates::secondOfDay),
    colorKey = colorKey,
    iconKey = iconKey,
    note = note,
    reminderId = reminderId,
)

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    targetType = runCatching { ReminderTarget.valueOf(targetType) }.getOrDefault(ReminderTarget.Note),
    targetId = targetId,
    title = title,
    body = body,
    iconKey = iconKey,
    triggerAtMillis = triggerAtMillis,
    repeat = RepeatRule.fromKey(repeat),
    leadMinutes = leadMinutes,
    enabled = enabled,
    sound = sound,
    vibrate = vibrate,
)

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    targetType = targetType.name,
    targetId = targetId,
    title = title,
    body = body,
    iconKey = iconKey,
    triggerAtMillis = triggerAtMillis,
    repeat = repeat.name,
    leadMinutes = leadMinutes,
    enabled = enabled,
    sound = sound,
    vibrate = vibrate,
)
