package com.rincon.espacio

import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.ThemePalette
import com.rincon.espacio.domain.model.DayItem
import com.rincon.espacio.domain.model.Goal
import com.rincon.espacio.domain.model.GoalStep
import com.rincon.espacio.domain.model.GoalWithSteps
import com.rincon.espacio.domain.model.Habit
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.domain.model.Subtask
import com.rincon.espacio.domain.usecase.DayPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainModelTest {

    @Test
    fun `el titulo de una nota es su primera linea`() {
        val note = Note(text = "Comprar pan\ny leche")
        assertEquals("Comprar pan", note.title)
    }

    @Test
    fun `el progreso de una nota sin subtareas depende de si esta hecha`() {
        assertEquals(0f, NoteWithSubtasks(Note()).progress, 0.001f)
        assertEquals(1f, NoteWithSubtasks(Note(done = true)).progress, 0.001f)
    }

    @Test
    fun `el progreso con subtareas es la fraccion completada`() {
        val item = NoteWithSubtasks(
            note = Note(id = 1),
            subtasks = listOf(
                Subtask(1, 1, "a", done = true),
                Subtask(2, 1, "b", done = false),
                Subtask(3, 1, "c", done = true),
                Subtask(4, 1, "d", done = false),
            ),
        )
        assertEquals(0.5f, item.progress, 0.001f)
    }

    @Test
    fun `un objetivo esta completo solo si tiene pasos y todos estan hechos`() {
        val empty = GoalWithSteps(Goal(id = 1, title = "EP"))
        assertFalse(empty.isComplete)
        assertEquals(0f, empty.progress, 0.001f)

        val done = GoalWithSteps(
            goal = Goal(id = 1, title = "EP"),
            steps = listOf(GoalStep(1, 1, "Mezclar", true), GoalStep(2, 1, "Masterizar", true)),
        )
        assertTrue(done.isComplete)
        assertEquals(1f, done.progress, 0.001f)
    }

    @Test
    fun `el progreso del dia mezcla tareas y habitos`() {
        val plan = DayPlan(
            date = LocalDate.of(2026, 9, 8),
            items = listOf(
                DayItem.TaskItem(Note(id = 1, isTask = true, done = true), 1f),
                DayItem.TaskItem(Note(id = 2, isTask = true, done = false), 0f),
                DayItem.HabitItem(Habit(id = 1, title = "Agua"), done = true, streak = 3),
                DayItem.HabitItem(Habit(id = 2, title = "Leer"), done = false, streak = 0),
            ),
        )
        assertEquals(4, plan.totalCount)
        assertEquals(2, plan.doneCount)
        assertEquals(0.5f, plan.progress, 0.001f)
        assertEquals(2, plan.upcoming.size)
    }

    @Test
    fun `las claves desconocidas caen en un valor por defecto seguro`() {
        assertEquals(Priority.Normal, Priority.fromKey("inventada"))
        assertEquals(Priority.High, Priority.fromKey("High"))
        assertEquals(PaperColor.Butter, PaperColor.fromKey(null))
        assertEquals(ThemePalette.Cream, ThemePalette.fromKey("otra"))
    }

    @Test
    fun `los colores de papel tienen tinta distinta en claro y oscuro`() {
        PaperColor.entries.forEach { color ->
            assertTrue(
                "El color ${color.name} deberia distinguir claro y oscuro",
                color.paper(false) != color.paper(true),
            )
        }
    }
}
