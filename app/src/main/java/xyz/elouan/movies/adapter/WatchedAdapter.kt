package xyz.elouan.movies.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import xyz.elouan.movies.R
import xyz.elouan.movies.data.WatchedItem

class WatchedAdapter(
    private val items: List<WatchedItem>,
    private val onClick: (WatchedItem) -> Unit
) : RecyclerView.Adapter<WatchedAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val poster: ImageView = view.findViewById(R.id.poster)
        private val title: TextView   = view.findViewById(R.id.title)
        private val meta: TextView    = view.findViewById(R.id.meta)

        fun bind(item: WatchedItem) {
            title.text = item.title
            meta.text = if (item.type == "series") {
                "S${item.season} E${item.episode}"
            } else {
                "Movie"
            }
            if (item.posterUrl.isNotBlank() && item.posterUrl != "N/A") {
                poster.load(item.posterUrl) { crossfade(true) }
            } else {
                poster.setImageResource(R.drawable.ic_no_poster)
            }
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
