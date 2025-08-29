// com/example/zionkids/core/di/RepositoryModule.kt
package com.example.zionkids.core.di


import com.example.zionkids.domain.repositories.online.AttendanceRepository
import com.example.zionkids.domain.repositories.online.AttendanceRepositoryImpl
import com.example.zionkids.domain.repositories.online.AuthRepository
import com.example.zionkids.domain.repositories.online.AuthRepositoryImpl
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.example.zionkids.domain.repositories.online.ChildrenRepositoryImpl
import com.example.zionkids.domain.repositories.online.EventsRepository
import com.example.zionkids.domain.repositories.online.EventsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChildrenRepository(impl: ChildrenRepositoryImpl): ChildrenRepository

    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository


}
