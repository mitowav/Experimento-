package com.rincon.espacio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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
        fun build(context: Context): RinconDatabase =
            Room.databaseBuilder(context, RinconDatabase::class.java, "rincon.db")
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
