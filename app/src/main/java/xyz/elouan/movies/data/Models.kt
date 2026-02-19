package xyz.elouan.movies.data

data class SearchResult(
    val imdbId: String,
    val title: String,
    val year: String,
    val type: String,   // "movie" or "series"
    val poster: String  // URL or "N/A"
)

data class EpisodeInfo(
    val episode: String,
    val title: String
)

data class WatchedItem(
    val imdbId: String,
    val title: String,
    val type: String,
    val season: Int,
    val episode: Int,
    val posterUrl: String,
    val watchedAt: Long = System.currentTimeMillis()
)

// All embed sources. Order = preference.
data class EmbedSource(
    val name: String,
    val movieUrl: String?,
    val tvUrl: String?
)

val EMBED_SOURCES = listOf(
    EmbedSource("vidsrc-embed.ru",
        movieUrl = "https://vidsrc-embed.ru/embed/movie?imdb={id}",
        tvUrl    = "https://vidsrc-embed.ru/embed/tv?imdb={id}&season={s}&episode={e}"),
    EmbedSource("player.videasy.net",
        movieUrl = "https://player.videasy.net/embed/{id}",
        tvUrl    = "https://player.videasy.net/tv/{id}/{s}/{e}"),
    EmbedSource("2embed.cc",
        movieUrl = "https://2embed.cc/embed/{id}",
        tvUrl    = "https://2embed.cc/embedtv/{id}&s={s}&e={e}"),
    EmbedSource("vidlink.pro",
        movieUrl = "https://vidlink.pro/embed/{id}",
        tvUrl    = "https://vidlink.pro/tv/{id}/{s}/{e}"),
    EmbedSource("multiembed.mov",
        movieUrl = "https://multiembed.mov/?video_id={id}",
        tvUrl    = "https://multiembed.mov/?video_id={id}&s={s}&e={e}"),
    EmbedSource("fsapi.xyz",
        movieUrl = "https://fsapi.xyz/movie/{id}",
        tvUrl    = "https://fsapi.xyz/tv-imdb/{id}-{s}-{e}"),
    EmbedSource("gomo.to",
        movieUrl = "https://gomo.to/movie/{id}",
        tvUrl    = null),
    EmbedSource("vidcloud.stream",
        movieUrl = "https://vidcloud.stream/{id}.html",
        tvUrl    = null)
)

fun EmbedSource.resolveUrl(imdbId: String, season: Int = 1, episode: Int = 1, isTv: Boolean): String? {
    val template = if (isTv) tvUrl else movieUrl ?: return null
    return template
        ?.replace("{id}", imdbId)
        ?.replace("{s}", season.toString())
        ?.replace("{e}", episode.toString())
}
