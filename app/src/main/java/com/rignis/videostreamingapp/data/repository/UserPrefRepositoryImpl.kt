package com.rignis.videostreamingapp.data.repository

import android.app.Application
import com.rignis.videostreamingapp.data.datastore.userPreferenceStore
import com.rignis.videostreamingapp.domain.model.UserPreference
import com.rignis.videostreamingapp.domain.repository.UserPrefRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPrefRepositoryImpl @Inject constructor(app: Application) : UserPrefRepository {
    private val dataStore = app.userPreferenceStore
    override val userState: Flow<UserPreference>
        get() = dataStore.data.map { UserPreference(it.cameraStorageDir) }

    override suspend fun updateCameraStorageDir(dir: String?) {
        dataStore.updateData {
            it.toBuilder().setCameraStorageDir(dir).build()
        }
    }
}