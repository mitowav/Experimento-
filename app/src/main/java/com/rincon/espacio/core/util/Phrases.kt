package com.rincon.espacio.core.util

import java.time.LocalDate

/**
 * La frase del día.
 *
 * Criterio de escritura: nada de "¡tú puedes!". Ninguna promete resultados,
 * ninguna riñe por no haber hecho suficiente y ninguna exige energía que quizá
 * hoy no tengas. Son frases que quitan peso, no que lo añaden — que es lo que
 * uno necesita al abrir una app de tareas.
 *
 * La elección es determinista por fecha: cambia cada día pero no cada vez que
 * entras, para que no se sienta como una máquina tragaperras.
 */
object Phrases {

    private val all = listOf(
        "Empieza por lo pequeño.",
        "No hace falta hacerlo todo hoy.",
        "Una cosa hecha vale más que diez pensadas.",
        "Lo importante casi nunca es lo urgente.",
        "Puedes parar cuando quieras.",
        "Hoy también cuenta.",
        "Deja algo listo para tu yo de mañana.",
        "Los días tranquilos también son días buenos.",
        "Lo que no cabe hoy, cabe otro día.",
        "Ordenar es una forma de descansar.",
        "Basta con dar el siguiente paso.",
        "Nadie lleva la cuenta más que tú.",
        "Un rato corto también es un rato.",
        "Lo empezado ya pesa menos.",
        "Escríbelo y suéltalo.",
        "Hazlo mal antes que no hacerlo.",
        "Hoy, sólo lo de hoy.",
        "El descanso no es tiempo perdido.",
        "Tu lista trabaja para ti, no al revés.",
        "Avanzar despacio sigue siendo avanzar.",
        "Guarda energía para lo que te importa.",
        "Lo que se repite, se vuelve fácil.",
        "Cierra algo antes de abrir otra cosa.",
        "No pasa nada por cambiar de plan.",
        "Lo hecho, hecho está.",
        "Un poco cada día llega lejos.",
        "Elige tres cosas. Con eso vale.",
        "Si se te olvidó, no era tan grave.",
        "Ponle hora y deja de pensarlo.",
        "Tener el día vacío también se disfruta.",
        "Empezar ya es la mitad.",
        "Sé amable contigo un rato.",
        "Lo difícil, primero y corto.",
        "Mañana también existe.",
        "Cada cosa en su sitio, y ya.",
        "Hoy has abierto la app: eso ya es algo.",
    )

    fun forDate(date: LocalDate = LocalDate.now()): String =
        all[(date.toEpochDay().mod(all.size))]
}
