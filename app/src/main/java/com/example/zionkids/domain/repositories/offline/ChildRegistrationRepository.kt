package com.example.zionkids.domain.repositories.offline

import android.content.Context
import android.content.SharedPreferences
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.sources.local.AppDatabase
import com.example.zionkids.data.sources.local.ChildrenDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRegistrationRepository @Inject constructor(
    val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {}

    private var childrenDao: ChildrenDao?

    init {
        val appDb = AppDatabase.Companion.getDatabase(context)
        childrenDao = appDb?.childrenDao()
    }

    fun fetchAllChilds(): Flow<List<Child>> = flow {
        val Childs: List<Child>?
        withContext(Dispatchers.IO) {
            Childs = childrenDao?.getAllChildren()
        }
        emit(Childs!!)
    }

    suspend fun  saveChild(Child: Child){
        withContext(Dispatchers.IO) {
            childrenDao?.insertChild(Child)
        }
    }
}