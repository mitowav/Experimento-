package com.rincon.espacio.data.repo

import com.rincon.espacio.data.local.RinconDatabase
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * Copia de seguridad.
 *
 * Los datos son de quien los escribe: tiene que poder llevárselos. El formato
 * es JSON legible a propósito —se puede abrir con cualquier editor y entender
 * qué hay dentro— y conserva los identificadores originales, que es lo que
 * mantiene intactas las relaciones entre notas, objetivos, asignaturas y
 * avisos al restaurar.
 *
 * Restaurar **reemplaza**, no mezcla: repetir una copia dos veces no debe
 * dejarte con todo duplicado.
 */
class BackupRepository(
    private val database: RinconDatabase,
    private val reminders: ReminderRepository,
) {

    suspend fun export(): String {
        val notes = database.noteDao()
        val goals = database.goalDao()
        val habits = database.habitDao()
        val study = database.studyDao()
        val events = database.eventDao()
        val reminderDao = database.reminderDao()

        val root = JSONObject()
        root.put("formato", FORMAT_VERSION)
        root.put("app", "Rincón")
        root.put("exportado", System.currentTimeMillis())

        root.put("notas", notes.allNotes().toJson(::noteToJson))
        root.put("pasos", notes.allSubtasks().toJson(::subtaskToJson))
        root.put("objetivos", goals.allGoals().toJson(::goalToJson))
        root.put("pasos_objetivo", goals.allSteps().toJson(::goalStepToJson))
        root.put("habitos", habits.allHabits().toJson(::habitToJson))
        root.put("marcas_habito", habits.allChecks().toJson(::habitCheckToJson))
        root.put("asignaturas", study.allSubjects().toJson(::subjectToJson))
        root.put("examenes", study.allExams().toJson(::examToJson))
        root.put("sesiones", study.allSessions().toJson(::sessionToJson))
        root.put("eventos", events.allEvents().toJson(::eventToJson))
        root.put("avisos", reminderDao.allReminders().toJson(::reminderToJson))

        return root.toString(2)
    }

    /** Devuelve cuántos elementos se han restaurado, o lanza si el archivo no vale. */
    suspend fun import(json: String): Int {
        val root = JSONObject(json)
        require(root.optString("app") == "Rincón") { "Este archivo no es una copia de Rincón." }

        val noteDao = database.noteDao()
        val goalDao = database.goalDao()
        val habitDao = database.habitDao()
        val studyDao = database.studyDao()
        val eventDao = database.eventDao()
        val reminderDao = database.reminderDao()

        // Se vacía primero: las tablas con clave foránea arrastran a sus hijas.
        noteDao.clearNotes()
        goalDao.clearGoals()
        habitDao.clearHabits()
        studyDao.clearSessions()
        studyDao.clearSubjects()
        eventDao.clearEvents()
        reminderDao.clearReminders()

        var restored = 0

        root.array("avisos").forEach { reminderDao.insert(it.toReminder()); restored++ }
        root.array("objetivos").forEach { goalDao.insert(it.toGoal()); restored++ }
        root.array("pasos_objetivo").forEach { goalDao.insertStep(it.toGoalStep()); restored++ }
        root.array("asignaturas").forEach { studyDao.insertSubject(it.toSubject()); restored++ }
        root.array("examenes").forEach { studyDao.insertExam(it.toExam()); restored++ }
        root.array("sesiones").forEach { studyDao.insertSession(it.toSession()); restored++ }
        root.array("habitos").forEach { habitDao.insert(it.toHabit()); restored++ }
        root.array("marcas_habito").forEach { habitDao.check(it.toHabitCheck()); restored++ }
        root.array("eventos").forEach { eventDao.insert(it.toEvent()); restored++ }
        root.array("notas").forEach { noteDao.insert(it.toNote()); restored++ }
        root.array("pasos").forEach { noteDao.insertSubtask(it.toSubtask()); restored++ }

        // Las alarmas del sistema no viven en la base de datos: hay que
        // volver a programarlas todas después de restaurar.
        reminders.rescheduleAll()
        return restored
    }

    private companion object {
        const val FORMAT_VERSION = 1
    }
}

// --- Serialización ------------------------------------------------------------

private fun <T> List<T>.toJson(mapper: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(mapper(it)) } }

