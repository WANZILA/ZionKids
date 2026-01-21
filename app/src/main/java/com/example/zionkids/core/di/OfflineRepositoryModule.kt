package com.example.zionkids.core.di

import com.example.zionkids.data.repositories.offline.OfflineUgAdminRepositoryImpl
import com.example.zionkids.domain.repositories.offline.OfflineAttendanceRepository
import com.example.zionkids.domain.repositories.offline.OfflineAttendanceRepositoryImpl
import com.example.zionkids.domain.repositories.offline.OfflineChildrenRepository
import com.example.zionkids.domain.repositories.offline.OfflineChildrenRepositoryImpl
import com.example.zionkids.domain.repositories.offline.OfflineEventsRepository
import com.example.zionkids.domain.repositories.offline.OfflineEventsRepositoryImpl
import com.example.zionkids.domain.repositories.offline.OfflineUgAdminRepository
//import com.example.zionkids.domain.repositories.offline.OfflineUgAdminRepositoryImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineRepositoryModule {

//    @Binds @Singleton
//    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOfflineChildrenRepository(impl: OfflineChildrenRepositoryImpl): OfflineChildrenRepository

    @Binds
    @Singleton
    abstract fun bindOfflineEventRepository(impl: OfflineEventsRepositoryImpl): OfflineEventsRepository

    @Binds
    @Singleton
    abstract fun bindOfflineAttendanceRepository(impl: OfflineAttendanceRepositoryImpl): OfflineAttendanceRepository

    @Binds
    @Singleton
    abstract fun bindOfflineUgAdminRepository(
        impl: OfflineUgAdminRepositoryImpl
    ): OfflineUgAdminRepository
//    @Binds
//    @Singleton
//    abstract fun bindOfflineKpiRepository(impl: KpiRepositoryImpl): OfflineEventsRepository

//    @Binds
//    @Singleton
//    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository
//
//    @Binds
//    @Singleton
//    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository
//
//    @Binds
//    @Singleton
//    abstract fun bindUsersRepository(impl: UsersRepositoryImpl): UsersRepository
//
//    @Binds
//    @Singleton
//    abstract fun bindLockedAccountsRepository(impl: LockedAccountsRepositoryImpl): LockedAccountsRepository
//
//
//    @Binds
//    @Singleton
//    abstract fun bindAppUpdateRepository(impl: AppUpdateRepositoryImpl): AppUpdateRepository
//
//    @Binds
//    @Singleton
//    abstract fun bindTechnicalSkillsRepository(impl: TechnicalSkillsRepositoryImpl): TechnicalSkillsRepository
//
//    @Binds
//    @Singleton
//    abstract fun bindStreetsRepository(impl: StreetsRepositoryImpl): StreetsRepository


}
