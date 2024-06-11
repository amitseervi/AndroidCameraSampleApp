package com.rignis.videostreamingapp.domain.repository

import com.rignis.videostreamingapp.domain.model.UserPersistentState

interface UserPrefRepository {
    suspend fun saveUserState(state: UserPersistentState)
    suspend fun getUserState(): UserPersistentState
}