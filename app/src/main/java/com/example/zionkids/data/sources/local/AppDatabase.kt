package com.example.zionkids.data.sources.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.zionkids.data.model.Kid

@Database(entities = [Kid::class],   version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private  var INSTANCE : AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase? {
            if (INSTANCE == null){
                synchronized(AppDatabase::class.java){
                    if (INSTANCE == null){
                        INSTANCE = Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java, "ZionKids"
                                            ).fallbackToDestructiveMigration(false).build()
                    }
                }
            }
            return INSTANCE
        }

    }
}