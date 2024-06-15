package com.rignis.videostreamingapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.rignis.videostreamingapp.data.datastore.serializer.UserPreferenceSerializer
import com.rignis.videostreamingapp.proto.UserPreference

private const val DATA_STORE_FILE_NAME = "user_prefs.pb"

val Context.userPreferenceStore: DataStore<UserPreference> by dataStore(
    DATA_STORE_FILE_NAME,
    serializer = UserPreferenceSerializer
)