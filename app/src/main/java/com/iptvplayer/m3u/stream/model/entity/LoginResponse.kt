package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginResponse(
    @SerializedName("user_info")
    val userInfo: UserInfo,

    @SerializedName("server_info")
    val serverInfo: ServerInfo,
    val url: String
)