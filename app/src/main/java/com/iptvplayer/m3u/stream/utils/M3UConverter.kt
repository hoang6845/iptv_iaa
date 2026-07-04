package com.iptvplayer.m3u.stream.utils

import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.UUID

suspend fun fetchM3U(
    client: OkHttpClient,
    url: String
): String = withContext(Dispatchers.IO) {

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
        }
        response.body?.string().orEmpty()
    }
}

fun parseM3U(m3u: String): List<Channel> {
    val lines = m3u.lines()
    val channels = mutableListOf<Channel>()

    var currentExtinf: String? = null

    for (line in lines) {
        val l = line.trim()
        if (l.isEmpty()) continue

        if (l.startsWith("#EXTINF")) {
            currentExtinf = l
        } else if (!l.startsWith("#") && currentExtinf != null) {
            val channel = parseExtinf(currentExtinf, l)
            channels.add(channel)
            currentExtinf = null
        }
    }

    return channels
}

fun parseM3UChannelPopular(m3u: String): List<ChannelPopular> {
    val lines = m3u.lines()
    val channels = mutableListOf<ChannelPopular>()

    var currentExtinf: String? = null

    for (line in lines) {
        val l = line.trim()
        if (l.isEmpty()) continue

        if (l.startsWith("#EXTINF")) {
            currentExtinf = l
        } else if (!l.startsWith("#") && currentExtinf != null) {
            val channel = parseExtinfPopular(currentExtinf, l)
            channels.add(channel)
            currentExtinf = null
        }
    }

    return channels
}
fun parseExtinf(extinf: String, streamUrl: String): Channel {
    fun find(attr: String): String? {
        val regex = Regex("""$attr="([^"]+)"""")
        return regex.find(extinf)?.groupValues?.get(1)
    }

    val name = extinf.substringAfter(",").trim()

    return Channel(
        id = find("tvg-id") + UUID.randomUUID().toString(),
        name = name,
        logo = find("tvg-logo"),
        group = find("group-title"),
        url = streamUrl.trim()
    )
}

fun parseExtinfPopular(extinf: String, streamUrl: String): ChannelPopular {
    fun find(attr: String): String? {
        val regex = Regex("""$attr="([^"]+)"""")
        return regex.find(extinf)?.groupValues?.get(1)
    }

    val name = extinf.substringAfter(",").trim()

    return ChannelPopular(
        id = find("tvg-id") + UUID.randomUUID().toString(),
        name = name,
        logo = find("tvg-logo"),
        group = find("group-title"),
        url = streamUrl.trim()
    )
}
