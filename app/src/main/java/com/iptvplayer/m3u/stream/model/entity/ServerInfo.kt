package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ServerInfo(
    val url: String,
    val port: String,

    @SerializedName("https_port")
    val httpsPort: String,

    @SerializedName("server_protocol")
    val serverProtocol: String,

    @SerializedName("rtmp_port")
    val rtmpPort: String,

    val timezone: String,

    @SerializedName("timestamp_now")
    val timestampNow: Long,

    @SerializedName("time_now")
    val timeNow: String
)