package com.rignis.videostreamingapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rignis.videostreamingapp.domain.model.UserPersistentState
import com.rignis.videostreamingapp.domain.repository.UserPrefRepository
import com.rignis.videostreamingapp.ui.CameraCommand.CAPTURE
import com.rignis.videostreamingapp.ui.CameraCommand.RECORD
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CameraCommand {
    RECORD, CAPTURE
}

enum class UiEvent {
    REQUEST_STORAGE_FOLDER,
    START_VIDEO_RECORDING_AND_SAVE,
    START_IMAGE_CAPTURE_AND_SAVE
}

enum class CommandSteps {
    START,
    EXECUTE
}

data class MainScreenState(
    val selectedFolderPath: String? = null,
    val command: CameraCommand? = null,
    val step: CommandSteps? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(private val userPrefRepository: UserPrefRepository) :
    ViewModel() {
    private val mState = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState>
        get() = mState
    private val mViewEvent = MutableSharedFlow<UiEvent>()
    val viewEvent: SharedFlow<UiEvent>
        get() = mViewEvent

    init {
        viewModelScope.launch {
            state.collectLatest { it ->
                when (it.command) {
                    RECORD -> {
                        when (it.step) {
                            CommandSteps.START -> {
                                selectStorageFolder()
                            }

                            CommandSteps.EXECUTE -> {
                                viewModelScope.launch {
                                    mViewEvent.emit(UiEvent.START_VIDEO_RECORDING_AND_SAVE)
                                }
                            }

                            else -> {
                                Log.i("amittest", "Unknown command RECORD")
                            }
                        }
                    }

                    CAPTURE -> {
                        when (it.step) {
                            CommandSteps.START -> {
                                selectStorageFolder()
                            }

                            CommandSteps.EXECUTE -> {
                                viewModelScope.launch {
                                    mViewEvent.emit(UiEvent.START_IMAGE_CAPTURE_AND_SAVE)
                                }
                            }

                            else -> {
                                Log.i("amittest", "Unknown command CAPTURE")
                            }
                        }
                    }

                    else -> {

                    }
                }
            }
        }
        viewModelScope.launch {
            mState.update {
                it.copy(
                    selectedFolderPath = userPrefRepository.getUserState().selectedFolder
                )
            }
        }
    }

    private fun selectStorageFolder() {
        if (mState.value.selectedFolderPath.isNullOrEmpty()) {
            viewModelScope.launch {
                mViewEvent.emit(UiEvent.REQUEST_STORAGE_FOLDER)
            }
        } else {
            viewModelScope.launch {
                mState.update {
                    it.copy(step = CommandSteps.EXECUTE)
                }
            }
        }
    }

    fun startExecutingCommand(cmd: CameraCommand) {
        viewModelScope.launch {
            mState.update {
                it.copy(command = cmd, step = CommandSteps.START)
            }
        }
    }

    fun onStorageFolderSelected(folderUri: String) {
        viewModelScope.launch {
            userPrefRepository.saveUserState(UserPersistentState(selectedFolder = folderUri))
        }
        viewModelScope.launch {
            mState.update {
                it.copy(selectedFolderPath = folderUri)
            }
        }
    }
}