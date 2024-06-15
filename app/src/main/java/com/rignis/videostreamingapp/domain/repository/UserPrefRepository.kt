package com.rignis.videostreamingapp.domain.repository

import com.rignis.videostreamingapp.domain.model.UserPreference
import kotlinx.coroutines.flow.Flow

interface UserPrefRepository {
    val userState: Flow<UserPreference>
    suspend fun updateCameraStorageDir(dir: String?)
}