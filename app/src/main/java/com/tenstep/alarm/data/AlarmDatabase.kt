package com.tenstep.alarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlarmEntity::class], version = 2, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        /** v1 -> v2: add the per-alarm snooze switch. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN snoozeEnabled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun build(context: Context): AlarmDatabase =
            Room.databaseBuilder(context, AlarmDatabase::class.java, "tenstep_alarm.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}