package com.example.zionkids.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zionkids.data.local.dao.AssessmentAnswerDao
import com.example.zionkids.data.local.dao.AssessmentQuestionDao
import com.example.zionkids.data.local.dao.AssessmentTaxonomyDao
//import com.example.zionkids.data.dao.UgAdminDao
import com.example.zionkids.data.local.dao.AttendanceDao
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.local.dao.EventDao
import com.example.zionkids.data.local.dao.KpiDao
import com.example.zionkids.data.local.dao.UgAdminDao
//import com.example.zionkids.data.local.dao.UgAdminDao // /// CHANGED: new DAO
import com.example.zionkids.data.local.entities.KpiCounter
import com.example.zionkids.data.model.AssessmentAnswer
import com.example.zionkids.data.model.AssessmentQuestion
import com.example.zionkids.data.model.AssessmentTaxonomy
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
        AssessmentQuestion::class,
        AssessmentAnswer::class,
        AssessmentTaxonomy::class,

    ],
    version = 3, // /// CHANGED: 1 -> 2
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

    abstract fun assessmentQuestionDao(): AssessmentQuestionDao
    abstract fun assessmentAnswerDao(): AssessmentAnswerDao

    abstract fun assessmentTaxonomyDao(): AssessmentTaxonomyDao

    companion object {
        // /// CHANGED: create new admin tables + indices (additive)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
        CREATE TABLE IF NOT EXISTS ug_regions (
            regionCode TEXT NOT NULL,
            regionName TEXT NOT NULL,
            PRIMARY KEY(regionCode)
        )
    """.trimIndent()
                )

                db.execSQL(
                    """
        CREATE TABLE IF NOT EXISTS ug_districts (
            districtCode TEXT NOT NULL,
            districtName TEXT NOT NULL,
            regionCode TEXT NOT NULL,
            PRIMARY KEY(districtCode)
        )
    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_districts_regionCode ON ug_districts(regionCode)")

                db.execSQL(
                    """
        CREATE TABLE IF NOT EXISTS ug_counties (
            countyCode TEXT NOT NULL,
            countyName TEXT NOT NULL,
            districtCode TEXT NOT NULL,
            PRIMARY KEY(countyCode)
        )
    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_counties_districtCode ON ug_counties(districtCode)")

                db.execSQL(
                    """
        CREATE TABLE IF NOT EXISTS ug_subcounties (
            subCountyCode TEXT NOT NULL,
            subCountyName TEXT NOT NULL,
            countyCode TEXT NOT NULL,
            PRIMARY KEY(subCountyCode)
        )
    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_subcounties_countyCode ON ug_subcounties(countyCode)")

                db.execSQL(
                    """
        CREATE TABLE IF NOT EXISTS ug_parishes (
            parishCode TEXT NOT NULL,
            parishName TEXT NOT NULL,
            subCountyCode TEXT NOT NULL,
            PRIMARY KEY(parishCode)
        )
    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_parishes_subCountyCode ON ug_parishes(subCountyCode)")

                db.execSQL(
                    """
        CREATE TABLE IF NOT EXISTS ug_villages (
            villageCode TEXT NOT NULL,
            villageName TEXT NOT NULL,
            parishCode TEXT NOT NULL,
            PRIMARY KEY(villageCode)
        )
    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_villages_parishCode ON ug_villages(parishCode)")
            }

        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS assessment_questions (
                questionId TEXT NOT NULL,
                category TEXT NOT NULL,
                subCategory TEXT NOT NULL,
                question TEXT NOT NULL,
                indexNum INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isDirty INTEGER NOT NULL,
                isDeleted INTEGER NOT NULL,
                deletedAt INTEGER,
                version INTEGER NOT NULL,
                PRIMARY KEY(questionId)
            )
        """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_category ON assessment_questions(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_subCategory ON assessment_questions(subCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_isActive ON assessment_questions(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_updatedAt ON assessment_questions(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_isDirty ON assessment_questions(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_isDeleted ON assessment_questions(isDeleted)")


                // Optional: index to speed ordering/filtering
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_indexNum ON assessment_questions(indexNum)")
                // If table doesn't exist yet, do nothing safely.
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS assessment_answers (
                answerId TEXT NOT NULL,
                childId TEXT NOT NULL,
                generalId TEXT NOT NULL,
                questionId TEXT NOT NULL,
                category TEXT NOT NULL,
                subCategory TEXT NOT NULL,
                questionSnapshot TEXT,
                answer TEXT NOT NULL,
                score INTEGER NOT NULL,
                notes TEXT NOT NULL,
                enteredByUid TEXT NOT NULL,
                lastEditedByUid TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                isDirty INTEGER NOT NULL,
                isDeleted INTEGER NOT NULL,
                version INTEGER NOT NULL,
                PRIMARY KEY(answerId)
            )
            """.trimIndent()
                )

                // Drop the old unique index if it exists
                db.execSQL("DROP INDEX IF EXISTS uq_assessment_answers_child_session_question")

                // Create the NEW unique index name Room expects now
                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS index_assessment_answers_childId_generalId_questionId
            ON assessment_answers(childId, generalId, questionId)
            """.trimIndent()
                )

                // (Optional) ensure other indices exist (safe IF NOT EXISTS)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_category ON assessment_answers(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_childId ON assessment_answers(childId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_enteredByUid ON assessment_answers(enteredByUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_generalId ON assessment_answers(generalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_isDeleted ON assessment_answers(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_isDirty ON assessment_answers(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_questionId ON assessment_answers(questionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_subCategory ON assessment_answers(subCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_updatedAt ON assessment_answers(updatedAt)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assessment_taxonomy (
                        taxonomyId TEXT NOT NULL,
                        categoryKey TEXT NOT NULL,
                        categoryLabel TEXT NOT NULL,
                        subCategoryKey TEXT NOT NULL,
                        subCategoryLabel TEXT NOT NULL,
                        indexNum INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,
                        version INTEGER NOT NULL,
                        PRIMARY KEY(taxonomyId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_categoryKey ON assessment_taxonomy(categoryKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_isActive ON assessment_taxonomy(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_updatedAt ON assessment_taxonomy(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_isDirty ON assessment_taxonomy(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_isDeleted ON assessment_taxonomy(isDeleted)")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_assessment_taxonomy_categoryKey_subCategoryKey
                    ON assessment_taxonomy(categoryKey, subCategoryKey)
                    """.trimIndent()
                )

                // /// ADDED: ensure assessment_questions has categoryKey/subCategoryKey
                db.execSQL("ALTER TABLE assessment_questions ADD COLUMN categoryKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assessment_questions ADD COLUMN subCategoryKey TEXT NOT NULL DEFAULT ''")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_categoryKey ON assessment_questions(categoryKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_subCategoryKey ON assessment_questions(subCategoryKey)")


            }

        }
    }
}

