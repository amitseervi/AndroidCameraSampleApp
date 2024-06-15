package com.rignis.videostreamingapp.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.rignis.videostreamingapp.proto.UserPreference
import java.io.InputStream
import java.io.OutputStream

object UserPreferenceSerializer : Serializer<UserPreference> {
    override val defaultValue: UserPreference
        get() = UserPreference.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreference {
        try {
            return UserPreference.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Can not read proto ", e)
        }
    }

    override suspend fun writeTo(t: UserPreference, output: OutputStream) {
        t.writeTo(output)
    }
}