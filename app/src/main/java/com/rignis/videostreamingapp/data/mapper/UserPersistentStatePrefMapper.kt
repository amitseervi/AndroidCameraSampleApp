package com.rignis.videostreamingapp.data.mapper

import com.rignis.videostreamingapp.data.local.UserPersistentStatePref
import com.rignis.videostreamingapp.domain.model.UserPersistentState

object UserPersistentStatePrefMapper {
    fun toEntity(item: UserPersistentState): UserPersistentStatePref {
        return UserPersistentStatePref(item.selectedFolder)
    }

    fun fromEntity(item: UserPersistentStatePref): UserPersistentState {
        return UserPersistentState(item.storageFileTree)
    }
}