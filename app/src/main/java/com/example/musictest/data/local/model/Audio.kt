package com.example.musictest.data.local.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Audio(
    val uri: Uri,
    val displayName: String,
    val id:Long,
    val artist: String,
    val data: String,
    val duration: Int,
    val title: String,
    var index : Int = -1
) : Parcelable

data class AudioCategory(
    val text: String,
    val audios: List<Audio>
)




