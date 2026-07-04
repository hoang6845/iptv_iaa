//package com.iptvplayer.m3u.stream.utils
//
//import com.iptvplayer.m3u.stream.model.entity.Channel
//
//fun parseM3U(m3u: String): List<Channel> {
//    val lines = m3u.lines()
//    val channels = mutableListOf<Channel>()
//    var currentExtinf: String? = null
//
//    for (line in lines) {
//        val l = line.trim()
//        if (l.isEmpty()) continue
//
//        when {
//            l.startsWith("#EXTINF") -> currentExtinf = l
//            !l.startsWith("#") && currentExtinf != null -> {
//                channels.add(parseExtinf(currentExtinf, l))
//                currentExtinf = null
//            }
//        }
//    }
//    return channels
//}
//
//private fun parseExtinf(extinf: String, streamUrl: String): Channel {
//    fun find(attr: String): String? =
//        Regex("""$attr="([^"]+)"""").find(extinf)?.groupValues?.get(1)
//
//    return Channel(
//        id = find("tvg-id"),
//        name = extinf.substringAfter(",").trim(),
//        logo = find("tvg-logo"),
//        group = find("group-title"),
//        url = streamUrl.trim()
//    )
//}