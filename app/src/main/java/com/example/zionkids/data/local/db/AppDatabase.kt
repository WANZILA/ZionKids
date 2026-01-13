package com.example.zionkids.data.local.db

//package com.example.zionkids.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zionkids.data.local.dao.AttendanceDao
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.local.dao.EventDao
import com.example.zionkids.data.local.dao.KpiDao
import com.example.zionkids.data.local.entities.KpiCounter
import com.example.zionkids.data.model.Attendance
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.Event


@Database(
    entities = [
        Child::class,
        Event::class,
        KpiCounter::class,
        Attendance::class,
               ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TimestampConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun eventDao(): EventDao

    abstract fun kpiDao(): KpiDao

    abstract  fun attendanceDao(): AttendanceDao

//    /// CHANGED: add migration to evolve existing installs without data loss
//    companion object {
//        val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("ALTER TABLE children ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
//                db.execSQL("ALTER TABLE children ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
//                db.execSQL("ALTER TABLE children ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
//
//                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_isDirty ON children(isDirty)")
//                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_isDeleted ON children(isDeleted)")
//                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_version ON children(version)")
//                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_isDeleted_updatedAt ON children(isDeleted, updatedAt)")
//            }
//        }
//    }
}

