package com.rignis.videostreamingapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rignis.videostreamingapp.domain.repository.UserPrefRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainScreenState(
    val selectedFolderPath: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(private val userPrefRepository: UserPrefRepository) :
    ViewModel() {
    val state: StateFlow<MainScreenState>
        get() = userPrefRepository.userState.map { MainScreenState(it.storageDir) }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly, MainScreenState()
        )

    fun onStorageFolderSelected(folderUri: String) {
        viewModelScope.launch {
            userPrefRepository.updateCameraStorageDir(folderUri)
        }
    }

    suspend fun getUserSelectedDir(): String? {
        return userPrefRepository.userState.firstOrNull()?.storageDir
    }
}