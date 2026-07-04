package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity
data class XtreamAuth(
    @PrimaryKey(autoGenerate = true)
    val id:Int ,
    val server: String,
    val username: String,
    val password: String,
    val name: String?,
    val urlAvatar: String = "avatar/avatar_1.png",
    val isEnablePasscode: Boolean = false,
    @SerializedName("created_at")
    val createAt: Long? = null,
    @SerializedName("exp_date")
    val expDate: Long? = null
) {
}

