package com.rincon.espacio.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Familia de iconos propia de Rincón.
 *
 * Todos comparten rejilla de 24, trazo de 1.9 y terminaciones redondeadas para
 * que se lean como un conjunto y no como iconos prestados de sitios distintos.
 * Se dibujan como [ImageVector] en código: cero PNGs, escalan a cualquier
 * densidad y se tiñen con el color del tema.
 */
object RinconIcons {

    private const val W = 1.9f

    private fun build(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = W,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Home: ImageVector by lazy {
        build(
            "Home",
            "M3.4 11.4 L12 4.2 L20.6 11.4",
            "M5.6 10.2 L5.6 18.4 A2 2 0 0 0 7.6 20.4 L16.4 20.4 A2 2 0 0 0 18.4 18.4 L18.4 10.2",
            "M9.8 20.4 L9.8 15.2 A2.2 2.2 0 0 1 14.2 15.2 L14.2 20.4",
        )
    }

    val Sun: ImageVector by lazy {
        build(
            "Sun",
            "M16 12 A4 4 0 1 1 8 12 A4 4 0 1 1 16 12",
            "M12 3.2 L12 5",
            "M12 19 L12 20.8",
            "M3.2 12 L5 12",
            "M19 12 L20.8 12",
            "M5.8 5.8 L7.1 7.1",
            "M16.9 16.9 L18.2 18.2",
            "M18.2 5.8 L16.9 7.1",
            "M7.1 16.9 L5.8 18.2",
        )
    }

    val Moon: ImageVector by lazy {
        build("Moon", "M20 14.6 A8.3 8.3 0 1 1 10.1 4.2 A6.7 6.7 0 0 0 20 14.6 Z")
    }

    val Calendar: ImageVector by lazy {
        build(
            "Calendar",
            "M4.6 7 A2 2 0 0 1 6.6 5 L17.4 5 A2 2 0 0 1 19.4 7 L19.4 18 A2 2 0 0 1 17.4 20 L6.6 20 A2 2 0 0 1 4.6 18 Z",
            "M4.6 9.6 L19.4 9.6",
            "M8.6 3.4 L8.6 6.4",
            "M15.4 3.4 L15.4 6.4",
            "M8.6 14 L10.7 16.1 L15.4 11.6",
        )
    }

    val Target: ImageVector by lazy {
        build(
            "Target",
            "M12 4.6 A7.4 7.4 0 1 0 12 19.4 A7.4 7.4 0 1 0 12 4.6",
            "M12 8.4 A3.6 3.6 0 1 0 12 15.6 A3.6 3.6 0 1 0 12 8.4",
            "M11.95 12 L12.05 12",
        )
    }

    val Note: ImageVector by lazy {
        build(
            "Note",
            "M5.2 6 A2 2 0 0 1 7.2 4 L14.4 4 L18.8 8.4 L18.8 18 A2 2 0 0 1 16.8 20 L7.2 20 A2 2 0 0 1 5.2 18 Z",
            "M14.4 4.2 L14.4 8.4 L18.6 8.4",
            "M8.6 12.6 L14.4 12.6",
            "M8.6 16.1 L12.4 16.1",
        )
    }

    val Check: ImageVector by lazy { build("Check", "M5 12.6 L9.8 17.4 L19 7.4") }

    val Plus: ImageVector by lazy { build("Plus", "M12 5.4 L12 18.6", "M5.4 12 L18.6 12") }

    val Close: ImageVector by lazy { build("Close", "M6.6 6.6 L17.4 17.4", "M17.4 6.6 L6.6 17.4") }

    val Clock: ImageVector by lazy {
        build(
            "Clock",
            "M12 4.5 A7.5 7.5 0 1 0 12 19.5 A7.5 7.5 0 1 0 12 4.5",
            "M12 8 L12 12.3 L15.1 14.2",
        )
    }

    val Bell: ImageVector by lazy {
        build(
            "Bell",
            "M7 16.4 L7 11.4 A5 5 0 0 1 17 11.4 L17 16.4 L18.4 18.4 L5.6 18.4 Z",
            "M10.1 18.4 A2 2 0 0 0 13.9 18.4",
            "M12 4.2 L12 6.4",
        )
    }

    val Book: ImageVector by lazy {
        build(
            "Book",
            "M6 4.6 L17 4.6 A1.5 1.5 0 0 1 18.5 6.1 L18.5 19.4 L7.5 19.4 A1.5 1.5 0 0 1 6 17.9 Z",
            "M6 16.4 L18.5 16.4",
            "M9.4 8.6 L15.1 8.6",
            "M9.4 11.8 L13.1 11.8",
        )
    }

    val Bulb: ImageVector by lazy {
        build(
            "Bulb",
            "M12 3.6 A5.4 5.4 0 0 0 8.7 13.2 A2.4 2.4 0 0 1 9.6 15.1 L9.6 16.4 L14.4 16.4 L14.4 15.1 A2.4 2.4 0 0 1 15.3 13.2 A5.4 5.4 0 0 0 12 3.6 Z",
            "M10.1 19 L13.9 19",
            "M10.8 21 L13.2 21",
        )
    }

    val Heart: ImageVector by lazy {
        build(
            "Heart",
            "M12 19.6 C12 19.6 4 15.1 4 9.9 A4.3 4.3 0 0 1 12 7.7 A4.3 4.3 0 0 1 20 9.9 C20 15.1 12 19.6 12 19.6 Z",
        )
    }

    val Music: ImageVector by lazy {
        build(
            "Music",
            "M9.3 16.4 L9.3 7.2 L18.8 5.2 L18.8 14.4",
            "M9.3 16.4 A2.3 2.3 0 1 1 4.7 16.4 A2.3 2.3 0 1 1 9.3 16.4",
            "M18.8 14.4 A2.3 2.3 0 1 1 14.2 14.4 A2.3 2.3 0 1 1 18.8 14.4",
        )
    }

    val Coffee: ImageVector by lazy {
        build(
            "Coffee",
            "M5.6 8.8 L16.4 8.8 L16.4 15 A4 4 0 0 1 12.4 19 L9.6 19 A4 4 0 0 1 5.6 15 Z",
            "M16.4 10.2 L17.9 10.2 A2.5 2.5 0 0 1 17.9 15.2 L16.4 15.2",
            "M9.2 3.6 C9.2 4.9 10.2 4.9 10.2 6.2",
            "M13.2 3.6 C13.2 4.9 14.2 4.9 14.2 6.2",
        )
    }

    val Leaf: ImageVector by lazy {
        build(
            "Leaf",
            "M5 19 C5 19 4.1 10.2 9.5 6.6 C13.9 3.8 19 5.1 19 5.1 C19 5.1 19.4 12.1 14.5 15.5 C10.6 18.2 5 19 5 19 Z",
            "M5.6 18.5 C9 15.1 12.1 12.1 16.4 8.6",
        )
    }

    val Drop: ImageVector by lazy {
        build(
            "Drop",
            "M12 3.6 C12 3.6 5.6 11 5.6 14.7 A6.4 6.4 0 0 0 18.4 14.7 C18.4 11 12 3.6 12 3.6 Z",
        )
    }

    val Flame: ImageVector by lazy {
        build(
            "Flame",
            "M12 3.2 C12 3.2 13.6 6.6 11.6 9.1 C10.1 11 7.1 11.6 7.1 15 A4.9 4.9 0 0 0 16.9 15 C16.9 11.1 13.5 9.6 12 3.2 Z",
        )
    }

    val Sparkle: ImageVector by lazy {
        build(
            "Sparkle",
            "M11 4.4 L12.5 9.9 L18 11.4 L12.5 12.9 L11 18.4 L9.5 12.9 L4 11.4 L9.5 9.9 Z",
            "M18.2 15.4 L18.8 17.4 L20.8 18 L18.8 18.6 L18.2 20.6 L17.6 18.6 L15.6 18 L17.6 17.4 Z",
        )
    }

    val Pencil: ImageVector by lazy {
        build(
            "Pencil",
            "M4.6 19.4 L4.6 15.9 L15.5 5 A2.1 2.1 0 0 1 19 8.5 L8.1 19.4 Z",
            "M13.6 6.9 L17.1 10.4",
        )
    }

    val Trash: ImageVector by lazy {
        build(
            "Trash",
            "M5 7.2 L19 7.2",
            "M9.6 7.2 L9.6 5.4 A1.2 1.2 0 0 1 10.8 4.2 L13.2 4.2 A1.2 1.2 0 0 1 14.4 5.4 L14.4 7.2",
            "M6.9 7.2 L7.8 18.8 A1.7 1.7 0 0 0 9.5 20.4 L14.5 20.4 A1.7 1.7 0 0 0 16.2 18.8 L17.1 7.2",
            "M10.6 10.8 L10.9 16.8",
            "M13.4 10.8 L13.1 16.8",
        )
    }

    val ChevronLeft: ImageVector by lazy { build("ChevronLeft", "M14.6 6 L8.6 12 L14.6 18") }
    val ChevronRight: ImageVector by lazy { build("ChevronRight", "M9.4 6 L15.4 12 L9.4 18") }
    val ChevronDown: ImageVector by lazy { build("ChevronDown", "M6 9.6 L12 15.6 L18 9.6") }
    val ChevronUp: ImageVector by lazy { build("ChevronUp", "M6 14.4 L12 8.4 L18 14.4") }

    val Person: ImageVector by lazy {
        build(
            "Person",
            "M12 11.6 A3.7 3.7 0 1 0 12 4.2 A3.7 3.7 0 1 0 12 11.6",
            "M5 20.2 A7 7 0 0 1 19 20.2",
        )
    }

    val Palette: ImageVector by lazy {
        build(
            "Palette",
            "M12 3.6 A8.4 8.4 0 1 0 12 20.4 A2 2 0 0 0 13.4 17 A1.6 1.6 0 0 1 14.7 14.5 L16.5 14.5 A4 4 0 0 0 20.4 10.5 A7.5 7.5 0 0 0 12 3.6 Z",
            "M8.6 9.2 L8.7 9.2",
            "M12 7.4 L12.1 7.4",
            "M15.4 9.2 L15.5 9.2",
            "M8 13.4 L8.1 13.4",
        )
    }

    val Speaker: ImageVector by lazy {
        build(
            "Speaker",
            "M4.8 9.6 L8.4 9.6 L12.8 5.6 L12.8 18.4 L8.4 14.4 L4.8 14.4 Z",
            "M15.8 9.6 A4.2 4.2 0 0 1 15.8 14.4",
            "M18.2 7.2 A7.6 7.6 0 0 1 18.2 16.8",
        )
    }

    val Vibrate: ImageVector by lazy {
        build(
            "Vibrate",
            "M8.2 5.6 A1.6 1.6 0 0 1 9.8 4 L14.2 4 A1.6 1.6 0 0 1 15.8 5.6 L15.8 18.4 A1.6 1.6 0 0 1 14.2 20 L9.8 20 A1.6 1.6 0 0 1 8.2 18.4 Z",
            "M4.8 9.6 L4.8 14.4",
            "M19.2 9.6 L19.2 14.4",
        )
    }

    val Graduation: ImageVector by lazy {
        build(
            "Graduation",
            "M12 4.2 L21.4 8.6 L12 13 L2.6 8.6 Z",
            "M6.6 10.7 L6.6 15.5 C6.6 17.4 9 19 12 19 C15 19 17.4 17.4 17.4 15.5 L17.4 10.7",
        )
    }

    val Tasks: ImageVector by lazy {
        build(
            "Tasks",
            "M4.6 7.4 L6.6 9.4 L9.6 5.8",
            "M4.6 15.4 L6.6 17.4 L9.6 13.8",
            "M12.6 7.4 L19.4 7.4",
            "M12.6 15.4 L19.4 15.4",
        )
    }

    val Bolt: ImageVector by lazy {
        build("Bolt", "M13.4 3.2 L6.2 13.4 L11.4 13.4 L10.6 20.8 L17.8 10.6 L12.6 10.6 Z")
    }

    val Sliders: ImageVector by lazy {
        build(
            "Sliders",
            "M6 4.4 L6 8.6", "M6 13.4 L6 19.6", "M8 11 A2 2 0 1 1 4 11 A2 2 0 1 1 8 11",
            "M12 4.4 L12 12.6", "M12 17.4 L12 19.6", "M14 15 A2 2 0 1 1 10 15 A2 2 0 1 1 14 15",
            "M18 4.4 L18 6.6", "M18 11.4 L18 19.6", "M20 9 A2 2 0 1 1 16 9 A2 2 0 1 1 20 9",
        )
    }

    val Grip: ImageVector by lazy { build("Grip", "M8 10 L16 10", "M8 14 L16 14") }

    val Undo: ImageVector by lazy {
        build(
            "Undo",
            "M4.6 8.6 L9 8.6 L9 4.2",
            "M4.9 8.4 A7.6 7.6 0 1 1 6.4 16.4",
        )
    }

    /** Iconos disponibles al etiquetar una nota, tarea, objetivo o hábito. */
    val catalog: List<Pair<String, ImageVector>> by lazy {
        listOf(
            "note" to Note,
            "check" to Check,
            "book" to Book,
            "graduation" to Graduation,
            "target" to Target,
            "bulb" to Bulb,
            "heart" to Heart,
            "music" to Music,
            "bolt" to Bolt,
            "coffee" to Coffee,
            "leaf" to Leaf,
            "drop" to Drop,
            "flame" to Flame,
            "sparkle" to Sparkle,
            "calendar" to Calendar,
            "bell" to Bell,
            "sun" to Sun,
            "moon" to Moon,
        )
    }

    private val byKey: Map<String, ImageVector> by lazy { catalog.toMap() }

    fun byKey(key: String?): ImageVector = byKey[key] ?: Note

    /** Etiqueta accesible para lectores de pantalla. */
    fun labelFor(key: String?): String = when (key) {
        "check" -> "Tarea"
        "book" -> "Estudio"
        "graduation" -> "Examen"
        "target" -> "Objetivo"
        "bulb" -> "Idea"
        "heart" -> "Personal"
        "music" -> "Música"
        "bolt" -> "Actividad"
        "coffee" -> "Descanso"
        "leaf" -> "Hábito"
        "drop" -> "Agua"
        "flame" -> "Racha"
        "sparkle" -> "Destacado"
        "calendar" -> "Calendario"
        "bell" -> "Recordatorio"
        "sun" -> "Mañana"
        "moon" -> "Noche"
        else -> "Nota"
    }
}
