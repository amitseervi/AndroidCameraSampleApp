package com.rignis.videostreamingapp.di

import com.rignis.videostreamingapp.data.repository.UserPrefRepositoryImpl
import com.rignis.videostreamingapp.domain.repository.UserPrefRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AppBinding {

    @Binds
    fun bindUserPrefRepository(userPrefRepositoryImpl: UserPrefRepositoryImpl): UserPrefRepository
}