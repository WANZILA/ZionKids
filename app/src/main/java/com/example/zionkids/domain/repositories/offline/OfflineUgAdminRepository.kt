package com.example.zionkids.domain.repositories.offline

import com.example.zionkids.data.model.*
import kotlinx.coroutines.flow.Flow

interface OfflineUgAdminRepository {

    // LIVE picker streams
    fun watchRegions(): Flow<List<UgRegionEntity>>
    fun watchDistricts(regionCode: String): Flow<List<UgDistrictEntity>>
    fun watchCounties(districtCode: String): Flow<List<UgCountyEntity>>
    fun watchSubcounties(countyCode: String): Flow<List<UgSubCountyEntity>>
    fun watchParishes(subcountyCode: String): Flow<List<UgParishEntity>>
    fun watchVillages(parishCode: String): Flow<List<UgVillageEntity>>

    // seed helpers
    suspend fun regionsCount(): Int
    suspend fun districtsCount(): Int
    suspend fun countiesCount(): Int
    suspend fun subcountiesCount(): Int
    suspend fun parishesCount(): Int
    suspend fun villagesCount(): Int

    suspend fun insertRegions(items: List<UgRegionEntity>)
    suspend fun insertDistricts(items: List<UgDistrictEntity>)
    suspend fun insertCounties(items: List<UgCountyEntity>)
    suspend fun insertSubcounties(items: List<UgSubCountyEntity>)
    suspend fun insertParishes(items: List<UgParishEntity>)
    suspend fun insertVillages(items: List<UgVillageEntity>)
}
