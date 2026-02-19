package xyz.elouan.movies.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import xyz.elouan.movies.R
import xyz.elouan.movies.data.SearchResult

class SearchAdapter(
    private val onClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val poster: ImageView = view.findViewById(R.id.poster)
        private val title: TextView   = view.findViewById(R.id.title)
        private val meta: TextView    = view.findViewById(R.id.meta)

        fun bind(item: SearchResult) {
            title.text = item.title
            meta.text  = buildString {
                append(item.year)
                append(" · ")
                append(if (item.type == "series") "TV Series" else "Movie")
            }
            if (item.poster != "N/A") {
                poster.load(item.poster) { crossfade(true) }
            } else {
                poster.setImageResource(R.drawable.ic_no_poster)
            }
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(a: SearchResult, b: SearchResult) = a.imdbId == b.imdbId
            override fun areContentsTheSame(a: SearchResult, b: SearchResult) = a == b
        }
    }
}
