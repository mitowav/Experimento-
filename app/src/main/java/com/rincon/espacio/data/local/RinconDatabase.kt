package com.rincon.espacio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rincon.espacio.data.local.dao.EventDao
import com.rincon.espacio.data.local.dao.GoalDao
import com.rincon.espacio.data.local.dao.HabitDao
import com.rincon.espacio.data.local.dao.NoteDao
import com.rincon.espacio.data.local.dao.ReminderDao
import com.rincon.espacio.data.local.dao.StudyDao
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

@Database(
    entities = [
        NoteEntity::class,
        SubtaskEntity::class,
        GoalEntity::class,
        GoalStepEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        SubjectEntity::class,
        ExamEntity::class,
        StudySessionEntity::class,
        EventEntity::class,
        ReminderEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RinconDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun goalDao(): GoalDao
    abstract fun habitDao(): HabitDao
    abstract fun studyDao(): StudyDao
    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): ReminderDao

    companion object {

        /**
         * v1 → v2.
         *
         * Añade el estilo de papel y, sobre todo, cambia el sistema de
         * coordenadas del escritorio: antes `x` era una fracción del ancho de
         * la pantalla (0..1) y ahora es una posición absoluta en dp dentro de
         * un tablero mucho más grande. Se migra multiplicando, para que quien
         * ya tenga notas colocadas las siga encontrando donde las dejó.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN styleKey TEXT NOT NULL DEFAULT 'Plain'")
                db.execSQL("UPDATE notes SET x = x * 320.0 WHERE x <= 1.0")
            }
        }

        /** v2 → v3: las tareas pueden repetirse. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'Once'")
            }
        }

        fun build(context: Context): RinconDatabase =
            Room.databaseBuilder(context, RinconDatabase::class.java, "rincon.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
