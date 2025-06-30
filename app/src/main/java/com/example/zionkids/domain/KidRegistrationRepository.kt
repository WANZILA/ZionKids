package com.example.zionkids.domain

import android.content.Context
import android.content.SharedPreferences
import com.example.zionkids.data.model.Kid
import com.example.zionkids.data.sources.local.AppDatabase
import com.example.zionkids.data.sources.local.KidsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KidRegistrationRepository @Inject constructor(
    val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {}

    private var kidsDao: KidsDao?

    init {
        val appDb = AppDatabase.getDatabase(context)
        kidsDao = appDb?.kidsDao()
    }

    fun fetchAllKids(): Flow<List<Kid>> = flow{
        val kids: List<Kid>?
        withContext(Dispatchers.IO){
            kids = kidsDao?.getAllKids()
        }
        emit(kids!!)
    }

    suspend fun  saveKid(kid: Kid){
        withContext(Dispatchers.IO){
            kidsDao?.insertKid(kid)
        }
    }
}