private fun JSONObject.array(name: String): List<JSONObject> {
    val array = optJSONArray(name) ?: return emptyList()
    return (0 until array.length()).map { array.getJSONObject(it) }
}

private fun JSONObject.longOrNull(name: String): Long? =
    if (isNull(name)) null else optLong(name)

private fun JSONObject.intOrNull(name: String): Int? =
    if (isNull(name)) null else optInt(name)

private fun noteToJson(e: NoteEntity) = JSONObject().apply {
    put("id", e.id); put("texto", e.text); put("color", e.colorKey); put("icono", e.iconKey)
    put("estilo", e.styleKey); put("x", e.x.toDouble()); put("y", e.y.toDouble())
    put("rotacion", e.rotation.toDouble()); put("escala", e.scale.toDouble()); put("capa", e.zIndex)
    put("esTarea", e.isTask); put("hecha", e.done); put("fijada", e.pinned)
    put("prioridad", e.priority); put("fecha", e.dueDate); put("hora", e.dueTime)
    put("objetivo", e.goalId); put("asignatura", e.subjectId); put("aviso", e.reminderId)
    put("repite", e.repeatRule)
    put("creada", e.createdAt); put("modificada", e.updatedAt); put("archivada", e.archived)
}

private fun JSONObject.toNote() = NoteEntity(
    id = optLong("id"), text = optString("texto"), colorKey = optString("color"),
    iconKey = optString("icono"), styleKey = optString("estilo", "Plain"),
    x = optDouble("x", 0.0).toFloat(), y = optDouble("y", 0.0).toFloat(),
    rotation = optDouble("rotacion", 0.0).toFloat(), scale = optDouble("escala", 1.0).toFloat(),
    zIndex = optInt("capa"), isTask = optBoolean("esTarea"), done = optBoolean("hecha"),
    pinned = optBoolean("fijada"), priority = optString("prioridad", "Normal"),
    dueDate = longOrNull("fecha"), dueTime = intOrNull("hora"),
    goalId = longOrNull("objetivo"), subjectId = longOrNull("asignatura"),
    reminderId = longOrNull("aviso"), repeatRule = optString("repite", "Once"),
    createdAt = optLong("creada"),
    updatedAt = optLong("modificada"), archived = optBoolean("archivada"),
)

private fun subtaskToJson(e: SubtaskEntity) = JSONObject().apply {
    put("id", e.id); put("nota", e.noteId); put("texto", e.text)
    put("hecho", e.done); put("orden", e.position)
}

private fun JSONObject.toSubtask() = SubtaskEntity(
    id = optLong("id"), noteId = optLong("nota"), text = optString("texto"),
    done = optBoolean("hecho"), position = optInt("orden"),
)

private fun goalToJson(e: GoalEntity) = JSONObject().apply {
    put("id", e.id); put("titulo", e.title); put("nota", e.note); put("color", e.colorKey)
    put("icono", e.iconKey); put("fecha", e.targetDate); put("archivado", e.archived)
    put("creado", e.createdAt); put("orden", e.position)
}

private fun JSONObject.toGoal() = GoalEntity(
    id = optLong("id"), title = optString("titulo"), note = optString("nota"),
    colorKey = optString("color"), iconKey = optString("icono"),
    targetDate = longOrNull("fecha"), archived = optBoolean("archivado"),
    createdAt = optLong("creado"), position = optInt("orden"),
)

private fun goalStepToJson(e: GoalStepEntity) = JSONObject().apply {
    put("id", e.id); put("objetivo", e.goalId); put("titulo", e.title)
    put("hecho", e.done); put("orden", e.position)
}

private fun JSONObject.toGoalStep() = GoalStepEntity(
    id = optLong("id"), goalId = optLong("objetivo"), title = optString("titulo"),
    done = optBoolean("hecho"), position = optInt("orden"),
)

private fun habitToJson(e: HabitEntity) = JSONObject().apply {
    put("id", e.id); put("titulo", e.title); put("color", e.colorKey); put("icono", e.iconKey)
    put("cadencia", e.cadence); put("veces", e.timesPerWeek); put("hora", e.reminderTime)
    put("aviso", e.reminderId); put("archivado", e.archived); put("creado", e.createdAt)
    put("orden", e.position)
}

