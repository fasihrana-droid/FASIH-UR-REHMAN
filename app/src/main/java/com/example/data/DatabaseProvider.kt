package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: AttendanceDatabase? = null

    fun getDatabase(context: Context): AttendanceDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AttendanceDatabase::class.java,
                "attendance_record_maker_db"
            )
            .fallbackToDestructiveMigration()
            .build()
            instance = db
            db
        }
    }
}
