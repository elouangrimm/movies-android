package xyz.elouan.movies.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "movies_prefs"
private const val KEY_WATCHED = "recently_watched"
private const val MAX_ITEMS = 20

object RecentlyWatched {

    fun add(context: Context, item: WatchedItem) {
        val list = getAll(context).toMutableList()
        // Remove existing entry for same imdbId+season+episode
        list.removeAll { it.imdbId == item.imdbId && it.season == item.season && it.episode == item.episode }
        list.add(0, item)
        if (list.size > MAX_ITEMS) list.subList(MAX_ITEMS, list.size).clear()
        save(context, list)
    }

    fun getAll(context: Context): List<WatchedItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_WATCHED, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        WatchedItem(
                            imdbId    = obj.optString("imdbId"),
                            title     = obj.optString("title"),
                            type      = obj.optString("type"),
                            season    = obj.optInt("season", 1),
                            episode   = obj.optInt("episode", 1),
                            posterUrl = obj.optString("posterUrl"),
                            watchedAt = obj.optLong("watchedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, list: List<WatchedItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("imdbId",    item.imdbId)
                put("title",     item.title)
                put("type",      item.type)
                put("season",    item.season)
                put("episode",   item.episode)
                put("posterUrl", item.posterUrl)
                put("watchedAt", item.watchedAt)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WATCHED, arr.toString())
            .apply()
    }
}
