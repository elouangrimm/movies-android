package xyz.elouan.movies

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.elouan.movies.adapter.SearchAdapter
import xyz.elouan.movies.adapter.WatchedAdapter
import xyz.elouan.movies.data.OmdbRepository
import xyz.elouan.movies.data.RecentlyWatched
import xyz.elouan.movies.data.SearchResult
import xyz.elouan.movies.data.WatchedItem

class MainActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchResults: RecyclerView
    private lateinit var recentlyWatchedLabel: TextView
    private lateinit var recentlyWatchedList: RecyclerView
    private lateinit var emptyState: TextView

    private val searchAdapter = SearchAdapter { result -> openPlayer(result) }
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchInput          = findViewById(R.id.search_input)
        searchResults        = findViewById(R.id.search_results)
        recentlyWatchedLabel = findViewById(R.id.recently_watched_label)
        recentlyWatchedList  = findViewById(R.id.recently_watched_list)
        emptyState           = findViewById(R.id.empty_state)

        searchResults.layoutManager = GridLayoutManager(this, 3)
        searchResults.adapter = searchAdapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.length < 2) {
                    showRecent()
                    return
                }
                searchJob = lifecycleScope.launch {
                    delay(400) // debounce
                    doSearch(query)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (searchInput.text.isNullOrBlank()) showRecent()
    }

    private fun showRecent() {
        searchAdapter.submitList(emptyList())
        searchResults.visibility = View.GONE

        val watched = RecentlyWatched.getAll(this)
        if (watched.isEmpty()) {
            recentlyWatchedLabel.visibility = View.GONE
            recentlyWatchedList.visibility  = View.GONE
            emptyState.visibility           = View.VISIBLE
        } else {
            emptyState.visibility           = View.GONE
            recentlyWatchedLabel.visibility = View.VISIBLE
            recentlyWatchedList.visibility  = View.VISIBLE
            recentlyWatchedList.layoutManager = GridLayoutManager(this, 3)
            recentlyWatchedList.adapter = WatchedAdapter(watched) { item ->
                openWatched(item)
            }
        }
    }

    private suspend fun doSearch(query: String) {
        recentlyWatchedLabel.visibility = View.GONE
        recentlyWatchedList.visibility  = View.GONE
        emptyState.visibility           = View.GONE
        searchResults.visibility        = View.VISIBLE

        val results = OmdbRepository.search(query)
        searchAdapter.submitList(results)

        if (results.isEmpty()) {
            emptyState.text       = getString(R.string.no_results)
            emptyState.visibility = View.VISIBLE
        }
    }

    private fun openPlayer(result: SearchResult) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_IMDB_ID,  result.imdbId)
            putExtra(PlayerActivity.EXTRA_TYPE,      result.type)   // "movie" or "series"
            putExtra(PlayerActivity.EXTRA_TITLE,     result.title)
            putExtra(PlayerActivity.EXTRA_POSTER,    result.poster)
        }
        startActivity(intent)
    }

    private fun openWatched(item: WatchedItem) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_IMDB_ID,  item.imdbId)
            putExtra(PlayerActivity.EXTRA_TYPE,      item.type)
            putExtra(PlayerActivity.EXTRA_TITLE,     item.title)
            putExtra(PlayerActivity.EXTRA_POSTER,    item.posterUrl)
            putExtra(PlayerActivity.EXTRA_SEASON,    item.season)
            putExtra(PlayerActivity.EXTRA_EPISODE,   item.episode)
        }
        startActivity(intent)
    }
}
