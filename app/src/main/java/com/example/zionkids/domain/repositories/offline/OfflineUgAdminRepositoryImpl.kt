package com.example.zionkids.data.repositories.offline

//import com.example.zionkids.data.dao.UgAdminDao
import com.example.zionkids.data.local.dao.UgAdminDao
import com.example.zionkids.data.model.*
import com.example.zionkids.domain.repositories.offline.OfflineUgAdminRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineUgAdminRepositoryImpl @Inject constructor(
    private val dao: UgAdminDao
) : OfflineUgAdminRepository {

    override fun watchRegions(): Flow<List<UgRegionEntity>> = dao.watchRegions()
    override fun watchDistricts(regionCode: String): Flow<List<UgDistrictEntity>> = dao.watchDistricts(regionCode)
    override fun watchCounties(districtCode: String): Flow<List<UgCountyEntity>> = dao.watchCounties(districtCode)
    override fun watchSubcounties(countyCode: String): Flow<List<UgSubCountyEntity>> = dao.watchSubcounties(countyCode)
    override fun watchParishes(subcountyCode: String): Flow<List<UgParishEntity>> = dao.watchParishes(subcountyCode)
    override fun watchVillages(parishCode: String): Flow<List<UgVillageEntity>> = dao.watchVillages(parishCode)

    override suspend fun regionsCount(): Int = dao.regionsCount()
    override suspend fun districtsCount(): Int = dao.districtsCount()
    override suspend fun countiesCount(): Int = dao.countiesCount()
    override suspend fun subcountiesCount(): Int = dao.subcountiesCount()
    override suspend fun parishesCount(): Int = dao.parishesCount()
    override suspend fun villagesCount(): Int = dao.villagesCount()

    override suspend fun insertRegions(items: List<UgRegionEntity>) = dao.insertRegions(items)
    override suspend fun insertDistricts(items: List<UgDistrictEntity>) = dao.insertDistricts(items)
    override suspend fun insertCounties(items: List<UgCountyEntity>) = dao.insertCounties(items)
    override suspend fun insertSubcounties(items: List<UgSubCountyEntity>) = dao.insertSubcounties(items)
    override suspend fun insertParishes(items: List<UgParishEntity>) = dao.insertParishes(items)
    override suspend fun insertVillages(items: List<UgVillageEntity>) = dao.insertVillages(items)
}
