package xyz.elouan.movies.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xyz.elouan.movies.BuildConfig
import java.net.URL

private const val BASE = "https://www.omdbapi.com/"
private val KEY get() = BuildConfig.OMDB_API_KEY

object OmdbRepository {

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val url = "$BASE?apikey=$KEY&s=${encode(query)}&type="
        val json = fetch(url) ?: return@withContext emptyList()
        val root = JSONObject(json)
        if (root.optString("Response") != "True") return@withContext emptyList()
        val search = root.optJSONArray("Search") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until search.length()) {
                val obj = search.getJSONObject(i)
                add(
                    SearchResult(
                        imdbId = obj.optString("imdbID"),
                        title  = obj.optString("Title"),
                        year   = obj.optString("Year"),
                        type   = obj.optString("Type"),  // "movie" or "series"
                        poster = obj.optString("Poster")
                    )
                )
            }
        }
    }

    suspend fun getTotalSeasons(imdbId: String): Int = withContext(Dispatchers.IO) {
        val url = "$BASE?apikey=$KEY&i=$imdbId"
        val json = fetch(url) ?: return@withContext 1
        JSONObject(json).optString("totalSeasons", "1").toIntOrNull() ?: 1
    }

    suspend fun getEpisodes(imdbId: String, season: Int): List<EpisodeInfo> = withContext(Dispatchers.IO) {
        val url = "$BASE?apikey=$KEY&i=$imdbId&season=$season"
        val json = fetch(url) ?: return@withContext emptyList()
        val root = JSONObject(json)
        val episodes = root.optJSONArray("Episodes") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until episodes.length()) {
                val ep = episodes.getJSONObject(i)
                val title = ep.optString("Title").takeIf { it.isNotBlank() && it != "N/A" } ?: ""
                add(EpisodeInfo(episode = ep.optString("Episode"), title = title))
            }
        }
    }

    private fun fetch(urlStr: String): String? = runCatching {
        URL(urlStr).openStream().bufferedReader().readText()
    }.getOrNull()

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
