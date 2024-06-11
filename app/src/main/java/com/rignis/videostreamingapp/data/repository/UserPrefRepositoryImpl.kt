package com.rignis.videostreamingapp.data.repository

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.rignis.videostreamingapp.data.local.UserPersistentStatePref
import com.rignis.videostreamingapp.data.mapper.UserPersistentStatePrefMapper
import com.rignis.videostreamingapp.domain.model.UserPersistentState
import com.rignis.videostreamingapp.domain.repository.UserPrefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserPrefRepositoryImpl @Inject constructor(app: Application) : UserPrefRepository {
    private val sharedPref = app.getSharedPreferences("user_state", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var mLatestState: UserPersistentState? = null

    override suspend fun saveUserState(state: UserPersistentState) = withContext(Dispatchers.IO) {
        mLatestState = state
        sharedPref.edit()
            .putString(
                "user_state_json",
                gson.toJson(UserPersistentStatePrefMapper.toEntity(state))
            )
            .apply()
    }

    override suspend fun getUserState(): UserPersistentState = withContext(Dispatchers.IO) {
        mLatestState?.let {
            return@withContext it
        }
        val storedJson = sharedPref.getString("user_state_json", null)
        if (!storedJson.isNullOrEmpty()) {
            val entity = gson.fromJson(storedJson, UserPersistentStatePref::class.java)
            return@withContext UserPersistentStatePrefMapper.fromEntity(entity)
        }
        return@withContext UserPersistentState()

    }
}