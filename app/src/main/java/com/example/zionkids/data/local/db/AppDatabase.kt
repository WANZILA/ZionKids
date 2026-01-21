package com.example.zionkids.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
//import com.example.zionkids.data.dao.UgAdminDao
import com.example.zionkids.data.local.dao.AttendanceDao
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.local.dao.EventDao
import com.example.zionkids.data.local.dao.KpiDao
import com.example.zionkids.data.local.dao.UgAdminDao
//import com.example.zionkids.data.local.dao.UgAdminDao // /// CHANGED: new DAO
import com.example.zionkids.data.local.entities.KpiCounter
import com.example.zionkids.data.model.Attendance
//import com.example.zionkids.data.local.entities.UgCountyEntity   // /// CHANGED
//import com.example.zionkids.data.local.entities.UgDistrictEntity // /// CHANGED
//import com.example.zionkids.data.local.entities.UgRegionEntity   // /// CHANGED
//import com.example.zionkids.data.local.entities.UgSubCountyEntity// /// CHANGED
//import com.example.zionkids.data.model.Attendance
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.Event
import com.example.zionkids.data.model.UgCountyEntity
import com.example.zionkids.data.model.UgDistrictEntity
import com.example.zionkids.data.model.UgParishEntity
import com.example.zionkids.data.model.UgRegionEntity
import com.example.zionkids.data.model.UgSubCountyEntity
import com.example.zionkids.data.model.UgVillageEntity

@Database(
    entities = [
        Child::class,
        Event::class,
        KpiCounter::class,
        Attendance::class,

        // /// CHANGED: seed tables
        UgRegionEntity::class,
        UgDistrictEntity::class,
        UgCountyEntity::class,
        UgSubCountyEntity::class,
        UgParishEntity::class,
        UgVillageEntity::class,

    ],
    version = 2, // /// CHANGED: 1 -> 2
    exportSchema = false
)
@TypeConverters(TimestampConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun eventDao(): EventDao
    abstract fun kpiDao(): KpiDao
    abstract fun attendanceDao(): AttendanceDao

    // /// CHANGED: new admin dao
    abstract fun ugAdminDao(): UgAdminDao

    companion object {
        // /// CHANGED: create new admin tables + indices (additive)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
        CREATE TABLE IF NOT EXISTS ug_regions (
            regionCode TEXT NOT NULL,
            regionName TEXT NOT NULL,
            PRIMARY KEY(regionCode)
        )
    """.trimIndent())

                db.execSQL("""
        CREATE TABLE IF NOT EXISTS ug_districts (
            districtCode TEXT NOT NULL,
            districtName TEXT NOT NULL,
            regionCode TEXT NOT NULL,
            PRIMARY KEY(districtCode)
        )
    """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_districts_regionCode ON ug_districts(regionCode)")

                db.execSQL("""
        CREATE TABLE IF NOT EXISTS ug_counties (
            countyCode TEXT NOT NULL,
            countyName TEXT NOT NULL,
            districtCode TEXT NOT NULL,
            PRIMARY KEY(countyCode)
        )
    """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_counties_districtCode ON ug_counties(districtCode)")

                db.execSQL("""
        CREATE TABLE IF NOT EXISTS ug_subcounties (
            subCountyCode TEXT NOT NULL,
            subCountyName TEXT NOT NULL,
            countyCode TEXT NOT NULL,
            PRIMARY KEY(subCountyCode)
        )
    """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_subcounties_countyCode ON ug_subcounties(countyCode)")

                db.execSQL("""
        CREATE TABLE IF NOT EXISTS ug_parishes (
            parishCode TEXT NOT NULL,
            parishName TEXT NOT NULL,
            subCountyCode TEXT NOT NULL,
            PRIMARY KEY(parishCode)
        )
    """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_parishes_subCountyCode ON ug_parishes(subCountyCode)")

                db.execSQL("""
        CREATE TABLE IF NOT EXISTS ug_villages (
            villageCode TEXT NOT NULL,
            villageName TEXT NOT NULL,
            parishCode TEXT NOT NULL,
            PRIMARY KEY(villageCode)
        )
    """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_villages_parishCode ON ug_villages(parishCode)")
            }

        }
    }
}

