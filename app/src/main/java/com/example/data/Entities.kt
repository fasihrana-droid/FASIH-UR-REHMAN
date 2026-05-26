package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val identifier: String = ""
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val title: String,
    val dateLong: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_values")
data class AttendanceValueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val memberId: Int,
    val status: String, // "PRESENT", "ABSENT", "LATE", "EXCUSED"
    val notes: String = ""
)
