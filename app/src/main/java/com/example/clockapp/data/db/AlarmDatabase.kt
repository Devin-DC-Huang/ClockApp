package com.example.clockapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.clockapp.data.model.Alarm

/**
 * Room database with java.time API support
 */
@Database(entities = [Alarm::class], version = 10, exportSchema = false)
@TypeConverters(LocalDateListConverter::class, IntListConverter::class, InstantConverter::class, SpecialAlarmModeConverter::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        /**
         * Migration from version 8 to 9: Rename isWorkdayAlarm to isSpecialAlarm
         * Using compatible approach for older SQLite versions (pre-3.25.0)
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite versions before 3.25.0 don't support RENAME COLUMN
                // Use table recreation approach instead
                
                // 1. Create new table with updated schema
                database.execSQL("""
                    CREATE TABLE alarms_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        dates TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        ringtone TEXT NOT NULL,
                        vibrate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isSpecialAlarm INTEGER NOT NULL,
                        isRegularAlarm INTEGER NOT NULL,
                        year INTEGER,
                        snoozeEnabled INTEGER NOT NULL,
                        snoozeMinutes INTEGER NOT NULL,
                        repeatDays TEXT NOT NULL,
                        specialAlarmMode TEXT NOT NULL
                    )
                """)
                
                // 2. Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO alarms_new (
                        id, title, hour, minute, dates, isEnabled, ringtone, vibrate, 
                        createdAt, isSpecialAlarm, isRegularAlarm, year, snoozeEnabled, 
                        snoozeMinutes, repeatDays, specialAlarmMode
                    )
                    SELECT 
                        id, title, hour, minute, dates, isEnabled, ringtone, vibrate,
                        createdAt, isWorkdayAlarm, isRegularAlarm, year, snoozeEnabled,
                        snoozeMinutes, repeatDays, specialAlarmMode
                    FROM alarms
                """)
                
                // 3. Drop old table
                database.execSQL("DROP TABLE alarms")
                
                // 4. Rename new table to old name
                database.execSQL("ALTER TABLE alarms_new RENAME TO alarms")
            }
        }

        /**
         * Migration from version 9 to 10: Schema update for isSpecialAlarm field
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes, just version bump for Room identity hash
            }
        }

        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
