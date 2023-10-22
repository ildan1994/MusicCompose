package com.example.musictest.data.repository

import android.util.Log
import com.example.musictest.data.ContentResolverHelper
import com.example.musictest.data.local.model.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
const val DataTag = "DataTag"
class AudioRepository
@Inject constructor(private val contextResolverHelper: ContentResolverHelper) {
    suspend fun getAudioData(): List<Audio> = withContext(Dispatchers.IO) {
        Log.d(DataTag, "getAudioData: invoked ")
        contextResolverHelper.getAudio()
    }

}