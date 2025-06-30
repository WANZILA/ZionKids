package com.example.zionkids.data.sources.local

import androidx.room.*
import com.example.zionkids.data.model.Kid

@Dao
interface KidsDao{
    @Query("SELECT * FROM kids")
    fun getAllKids(): List<Kid>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertKid(kid: Kid)
}