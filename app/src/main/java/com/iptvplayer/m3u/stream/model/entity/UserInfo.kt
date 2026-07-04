package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UserInfo(
    val username: String,
    val password: String,
    val message: String,

    val auth: Int,
    val status: String,

    @SerializedName("exp_date")
    val expDate: String,

    @SerializedName("is_trial")
    val isTrial: String,

    @SerializedName("active_cons")
    val activeCons: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("max_connections")
    val maxConnections: String,

    @SerializedName("allowed_output_formats")
    val allowedOutputFormats: List<String>
)