private fun JSONObject.toHabit() = HabitEntity(
    id = optLong("id"), title = optString("titulo"), colorKey = optString("color"),
    iconKey = optString("icono"), cadence = optString("cadencia", "Daily"),
    timesPerWeek = optInt("veces", 3), reminderTime = intOrNull("hora"),
    reminderId = longOrNull("aviso"), archived = optBoolean("archivado"),
    createdAt = optLong("creado"), position = optInt("orden"),
)

private fun habitCheckToJson(e: HabitCheckEntity) = JSONObject().apply {
    put("id", e.id); put("habito", e.habitId); put("dia", e.date)
}

private fun JSONObject.toHabitCheck() = HabitCheckEntity(
    id = optLong("id"), habitId = optLong("habito"), date = optLong("dia"),
)

private fun subjectToJson(e: SubjectEntity) = JSONObject().apply {
    put("id", e.id); put("nombre", e.name); put("color", e.colorKey); put("icono", e.iconKey)
    put("profesor", e.teacher); put("archivada", e.archived); put("orden", e.position)
}

private fun JSONObject.toSubject() = SubjectEntity(
    id = optLong("id"), name = optString("nombre"), colorKey = optString("color"),
    iconKey = optString("icono"), teacher = optString("profesor"),
    archived = optBoolean("archivada"), position = optInt("orden"),
)

private fun examToJson(e: ExamEntity) = JSONObject().apply {
    put("id", e.id); put("asignatura", e.subjectId); put("titulo", e.title)
    put("dia", e.date); put("hora", e.time); put("preparacion", e.readiness)
    put("aviso", e.reminderId)
}

private fun JSONObject.toExam() = ExamEntity(
    id = optLong("id"), subjectId = optLong("asignatura"), title = optString("titulo"),
    date = optLong("dia"), time = intOrNull("hora"), readiness = optInt("preparacion"),
    reminderId = longOrNull("aviso"),
)

private fun sessionToJson(e: StudySessionEntity) = JSONObject().apply {
    put("id", e.id); put("asignatura", e.subjectId); put("dia", e.date)
    put("inicio", e.startTime); put("minutos", e.minutes); put("nota", e.note)
    put("hecha", e.completed); put("aviso", e.reminderId)
}

private fun JSONObject.toSession() = StudySessionEntity(
    id = optLong("id"), subjectId = longOrNull("asignatura"), date = optLong("dia"),
    startTime = optInt("inicio"), minutes = optInt("minutos"), note = optString("nota"),
    completed = optBoolean("hecha"), reminderId = longOrNull("aviso"),
)

private fun eventToJson(e: EventEntity) = JSONObject().apply {
    put("id", e.id); put("titulo", e.title); put("dia", e.date); put("inicio", e.startTime)
    put("fin", e.endTime); put("color", e.colorKey); put("icono", e.iconKey)
    put("nota", e.note); put("aviso", e.reminderId)
}

private fun JSONObject.toEvent() = EventEntity(
    id = optLong("id"), title = optString("titulo"), date = optLong("dia"),
    startTime = intOrNull("inicio"), endTime = intOrNull("fin"),
    colorKey = optString("color"), iconKey = optString("icono"),
    note = optString("nota"), reminderId = longOrNull("aviso"),
)

private fun reminderToJson(e: ReminderEntity) = JSONObject().apply {
    put("id", e.id); put("tipo", e.targetType); put("destino", e.targetId)
    put("titulo", e.title); put("cuerpo", e.body); put("icono", e.iconKey)
    put("cuando", e.triggerAtMillis); put("repite", e.repeat); put("antes", e.leadMinutes)
    put("activo", e.enabled); put("sonido", e.sound); put("vibra", e.vibrate)
}

private fun JSONObject.toReminder() = ReminderEntity(
    id = optLong("id"), targetType = optString("tipo", "Note"), targetId = optLong("destino"),
    title = optString("titulo"), body = optString("cuerpo"), iconKey = optString("icono", "bell"),
    triggerAtMillis = optLong("cuando"), repeat = optString("repite", "Once"),
    leadMinutes = optInt("antes"), enabled = optBoolean("activo", true),
    sound = optBoolean("sonido", true), vibrate = optBoolean("vibra", true),
)
