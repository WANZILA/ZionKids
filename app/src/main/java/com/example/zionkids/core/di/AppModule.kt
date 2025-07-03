package com.example.zionkids.core.di

import android.content.Context
import com.example.zionChilds.domain.ChildRegistrationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [NetworkModule::class])
class AppModule {
    @Provides
    @Singleton
    fun provideKidRegistrationRepository(
        @ApplicationContext context: Context,
    ): ChildRegistrationRepository = ChildRegistrationRepository(context)
}