package com.example.zionkids.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zionkids.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UgAdminDao {

    // --- reads (for pickers) LIVE ---
    @Query("SELECT * FROM ug_regions ORDER BY regionName")
    fun watchRegions(): Flow<List<UgRegionEntity>>

    @Query("SELECT * FROM ug_districts WHERE regionCode = :regionCode ORDER BY districtName")
    fun watchDistricts(regionCode: String): Flow<List<UgDistrictEntity>>

    @Query("SELECT * FROM ug_counties WHERE districtCode = :districtCode ORDER BY countyName")
    fun watchCounties(districtCode: String): Flow<List<UgCountyEntity>>

    @Query("SELECT * FROM ug_subcounties WHERE countyCode = :countyCode ORDER BY subcountyName")
    fun watchSubcounties(countyCode: String): Flow<List<UgSubCountyEntity>>

    @Query("SELECT * FROM ug_parishes WHERE subcountyCode = :subcountyCode ORDER BY parishName")
    fun watchParishes(subcountyCode: String): Flow<List<UgParishEntity>>

    @Query("SELECT * FROM ug_villages WHERE parishCode = :parishCode ORDER BY villageName")
    fun watchVillages(parishCode: String): Flow<List<UgVillageEntity>>

    // --- seed helpers (keep suspend) ---
    @Query("SELECT COUNT(*) FROM ug_regions")
    suspend fun regionsCount(): Int

    @Query("SELECT COUNT(*) FROM ug_districts")
    suspend fun districtsCount(): Int

    @Query("SELECT COUNT(*) FROM ug_counties")
    suspend fun countiesCount(): Int

    @Query("SELECT COUNT(*) FROM ug_subcounties")
    suspend fun subcountiesCount(): Int

    @Query("SELECT COUNT(*) FROM ug_parishes")
    suspend fun parishesCount(): Int

    @Query("SELECT COUNT(*) FROM ug_villages")
    suspend fun villagesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(items: List<UgRegionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(items: List<UgDistrictEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounties(items: List<UgCountyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcounties(items: List<UgSubCountyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParishes(items: List<UgParishEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillages(items: List<UgVillageEntity>)
}
