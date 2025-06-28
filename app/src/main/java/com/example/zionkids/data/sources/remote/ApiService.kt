package com.example.zionkids.data.sources.remote

import androidx.annotation.Keep
import com.example.zionkids.data.model.Kid
import retrofit2.http.GET

@Keep
interface ApiService {
    @GET("kids")
    suspend fun getKids(): List<Kid>
}